package org.sirenia;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.sirenia.agent.javassist.JavassistProxy;
import org.sirenia.agent.javassist.MethodInvoker;
import org.sirenia.agent.util.AppClassPoolHolder;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class JavassistAopTest {
	@Test
	public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException{
		ClassPool pool = ClassPool.getDefault();
		AppClassPoolHolder.set(pool);
		String className = "org.sirenia.Dog";
		MethodInvoker invoker = new MethodInvoker() {
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				System.out.println("123");
				return null;
			}
		};
		CtClass ct = JavassistProxy.proxy(className, null, invoker );
		if(ct.isFrozen()){
			ct.defrost();
		}
		String name = ct.getClassFile().getSourceFile();
		//ct.toClass();
		ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile());
		//ct.writeFile("d:/");
		Dog dog = new Dog();
		dog.shoutTwo("456");
	}
	@Test
	public void test2() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException{
		String className = "org.wt.service.impl.HelloServiceImpl";
		MethodInvoker invoker = new MethodInvoker() {
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				System.out.println("123");
				return null;
			}
		};
		CtClass ct = JavassistProxy.proxy(className, null, invoker );
		if(ct.isFrozen()){
			ct.defrost();
		}
		String name = ct.getClassFile().getSourceFile();
		//ct.toClass();
		ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile());
		//ct.writeFile("d:/");
		Dog dog = new Dog();
		dog.shoutTwo("456");
	}
}
