package org.sirenia;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.sirenia.agent.javassist.JavassistProxy;
import org.sirenia.agent.javassist.MethodInvoker;
import org.sirenia.agent.util.ClassPoolUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class JavassistAopTest {
	@Test
	public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException{
		ClassPool pool = ClassPoolUtils.linkClassPool();
		String className = "org.sirenia.Dog";
		MethodInvoker invoker = new MethodInvoker() {
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				System.out.println("123");
				return null;
			}
		};
		CtClass ct = JavassistProxy.proxy(this.getClass().getClassLoader(),className, null, invoker );
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
		CtClass ct = JavassistProxy.proxy(this.getClass().getClassLoader(),className, null, invoker );
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
	public void test3() throws Exception{
		ClassLoader cl = this.getClass().getClassLoader();
		ClassPool pool = new ClassPool();
		pool.appendClassPath(new LoaderClassPath(cl));
		CtClass ct = pool.getCtClass("java.lang.String");
		
		cl = this.getClass().getClassLoader().getParent();
		//this.getClass().getClassLoader().getSystemClassLoader();
		pool =  new ClassPool();
		pool.appendClassPath(new LoaderClassPath(cl));
		ct = pool.getCtClass("java.lang.String");
	}
}
