package org.sirenia.agent;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;

public class JavaAgent implements ClassFileTransformer {
	public static String groovyFileDir = System.getProperty("user.home") + "/mock";// "/home/wt/mock/";
	//private volatile GroovyObject groovyObject;
	private GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();

	public JavaAgent() {
		try {
			GroovyShell shell = new GroovyShell();
			File file = new File(groovyFileDir, "ClassFileTransformer.groovy");
			if (!file.exists()) {
				throw new RuntimeException("file not found: " + file.getAbsolutePath());
			}
			System.out.println("use transformer : "+file.getAbsolutePath());
			groovyObject = (GroovyObject) shell.evaluate(file);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
			RuntimeException e = new RuntimeException("transform error: "+className,e1);
			e.printStackTrace();
			throw e;
		}
	}

	public static void premain(String agentOps, Instrumentation inst) {
		if (agentOps != null && !agentOps.trim().isEmpty()) {
			groovyFileDir = agentOps;
		}
		inst.addTransformer(new JavaAgent(), true);
	}

}
