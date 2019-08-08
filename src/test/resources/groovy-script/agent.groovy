import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.HashSet
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.agent.groovy.GroovyScriptShell
import org.sirenia.agent.javassist.MethodInvoker
import org.sirenia.agent.javassist.JavassistProxy
import org.sirenia.agent.javassist.MyMethodProxy
import org.sirenia.agent.javassist.MethodFilter

import groovy.lang.GroovyShell
import javassist.CtClass
import javassist.CtMethod
import org.slf4j.LoggerFactory
/*
-javaagent:d:/git-repo/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar
*/
class MyClassFileTransformer{
	def logger = LoggerFactory.getLogger(MyClassFileTransformer.class);
	GroovyShell shell = new GroovyShell()
	def loadedClass = new ConcurrentHashMap<>()
	//需要代理的类，不要通过正则匹配大范围的类，会导致启动很慢
	def classSet = new HashSet<>();
	{
		classSet.add("org.wt.web.HelloController")
	}
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		try{
			if(className == null){
				return null;
			}
			className = className.replace("/", ".");
			if(loadedClass.containsKey(className)){
				return null;
			}
			//配置哪些类需要被代理，如果不配置，groovy的类也会被代理。而groovy的类我们不需要代理。
			if(className.startsWith("org.sirenia")){
				return null
			}
			if(!classSet.contains(className)){
				return null
			}
			/*if(className.contains("FxApiDataCryptComponent")){
				return null
			}*/
			
			//println "transformer => $className"
			def parts = className.split('\\.')
			def simpleName = parts[-1]
			File file = new File("/tomcat/mock/${simpleName}.groovy");
			if(!file.exists()){
				return null;
			}
			println "transformer => $className"
			MethodInvoker invoker = new MethodInvoker(){
				@Override
				public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
					//调用另一个groovy脚本
					Object o = shell.evaluate(file)
					if(o.metaClass.respondsTo(o,thisMethod.getName())){
						//println('######################################'+thisMethod.getName())
						return o.invokeMethod(thisMethod.getName(),args)
					}
					return proceed.invoke(self,args)
				}
			};
			CtClass ctClass = MyMethodProxy.proxy(className, null, invoker)
			def bytecode = ctClass.toBytecode()
			//ctClass.writeFile("D:/git-repo/java-agent-web/target/classes");
			//ctClass.detach()
			loadedClass.put(className, "")
			bytecode
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}