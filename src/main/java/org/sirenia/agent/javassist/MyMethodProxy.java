package org.sirenia.agent.javassist;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.sirenia.agent.util.ClassPoolUtils;
import org.sirenia.agent.util.ClassUtil;
import org.sirenia.agent.util.StrFmt;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

/**
 * 结合PackageUtil，可以设置指定包下的指定类的指定方法的 方法拦截。支持环绕通知。
 */
public class MyMethodProxy {
	private static final Map<String, MethodInvoker> invokerMap = new ConcurrentHashMap<>();
	public static Object invoke(String uid, String className, String methodName, String parameterTypeNames, Object self,
			Object[] args) throws Throwable {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?> selfClass = Class.forName(className,true,cl);
		Method thisMethod = null;
		Method proceed = null;
		if(parameterTypeNames==""){
			thisMethod = selfClass.getDeclaredMethod(methodName);
			proceed = selfClass.getDeclaredMethod(methodName + JavassistProxy.methodSuffix);
		}else{
			String[] parameterTypeArray = parameterTypeNames.split(",");
			Class<?>[] parameterTypes = new Class[parameterTypeArray.length];
			for (int i = 0; i < parameterTypeArray.length; i++) {
				parameterTypes[i] = ClassUtil.forName(parameterTypeArray[i],true,cl);
			}
			thisMethod = selfClass.getDeclaredMethod(methodName, parameterTypes);
			proceed = selfClass.getDeclaredMethod(methodName + JavassistProxy.methodSuffix, parameterTypes);
		}
		MethodInvoker invoker = invokerMap.get(uid);
		if (!proceed.isAccessible()) {
			proceed.setAccessible(true);
		}
		return invoker.invoke(self, thisMethod, proceed, args);
	}

	public static CtClass proxy(CtClass ct, MethodFilter filter, MethodInvoker invoker)
			throws NotFoundException, CannotCompileException, ClassNotFoundException, IOException {
		// 解冻
		ct.defrost();
		String uid = UUID.randomUUID().toString();
		CtMethod[] methods = ct.getDeclaredMethods();// ct.getMethods();
		for (int i = 0; i < methods.length; i++) {
			CtMethod method = methods[i];
			if (filter != null && !filter.filter(method)) {
				continue;
			}
			String methodName = method.getName();
			int modifiers = method.getModifiers();
			boolean isStatic = Modifier.isStatic(modifiers);
			CtMethod copyMethod = CtNewMethod.copy(method, method.getName() +JavassistProxy.methodSuffix, ct, null);
			ct.addMethod(copyMethod);
			CtClass[] paramCtClasses = method.getParameterTypes();
			String[] paramClassNames = new String[paramCtClasses.length];
			for (int j = 0; j < paramCtClasses.length; j++) {
				String name = paramCtClasses[j].getName();
				paramClassNames[j] = name;
			}
			String parameterTypes = String.join(",", paramClassNames).toString();
			String methodProxyName = MyMethodProxy.class.getName();
			List<String> body = new ArrayList<>();
			body.add("{");
			if (isStatic) {
				body.add(
						"return ($r)${methodProxyName}.invoke(${uid},${className},${methodName},${parameterTypes},null,$args);");
			} else {
				body.add(
						"return ($r)${methodProxyName}.invoke(${uid},${className},${methodName},${parameterTypes},$0,$args);");
			}
			body.add("}");
			String bodyString = String.join("\n", body);
			Map<String, Object> variables = new HashMap<>();
			variables.put("methodProxyName", methodProxyName);
			variables.put("uid", wrapQuota(uid));
			variables.put("className", wrapQuota(ct.getName()));
			variables.put("methodName", wrapQuota(methodName));
			variables.put("parameterTypes", wrapQuota(parameterTypes));
			bodyString = new StrFmt("${", "}").withVariables(variables).render(bodyString);
			method.setBody(bodyString);
		}
		// ct.writeFile();
		// ct.toClass();
		invokerMap.put(uid, invoker);
		return ct;
	}

	public static CtClass proxy(String className, MethodFilter filter, MethodInvoker invoker)
			throws NotFoundException, CannotCompileException, ClassNotFoundException, IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ClassPool pool = ClassPoolUtils.linkClassPool(cl);
		//ClassLoader classLoader = MyMethodProxy.class.getClassLoader();
		//pool.appendClassPath(new LoaderClassPath(classLoader));
		//pool.appendClassPath(new LoaderClassPath(cl));
		CtClass ct = pool.getCtClass(className);
		// CtClass ct = pool.get(className);
		return proxy(ct, filter, invoker);
	};
	
	private static String wrapQuota(String text) {
		return "\"" + text + "\"";
	}
}
