import java.lang.reflect.Modifier
import java.util.Map
import java.util.concurrent.ConcurrentHashMap

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.LoaderClassPath

class AssistProxy {
	static final String methodSuffix = "_pxy"
	static final String delegateName = "_pxy_ivk"
	static String ivkName = AssistInvoker.class.getName()
	static String pxyName = AssistProxy.class.getName()
	static final Map<String, AssistInvoker> ivkMap = new ConcurrentHashMap<>()

	static CtClass proxy(ClassLoader cl, String className, AssistInvoker ivk) throws Exception {
		if (cl == null) {
			cl = Thread.currentThread().getContextClassLoader()
		}
		ClassPool pool = ClassPool.getDefault()// ClassPoolUtils.linkClassPool(cl)
		pool.appendClassPath(new LoaderClassPath(cl))
		CtClass ct = pool.getCtClass(className)
		// 解冻
		if (ct.isFrozen()) {
			ct.defrost()
		}
		/*
		 * CtClass type = pool.get(AssistInvoker.class.getName()) CtField ctf =
		 * new CtField(type , delegateName, ct)
		 * ctf.setModifiers(Modifier.PUBLIC|Modifier.STATIC) ct.addField(ctf)
		 */
		CtMethod[] methods = ct.getDeclaredMethods()// ct.getMethods()
		for (int i = 0 i < methods.length i++) {
			CtMethod method = methods[i]
			String methodName = method.getName()
			CtMethod copyMethod = CtNewMethod.copy(method, methodName + methodSuffix, ct, null)
			ct.addMethod(copyMethod)
			int mod = method.getModifiers()
			String body = ""
			if (Modifier.isStatic(mod)) {
				body = "{return ($r)$proceed($class,null," + wrapQuota(methodName) + ",$sig,$args)}"
			} else {
				body = "{return ($r)$proceed($class,$0," + wrapQuota(methodName) + ",$sig,$args)}"
			}
			// method.setBody(body,delegateName, "invoke")
			String delegateName = "("+ivkName+")"+pxyName+".ivkMap.get("+wrapQuota(ct.getName())+")"
			method.setBody(body, delegateName, "invoke")
		}
		if (ivk != null) {
			ivkMap.put(ct.getName(), ivk)
		}
		// Class<?> c = ct.toClass()
		// ct.writeFile("d:/")
		// ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile())
		return ct
	}

	public static CtClass proxy(ClassLoader cl, String className) throws Exception {
		return proxy(cl, className, null)
	}

	public static void setInvoker(String className, AssistInvoker ivk) throws Exception {
		ivkMap.put(className, ivk)
	}
	public static void setInvoker(Class<?> c, AssistInvoker ivk) throws Exception {
		setInvoker(c.getName(), ivk)
	}

	public static void setInvoker(CtClass ct, AssistInvoker ivk) throws Exception {
		/*
		 * Field f = clazz.getDeclaredField(delegateName) f.set(clazz,
		 * invoker)
		 */
		setInvoker(ct.getName(), ivk)
	}

	private static String wrapQuota(String text) {
		return "\"" + text + "\""
	}
}
