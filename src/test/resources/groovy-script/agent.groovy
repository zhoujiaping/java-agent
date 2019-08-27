import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.agent.javassist.MethodInvoker
import org.sirenia.agent.javassist.MyMethodProxy
import org.sirenia.agent.JavaAgent
import org.sirenia.agent.util.LastModCacheUtil
import org.codehaus.groovy.control.CompilerConfiguration

/*
-javaagent:d:/git-repo/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=/tomcat/groovy
-javaagent:/home/wt/IdeaProjects/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar

*/
//@groovy.util.logging.Slf4j
class MyClassFileTransformer{
	//def logger = LoggerFactory.getLogger(MyClassFileTransformer.class);
	//执行groovy脚本的shell
	//记录已代理过的类名
	def loadedClass = new ConcurrentHashMap()
	def classSet = new HashSet()
	MyClassFileTransformer(){
		init()
	}
	def init(){
		println "#"*10+"init agent"+"#"*10
		def classes = """
org.wt.util.CryptUtils
org.wt.model.User
"""
		classSet = classes.trim().split(/\s+/).findAll{!it.endsWith("//")} as HashSet
		classSet << "com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler" //通过代理dubbo的InvokerInvocationHandler，实现对远程dubbo服务的代理
		classSet << "com.alibaba.dubbo.common.bytecode.ClassGenerator" //兼容dubbo的代理
	}
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		try{
			return doTransform(classLoader, className)
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace()
			throw new RuntimeException(e)
		}
	}

	private byte[] doTransform(ClassLoader classLoader, String className) {
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
		def matchReg = className ==~ /org.wt.*(Mapper|Service|Component|Controller).*/
		//不需要代理的类，放行
		def needProxy = matchReg || classSet.contains(className)
		if (!needProxy) {
			//默认类名匹配以下正则的进行代理
			return null
		}
		//println "transformer => $className"
		def parts = className.split(/\./)
		def simpleName = parts[-1]
		File file = new File(JavaAgent.groovyFileDir, "${simpleName}.groovy")
		println "transformer => $className"
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
		def ctClass = MyMethodProxy.proxy(className, null, invoker)
		def bytecode = ctClass.toBytecode()
		//ctClass.writeFile("D:/git-repo/java-agent-web/target/classes");
		//ctClass.detach()
		loadedClass.put(className, "")
		//println("proxy ${simpleName}.groovy")
		bytecode
	}
}
