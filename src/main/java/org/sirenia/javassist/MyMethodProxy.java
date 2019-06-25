package org.sirenia.javassist;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.sirenia.util.ClassUtil;
import org.sirenia.util.JRender;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
/**
 * 结合PackageUtil，可以设置指定包下的指定类的指定方法的 方法拦截。支持环绕通知。
 */
public class MyMethodProxy {
	private static final Map<String,MethodInvoker> contextMap = new ConcurrentHashMap<>();
	
	public static Object invoke(String uid,String className, String methodName,String parameterTypeNames,Object self,Object[] args) throws Throwable{
		Class<?> selfClass = Class.forName(className);
		String[] parameterTypeArray = parameterTypeNames.split(",");
		Class<?>[] parameterTypes = new Class[parameterTypeArray.length];
		for(int i=0;i<parameterTypeArray.length;i++){
			parameterTypes[i] = ClassUtil.forName(parameterTypeArray[i]);
		}
		Method thisMethod = selfClass.getMethod(methodName,parameterTypes);
		Method proceed = selfClass.getMethod(methodName+"$proxy",parameterTypes);
		MethodInvoker invoker = contextMap.get(uid);
		return invoker.invoke(self, thisMethod, proceed, args);
	}
	public static CtClass proxy(String className, MyMethodFilter[] filters, MethodInvoker invoker) throws NotFoundException, CannotCompileException, ClassNotFoundException, IOException {
		ClassPool pool = ClassPool.getDefault();
		ClassLoader classLoader = MyMethodProxy.class.getClassLoader();
		pool.appendClassPath(new LoaderClassPath(classLoader));
		CtClass ct = pool.getCtClass(className);
		//CtClass ct = pool.get(className);
		// 解冻
		ct.defrost();
		String uid = UUID.randomUUID().toString();
		CtMethod[] methods = ct.getDeclaredMethods();//ct.getMethods();
		for (int i = 0; i < methods.length; i++) {
			CtMethod method = methods[i];
			boolean accept = true;
			if(filters!=null){
				for(MyMethodFilter filter : filters){
					if(!filter.equals(method)){
						accept = false;
						break;
					}
				}
			}
			if(!accept){
				continue;
			}
			String methodName = method.getName();
			int modifiers = method.getModifiers();
			boolean isStatic = Modifier.isStatic(modifiers);
			CtMethod copyMethod = CtNewMethod.copy(method, method.getName() + "$proxy", ct, null);
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
				body.add("return ($r)${methodProxyName}.invoke(${uid},${className},${methodName},${parameterTypes},null,$args);");
			} else {
				body.add("return ($r)${methodProxyName}.invoke(${uid},${className},${methodName},${parameterTypes},$0,$args);");
			}
			body.add("}");
			String bodyString = String.join("\n", body);
			Map<String, Object> variables = new HashMap<>();
			variables.put("methodProxyName", methodProxyName);
			variables.put("uid", wrapQuota(uid));
			variables.put("className", wrapQuota(className));
			variables.put("methodName", wrapQuota(methodName));
			variables.put("parameterTypes", wrapQuota(parameterTypes));
			bodyString = new JRender("${","}").withVariables(variables ).render(bodyString);
			method.setBody(bodyString);
		}
		//ct.writeFile();
		//ct.toClass();
		contextMap.put(uid, invoker);
		return ct;
	};
	private static String wrapQuota(String text){
		return "\""+text+"\"";
	}
}
