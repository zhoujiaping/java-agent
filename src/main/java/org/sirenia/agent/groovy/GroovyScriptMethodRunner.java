package org.sirenia.agent.groovy;

import java.io.File;

import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class GroovyScriptMethodRunner {
	private GroovyClassLoader groovyClassLoader;
	public void initGroovyClassLoader() {
		CompilerConfiguration config = new CompilerConfiguration();
		config.setSourceEncoding("UTF-8");
		// 设置该GroovyClassLoader的父ClassLoader为当前线程的加载器(默认)
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		groovyClassLoader = new GroovyClassLoader(cl, config);
	}

	public GroovyObject loadGroovyScript(File file) throws Exception {
		Class<?> groovyClass = groovyClassLoader.parseClass(file);
		GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
		return groovyObject;
	}

	public Object invokeMethod(GroovyObject groovyObject, String method, Object args) throws Exception {
		Object res = groovyObject.invokeMethod(method, args);
		return res;
	}

}
