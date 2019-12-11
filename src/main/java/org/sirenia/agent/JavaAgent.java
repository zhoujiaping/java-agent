package org.sirenia.agent;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;

public class JavaAgent implements ClassFileTransformer {
	public static String mockDir = System.getProperty("user.dir")+"/src/test/resources/mock";
	private GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();

	public JavaAgent() {
		try {
			File file = null;
			String mockDir = System.getProperty("mock.dir");
			if(mockDir!=null && mockDir.trim().length()>0) {
				JavaAgent.mockDir = mockDir;
			}
			file = new File(JavaAgent.mockDir,"agent/ClassFileTransformer.groovy");
			GroovyClassLoader gcl = new GroovyClassLoader();
			if (!file.exists()) {
				throw new RuntimeException("file not found: " + file.getAbsolutePath());
			}
			System.out.println("use transformer : "+file.getAbsolutePath());
			Class clazz = gcl.parseClass(file);
			groovyObject = (GroovyObject) clazz.newInstance();
			//groovyObject = (GroovyObject) shell.evaluate(file);
			groovyObject.invokeMethod("init",new Object[]{gcl});
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
			System.err.println("transform error for : "+className);
			RuntimeException e = new RuntimeException("transform error for : "+className,e1);
			e.printStackTrace();
			throw e;
		}
	}

	public static void premain(String agentOps, Instrumentation inst) {
		inst.addTransformer(new JavaAgent(), true);
	}

}
