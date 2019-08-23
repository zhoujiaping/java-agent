package org.sirenia.agent;

import java.io.File;
import java.io.FileNotFoundException;
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
	public static String groovyFileDir = System.getProperty("user.home")+"/mock";//"/home/wt/mock/";
	private long prevModified = 0;
	private volatile GroovyObject groovyObject;
	private Set<String> mustIgnored = new HashSet<>();
	public JavaAgent()  {
		groovyRunner.initGroovyClassLoader();
		File file = new File(groovyFileDir,"agent.groovy");
		if(!file.exists()){
			throw new RuntimeException("file not found: "+file.getAbsolutePath());
		}
		try {
			groovyObject = groovyRunner.loadGroovyScript(file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
			//System.out.println("groobyObject="+groovyObject);
			Object res = groovyObject.invokeMethod("transform", new Object[]{classLoader,className,clazz,domain,bytes});
			return (byte[]) res;
		} catch (Exception e1) {
			System.err.println(className);
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
	}
    public static void premain(String agentOps,Instrumentation inst){
    	if(agentOps!=null && !agentOps.trim().isEmpty()){
			groovyFileDir = agentOps;
    	}
        inst.addTransformer(new JavaAgent(),true);
    }

}
