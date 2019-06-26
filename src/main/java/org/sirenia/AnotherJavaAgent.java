package org.sirenia;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sirenia.groovy.GroovyScriptMethodRunner;
import org.sirenia.javassist.MethodInvoker;
import org.sirenia.javassist.MyMethodProxy;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import javassist.CtClass;

public class AnotherJavaAgent implements ClassFileTransformer {
	private GroovyScriptMethodRunner groovyRunner = new GroovyScriptMethodRunner();
	private long prevModified = 0;
	private GroovyObject groovyObject;
	public AnotherJavaAgent(){
		groovyRunner.initGroovyClassLoader();
	}
	private Map<String,String> loadedClass = new ConcurrentHashMap<>();
	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes)
			throws IllegalClassFormatException {
		if(className == null){
			return null;
		}
        className = className.replace("/", ".");
		if(!className.contains("One")){
			return null;
		}
		if(loadedClass.containsKey(className)){
			return null;
		}
		loadedClass.put(className, "");
		MethodInvoker invoker = new MethodInvoker(){
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				Binding binding = new Binding();
				binding.setVariable("self", self);
				binding.setVariable("thisMethod", thisMethod);
				binding.setVariable("proceed", proceed);
				binding.setVariable("args", args);
				File groovyFile = new File("e:/groovy-script/agent.groovy");
				long lastModifyTime = groovyFile.lastModified();
				if(prevModified<lastModifyTime){
					prevModified = lastModifyTime;
					groovyObject = groovyRunner.loadGroovyScript(groovyFile);
				}
				return groovyObject.invokeMethod("invoke", new Object[]{self,thisMethod,proceed,args});
			}
		};
		try {
			CtClass ctClass = MyMethodProxy.proxy(className, null, invoker);
			//System.out.println(className);
			return ctClass.toBytecode();
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
    public static void    premain(String agentOps,Instrumentation inst){
        inst.addTransformer(new AnotherJavaAgent(),true);
    }

}
