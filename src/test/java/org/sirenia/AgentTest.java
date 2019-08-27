package org.sirenia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.sirenia.agent.AssistInvoker;
import org.sirenia.agent.AssistProxy;

import groovy.json.JsonOutput;
import javassist.CtClass;

public class AgentTest {

	public static void main(String[] args) throws Exception {
		System.out.println("agent");
		AssistInvoker ivk = new AssistInvoker(){
			@Override
			public Object invoke(Class<?> selfClass, Object self, String method, Class<?>[] types, Object[] args)
					throws Throwable {
				System.out.println("invoke "+method);
				return AssistInvoker.defaultIvk.invoke(selfClass, self, method, types, args);
			}
		};
		CtClass ct = AssistProxy.proxy(null, "org.sirenia.Dog",ivk);
		//AssistProxy.setInvoker(ct,  ivk);
		Class<?> c = ct.toClass();
		
		//Field f = c.getDeclaredField("_pxy_ivk");
		//f.set(c, ivk);
		
		Object i = c.newInstance();
		Method m = c.getDeclaredMethod("shout", String.class);
		Object r = m.invoke(i, "qwer");
		System.out.println(r);
	}
	

}
