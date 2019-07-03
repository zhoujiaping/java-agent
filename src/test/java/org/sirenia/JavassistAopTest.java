package org.sirenia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class JavassistAopTest {
	@Test
	public void test() throws NotFoundException, CannotCompileException, IOException{
		String className = "org.sirenia.Dog";
		ClassPool pool = ClassPool.getDefault();
		ClassLoader classLoader = JavassistAopTest.class.getClassLoader();
		pool.insertClassPath(new LoaderClassPath(classLoader));
		CtClass ct = pool.getCtClass(className);
		CtMethod method = ct.getDeclaredMethod("shout");
		CtMethod copyMethod = CtNewMethod.copy(method, method.getName() + "$proxy", ct, null);
		ct.addMethod(copyMethod);
		/*method.insertBefore("");
		method.insertAfter("");
		method.insertAfter("", true);
		method.addCatch("", pool.getCtClass("java.lang.Throwable"));*/
		List<String> body = new ArrayList<>();
		body.add("{");
		body.add("return ($r)$proceed($class,$0,\""+method.getName()+"\",$sig,$args);");
		body.add("}");
		method.setBody(String.join("\n", body), "org.sirenia.JavassistProxy", "invoke");
		ct.toClass();
		ct.writeFile();
		Dog dog = new Dog();
		System.out.println(dog.shout("zhou"));
	}
}
