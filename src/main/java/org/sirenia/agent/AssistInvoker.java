package org.sirenia.agent;

import java.lang.reflect.Method;

public abstract class AssistInvoker {
	public static AssistInvoker defaultIvk = new AssistInvoker(){
		public Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args) throws Throwable{
			//Method thisMethod = selfClass.getDeclaredMethod(method, types);
			Method proceed = selfClass.getDeclaredMethod(method+AssistProxy.methodSuffix, types);
			if(!proceed.isAccessible()){
				proceed.setAccessible(true);
			}
			return proceed.invoke(self, args);
		}
	};
	public Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args) throws Throwable{
		Method thisMethod = selfClass.getDeclaredMethod(method, types);
		Method proceed = selfClass.getDeclaredMethod(method+AssistProxy.methodSuffix, types);
		if(!proceed.isAccessible()){
			proceed.setAccessible(true);
		}
		return invoke(self,thisMethod,proceed,args);
	}
	public Object invoke(Object self,Method thisMethod,Method proceed,Object[] args) throws Throwable{
		return proceed.invoke(self, args);
	}

}
