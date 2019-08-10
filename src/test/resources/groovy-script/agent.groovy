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
import org.sirenia.agent.JavaAgent
/*
-javaagent:d:/git-repo/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar
-javaagent:/home/wt/IdeaProjects/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar

*/
class MyClassFileTransformer{
	//def logger = LoggerFactory.getLogger(MyClassFileTransformer.class);
	//执行groovy脚本的shell
	def shell = new GroovyShell()
	//记录已代理过的类名
	def loadedClass = new ConcurrentHashMap<>()
	//需要代理的类，不要通过正则匹配大范围的类，会导致启动很慢
	def classSet = new HashSet<>();
	{
		classSet.add("org.wt.web.HelloController")
		classSet.add("org.wt.service.impl.HelloServiceImpl")
		//通过代理dubbo的InvokerInvocationHandler，实现对远程dubbo服务的代理
		classSet.add("com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler")
		//兼容dubbo的代理
		classSet.add("com.alibaba.dubbo.common.bytecode.ClassGenerator")
	}
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		//println("transformer000 => ${className}")
		try{    //类名有可能是null
			if(className == null){
				return null;
			}
			//类名使用/分隔的，替换成.分隔
			className = className.replace("/", ".");
			//已经代理过，不需要再代理，直接返回null
			if(loadedClass.containsKey(className)){
				return null;
			}
			//对java-agent项目中的类直接放行，不代理
			if(className.startsWith("org.sirenia")){
				return null
			}
			//不需要代理的类，放行
			if(!classSet.contains(className)){
				return null
			}
			
			//println "transformer => $className"
			def parts = className.split('\\.')
			def simpleName = parts[-1]
			File file = new File(JavaAgent.groovyFileDir,"${simpleName}.groovy");
			//对应的代理文件不存在，放行
			if(!file.exists()){
				println("${simpleName}.groovy not found")
				return null;
			}
			println "transformer => $className"
			def invoker = new MethodInvoker(){
				@Override
				public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
					//调用另一个groovy脚本
					def o = shell.evaluate(file)
					def methodName = thisMethod.getName()
					//如果groovy对象有对应的方法，就执行对应的方法
					if(o.metaClass.respondsTo(o,methodName)){
						//println('######################################'+thisMethod.getName())
						return o.invokeMethod(thisMethod.getName(),args)
					//提供在代理方法中调用 目标方法 的机制
					}else if(o.metaClass.respondsTo(o,methodName+"#invoke")){
						//println("")
						return o.invokeMethod(methodName+"#invoke",[self,thisMethod,proceed,args])
					}
					return proceed.invoke(self,args)
				}
			};
			//使用javassist工具，增强字节码，进行代理
			def ctClass = MyMethodProxy.proxy(className, null, invoker)
			def bytecode = ctClass.toBytecode()
			//ctClass.writeFile("D:/git-repo/java-agent-web/target/classes");
			//ctClass.detach()
			loadedClass.put(className, "")
			//println("proxy ${simpleName}.groovy")
			bytecode
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
