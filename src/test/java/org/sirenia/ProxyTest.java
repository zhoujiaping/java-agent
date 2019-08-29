package org.sirenia;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

import org.junit.Test;

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
		byte[] buf = (byte[]) proxy.invokeMethod("proxy", "org.sirenia.enums.Color");
		Class res = gcl.defineClass("org.sirenia.enums.Color", buf);
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
		Object ct = proxy.invokeMethod("proxy", "org.sirenia.domain.Naruto");
		Class res = (Class) ct.getClass().getMethod("toClass").invoke(ct);
		//Class res = gcl.defineClass("org.sirenia.domain.Naruto", buf);
		Method[] ms = res.getDeclaredMethods();
		Constructor c = res.getDeclaredConstructor();
		c.setAccessible(true);
		Object target = c.newInstance();
		Method m = res.getDeclaredMethod("luoxuanwan", Date.class,int.class);
		Method m2 = res.getDeclaredMethod("luoxuanwan_pxy", Date.class,int.class);
		m2.setAccessible(true);
		Object r = m.invoke(target, null,1);
		System.out.println(r);
		System.out.println(res);
	}
}
