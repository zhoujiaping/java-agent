package org.sirenia;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.sirenia.agent.javassist.JavassistProxy;
import org.sirenia.agent.javassist.MethodInvoker;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

public class JavassistAopTest {
	@Test
	public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException{
		String className = "org.sirenia.Dog";
		MethodInvoker invoker = new MethodInvoker() {
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				System.out.println("123");
				return null;
			}
		};
		CtClass ct = JavassistProxy.proxy(className, null, invoker );
		ct.toClass();
		ct.writeFile();
		Dog dog = new Dog();
		dog.shoutTwo("456");
	}
}
