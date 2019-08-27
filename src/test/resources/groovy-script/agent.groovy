import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.concurrent.ConcurrentHashMap

import org.sirenia.agent.AssistProxy
import org.sirenia.agent.AssistInvoker
import org.sirenia.agent.JavaAgent
//import org.sirenia.agent.util.LastModCacheUtil
import org.codehaus.groovy.control.CompilerConfiguration

/*
-javaagent:d:/git-repo/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=/tomcat/groovy
-javaagent:/home/wt/IdeaProjects/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar
*/
//TODO
class MyClassFileTransformer{
	//记录已代理过的类名
	def loadedClass = new ConcurrentHashMap()
	def classSet = new HashSet()
	def matchReg = /org.wt.*(Mapper|Service|Component|Controller).*/
	def classes = """
org.wt.util.CryptUtils
org.wt.model.User
"""
	MyClassFileTransformer(){
		init()
	}
	def init(){
		println "#"*10+"init agent"+"#"*10
		classSet = classes.trim().split(/\s+/).findAll{!it.endsWith("//")} as HashSet
		//classSet << "com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler" //通过代理dubbo的InvokerInvocationHandler，实现对远程dubbo服务的代理
		classSet << "com.alibaba.dubbo.common.bytecode.ClassGenerator" //兼容dubbo的代理
	}
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		if (classLoader.getClass().getName().contains('GroovyClassLoader')) {
			return null
		}
		if (className in loadedClass) {
			return null
		}
		//类名有可能是null
		if (className == null) {
			return null
		}
		//类名使用/分隔的，替换成.分隔
		className = className.replace("/", ".")
		//已经代理过，不需要再代理，直接返回null
		if (loadedClass.containsKey(className)) {
			return null
		}
		//对java-agent项目中的类直接放行，不代理
		if (className.startsWith("org.sirenia")) {
			return null
		}
		//这里配置class name regexp to proxy的正则表达式，这样在mock时，不需要重启应用。
		def matchRes = className ==~ matchReg
		//不需要代理的类，放行
		if (!matchRes && !classSet.contains(className)) {
			return null
		}
		loadedClass.put(className, "")
		//println "transformer => $className"
		def parts = className.split(/\./)
		def simpleName = parts[-1]
		File file = new File(JavaAgent.groovyFileDir, "${simpleName}.groovy")
		if(!file.exists()){
			return null
		}
		println "transformer => $className"
		
		def config = new CompilerConfiguration()
		config.setSourceEncoding("UTF-8")
		def groovyClassLoader = new GroovyClassLoader(classLoader, config)
		def proxy = groovyClassLoader.parseClass(file).newInstance()
		
		//每个类加载器，使用
		def ivk = new AssistInvoker(){
			Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args){
				println "ivk=====> $selfClass,$method,$args"
				Method thisMethod = selfClass.getDeclaredMethod(method, types);
				Method proceed = selfClass.getDeclaredMethod(method+AssistProxy.methodSuffix, types);
				if(!proceed.isAccessible()){
					proceed.setAccessible(true);
				}
				if (proxy.metaClass.respondsTo(proxy, method, *args)) {
					proxy."$method"(*args)
				} else {
					def ivkArgs = [self, thisMethod, proceed, args]
					if (proxy.metaClass.respondsTo(proxy, "${method}#invoke", *ivkArgs)) {
						proxy."${method}#invoke"(*ivkArgs)
					}else {
						proceed.invoke(self, args)
					}
				}
				//super.invoke(selfClass,self,method,types,args)
			}
		}
		def ctClass = AssistProxy.proxy(classLoader,className,ivk)
		
		/*
		//即使对应的代理文件不存在，也进行代理，这样添加文件的时候，不需要重启。
		def invoker = new MethodInvoker() {
			@Override
			def invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				//调用另一个groovy脚本
				if (!file.exists()) {
					return proceed.invoke(self, args)
				}
				def onExpire = {
					//dont use GroovyShell, becuase it can not use classes in your project
					def config = new CompilerConfiguration()
					config.setSourceEncoding("UTF-8")
					def groovyClassLoader = new GroovyClassLoader(classLoader, config)
					groovyClassLoader.parseClass(file).newInstance()
				} as LastModCacheUtil.OnExpire
				def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
				proxy.metaClass['#classLoader'] = classLoader
				def methodName = thisMethod.getName()
				if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
					proxy."$methodName"(*args)
				} else {
					def ivkArgs = [self, thisMethod, proceed, args]
					if (proxy.metaClass.respondsTo(proxy, "${methodName}#invoke", *ivkArgs)) {
						proxy."${methodName}#invoke"(*ivkArgs)
					}else {
						proceed.invoke(self, args)
					}
				}
			}
		}
		//使用javassist工具，增强字节码，进行代理
		
		def ctClass = MyMethodProxy.proxy(classLoader,className, null, invoker)*/
		def bytecode = ctClass.toBytecode()
		//ctClass.writeFile("D:/git-repo/java-agent-web/target/classes");
		//ctClass.detach()
		
		//println("proxy ${simpleName}.groovy")
		bytecode
	}
}
