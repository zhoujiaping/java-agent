package org.sirenia.agent.groovy;

import java.io.File;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
/**
 * 这种方式会每次都解析groovy代码，性能开销不如GroovyClassLoader方式。
 * GroovyScriptShell方式实际上是创建了一个脚本类，执行了其实例的run方法。
 * @author Administrator
 *
 */
public class GroovyScriptShell {
	public static Object evaluate(File file, Binding binding) {
		// Binding binding = new Binding();
		// binding.setProperty("name", "lufy");
		GroovyShell groovyShell = new GroovyShell(binding);
		Object result;
		try {
			result = groovyShell.evaluate(file);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object evaluate(String script, Binding binding) {
		GroovyShell groovyShell = new GroovyShell(binding);
		Object result;
		try {
			result = groovyShell.evaluate(script);
			System.out.println("groovy: "+result);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
