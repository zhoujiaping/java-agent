import org.sirenia.agent.JavaAgent
import org.sirenia.agent.util.LastModCacheUtil
import org.codehaus.groovy.control.CompilerConfiguration
/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/

class InvokerInvocationHandler{
	def logger = org.slf4j.LoggerFactory.getLogger 'InvokerInvocationHandler#groovy'
	def classes = """
org.wt.service.HelloService
org.wt.service.RemoteUserService
""".trim().split(/\s+/) as HashSet
			def shell = new GroovyShell()
	/**
	 params[] => target,method,args
	 */
	def "invoke#invoke"(self,thisMethod,proceed,args){
		def serviceTarget = args[0]
		def serviceMethod = args[1]
		def serviceArgs = args[2]
		def matchedInterface = serviceTarget.getClass().getInterfaces().find{
			classes.contains(it.getName())
		}
		if(matchedInterface){
			def className = matchedInterface.getName()
			logger.info "transformer(dubbo)=> $className"

			
			def parts = className.split(/\./)
			def simpleName = parts[-1]
			File file = new File(JavaAgent.groovyFileDir,"/dubbo/${simpleName}.groovy")
			if(!file.exists()){
				println("${simpleName}.groovy not found")
				proceed.invoke(self,args)
			}else{
				def onExpire = {
					//dont use GroovyShell, becuase it can not use classes in your project
					def config = new CompilerConfiguration()
					config.setSourceEncoding("UTF-8")
					def groovyClassLoader = new GroovyClassLoader(this['#classLoader'], config)
					groovyClassLoader.parseClass(file).newInstance()
				} as LastModCacheUtil.OnExpire
				def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
				proxy.metaClass['#classLoader'] = this['#classLoader']
				def methodName = serviceMethod.getName()
				if(proxy.metaClass.respondsTo(proxy,methodName,*serviceArgs)){
					proxy."$methodName"(*serviceArgs)
				}else if(proxy.metaClass.respondsTo(proxy,"${methodName}#invoke",*args)){
					proxy."${methodName}#invoke"(*args)
				}else{
					proceed.invoke(self, args)
				}
			}
		}else{
			proceed.invoke(self,args)
		}
	}

}



