package org.sirenia;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

import org.junit.Test;
import org.sirenia.domain.Naruto;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class ProxyTest {
	/**
	 * 不能修改枚举类
	 * @throws Exception
	 */
	@Test
	public void testProxyEnum() throws Exception{
		GroovyClassLoader gcl = new GroovyClassLoader();
		String filename = gcl.getResource("script/ClassProxy.groovy").getFile();
		File file = new File(filename);
		GroovyObject proxy = (GroovyObject) gcl.parseClass(file ).newInstance();
		proxy.invokeMethod("init", gcl);
		Class res = (Class) proxy.invokeMethod("proxyToClass", "org.sirenia.enums.Color");
		Object str = res.getDeclaredMethod("hello").invoke(res.getDeclaredField("BLACK").get(null));
		System.out.println(str);
		System.out.println(res);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	@Test
	public void testProxyFinalClass()throws Exception{
		GroovyClassLoader gcl = new GroovyClassLoader();
		String filename = gcl.getResource("script/ClassProxy.groovy").getFile();
		File file = new File(filename);
		GroovyObject proxy = (GroovyObject) gcl.parseClass(file ).newInstance();
		proxy.invokeMethod("init", gcl);
		Class monkey = Class.forName("org.sirenia.domain.Monkey");//(Class) proxy.invokeMethod("proxyToClass", "org.sirenia.domain.Monkey");
		Class shuimen = Class.forName("org.sirenia.domain.Shuimen");// (Class) proxy.invokeMethod("proxyToClass", "org.sirenia.domain.Shuimen");
		Class res = (Class) proxy.invokeMethod("proxyToClass", "org.sirenia.domain.Naruto");
		
		//Class res = gcl.defineClass("org.sirenia.domain.Naruto", buf);
		Method[] ms = res.getDeclaredMethods();
		Constructor c = res.getDeclaredConstructor();
		c.setAccessible(true);
		Object target = c.newInstance();
		Method m = res.getDeclaredMethod("luoxuanwan", Date.class,int.class);
		//Method m2 = res.getDeclaredMethod("luoxuanwan_pxy", Date.class,int.class);
		//m2.setAccessible(true);
		Object r = m.invoke(target, null,1);
		System.out.println(r);
		System.out.println(res);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	@Test
	public void testProxyFinalClass2()throws Exception{
		GroovyClassLoader gcl = new GroovyClassLoader();
		String filename = gcl.getResource("script/ClassProxy.groovy").getFile();
		File file = new File(filename);
		GroovyObject proxy = (GroovyObject) gcl.parseClass(file ).newInstance();
		proxy.invokeMethod("init", gcl);
		Class res = (Class) proxy.invokeMethod("proxyToClass", "org.sirenia.domain.Naruto");
		//Class res = gcl.defineClass("org.sirenia.domain.Naruto", buf);
		Method[] ms = res.getDeclaredMethods();
		Constructor c = res.getDeclaredConstructor();
		c.setAccessible(true);
		Object target = c.newInstance();
		Method m = res.getDeclaredMethod("addDate", Date.class,int.class);
		m.setAccessible(true);
 		Object r = m.invoke(target, new Date(),1);
		System.out.println(r);
		System.out.println(res);
	}

	@Test
	public void testClassPath() throws ClassNotFoundException {
		String cp = System.getProperty("java.class.path");
		System.setProperty("java.class.path",cp+":/home/wt/IdeaProjects/java-agent-web/target/classes");

	}
	@Test
	public void testInvokerParentMethod()throws Exception{
		Class c = Class.forName("org.sirenia.domain.Shuimen");
		Naruto n = new Naruto();
		Method m = c.getDeclaredMethod("luoxuanwan", Date.class,int.class);
		Object res = m.invoke(n, new Date(),1);
		System.out.println(res);
	}
}
