package org.sirenia.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class JavaAgent implements ClassFileTransformer {
	public static String groovyFileDir = System.getProperty("user.home") + "/mock";// "/home/wt/mock/";
	private volatile GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();

	public JavaAgent() {
		CompilerConfiguration config = new CompilerConfiguration();
		config.setSourceEncoding("UTF-8");
		// 设置该GroovyClassLoader的父ClassLoader为当前线程的加载器(默认)
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		GroovyClassLoader gcl = null;
		try {
			gcl = new GroovyClassLoader(cl, config);
			File file = new File(groovyFileDir, "ClassFileTransformer.groovy");
			if (!file.exists()) {
				throw new RuntimeException("file not found: " + file.getAbsolutePath());
			}
			Class<?> agentClass = gcl.parseClass(file);
			groovyObject = (GroovyObject) agentClass.newInstance();
			groovyObject.invokeMethod("init", new Object[] {});
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (gcl != null) {
				try {
						gcl.close();
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}
		}
		/**
		 * 如果我们在transform中调用了Method#invoke方法，就必须忽略这个类。
		 * 否则执行Method#invoke要去加载MethodHandleImpl，加载MethodHandleImpl又要去执行Method#
		 * invoke，死循环了。
		 */
		mustIgnored.add("java/lang/invoke/MethodHandleImpl");
	}

	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain,
			byte[] bytes) {
		try {
			if (className == null) {
				return null;
			}
			if (mustIgnored.contains(className)) {
				return null;
			}
			Object res = groovyObject.invokeMethod("transform",
					new Object[] { classLoader, className, clazz, domain, bytes });
			return (byte[]) res;
		} catch (Exception e1) {
			// System.err.println(className);
			// e1.printStackTrace();
			throw new RuntimeException(e1);
		}
	}

	public static void premain(String agentOps, Instrumentation inst) {
		if (agentOps != null && !agentOps.trim().isEmpty()) {
			groovyFileDir = agentOps;
		}
		inst.addTransformer(new JavaAgent(), true);
	}

}
