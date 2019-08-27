package org.sirenia.agent;

import java.lang.reflect.Method;

import org.sirenia.agent.javassist.JavassistProxy;
import org.sirenia.agent.util.ClassUtil;

public class Ivk {
	public static Class<?> loadClass(ClassLoader cl,String name) throws ClassNotFoundException{
		return Class.forName(name, true, cl);
	}
	public static Object invoke(ClassLoader cl,String className, String methodName, String parameterTypeNames, Object self,
			Object[] args) throws Throwable {
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
		if (!proceed.isAccessible()) {
			proceed.setAccessible(true);
		}
		return proceed.invoke(self, args);
	}
}
