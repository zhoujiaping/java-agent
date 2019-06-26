package org.sirenia;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import org.sirenia.groovy.GroovyScriptMethodRunner;

import groovy.lang.GroovyObject;

public class JavaAgent implements ClassFileTransformer {
	private GroovyScriptMethodRunner groovyRunner = new GroovyScriptMethodRunner();
	private static String groovyFile;
	private long prevModified = 0;
	private GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();
	public JavaAgent(){
		groovyRunner.initGroovyClassLoader();
		/**
		 * 如果我们在transform中调用了Method#invoke方法，就必须忽略这个类。
		 * 否则执行Method#invoke要去加载MethodHandleImpl，加载MethodHandleImpl又要去执行Method#invoke，死循环了。
		 */
		mustIgnored.add("java/lang/invoke/MethodHandleImpl");
	}
	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes)
			throws IllegalClassFormatException {
		//System.out.println("加载: "+className);
		try {
			if(mustIgnored.contains(className)){
				return null;
			}
			File file = new File(groovyFile);
			long lastModifyTime = file.lastModified();
			if(prevModified<lastModifyTime){
				prevModified = lastModifyTime;
				groovyObject = groovyRunner.loadGroovyScript(file);
			}
			Object res = groovyObject.invokeMethod("transform", new Object[]{classLoader,className,clazz,domain,bytes});
			//groovyObject.invokeMethod("transform",null);
			return (byte[]) res;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
	}
    public static void    premain(String agentOps,Instrumentation inst){
    	groovyFile = agentOps;
        inst.addTransformer(new JavaAgent(),true);
    }

}
