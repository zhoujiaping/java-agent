package org.sirenia.agent;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import org.sirenia.agent.groovy.GroovyScriptMethodRunner;

import groovy.lang.GroovyObject;

public class JavaAgent implements ClassFileTransformer {
	private GroovyScriptMethodRunner groovyRunner = new GroovyScriptMethodRunner();
	private static String groovyFile = "/tomcat/mock/agent.groovy";
	private long prevModified = 0;
	private GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();
	//private boolean loaded;
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
		try {
			if(className == null){
				return null;
			}
			if(mustIgnored.contains(className)){
				return null;
			}
			if (className.startsWith("com.alibaba.dubbo.common.bytecode.proxy")) {
				System.out.println("%%%%%%%%"+className);
			}
			/**
			 * 提前将java-agent项目中的类加载（被appclassloader加载），否则由于调用时机的不同，有些类会被WebappClassLoaderBase加载，
			 * 当我们对WebappClassLoaderBase也进行了拦截的时候，就会出现无限递归调用。
			 */
			/*if(!loaded){
				Set<String> classes = PackageUtil.getClassSet("org.sirenia.agent", true);
				classes.forEach(item->{
					try {
						Class.forName(item);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				});
				loaded = true;
			}*/
			File file = new File(groovyFile);
			long lastModifyTime = file.lastModified();
			if(prevModified<lastModifyTime){
				prevModified = lastModifyTime;
				groovyObject = groovyRunner.loadGroovyScript(file);
			}
			Object res = groovyObject.invokeMethod("transform", new Object[]{classLoader,className,clazz,domain,bytes});
			return (byte[]) res;
		} catch (Exception e1) {
			System.out.println(className);
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
	}
    public static void    premain(String agentOps,Instrumentation inst){
    	if(agentOps!=null && !agentOps.trim().isEmpty()){
    		groovyFile = agentOps;
    	}
        inst.addTransformer(new JavaAgent(),true);
    }

}
