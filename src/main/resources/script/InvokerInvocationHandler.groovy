package script

import org.sirenia.agent.JavaAgent
import org.sirenia.agent.LastModCacheUtil
/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
import org.slf4j.LoggerFactory
class InvokerInvocationHandler{
	def classes = """
org.wt.service.HelloService
org.wt.service.RemoteUserService
""".trim().split(/\s+/) as HashSet
/**
	 params[] => target,method,args
 */
	def "invoke#invoke"(ivkSelf,ivkThisMethod,ivkProceed,ivkArgs){
		def serviceTarget = ivkArgs[0]
		def serviceMethod = ivkArgs[1]
		def serviceArgs = ivkArgs[2]
		def matchedInterface = serviceTarget.getClass().getInterfaces().find{
			classes.contains(it.getName())
		}
		if(matchedInterface){
			def className = matchedInterface.getName()
			logger.info "transformer(dubbo)=> $className"

			def sn = className.split(/\./)[-1]
			File file = new File(JavaAgent.groovyFileDir,"interface/${sn}.groovy")

			if(!file.exists()){
				logger.info("${file} not found")
				return ivkProceed.invoke(ivkSelf, ivkArgs)
			}

			def onExpire = {
				groovyClassLoader.parseClass(file).newInstance()
			} as LastModCacheUtil.OnExpire
			def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
			proxy.metaClass.logger = LoggerFactory.getLogger("${sn}#proxy")
			proxy.metaClass.groovyClassLoader = groovyClassLoader
			def methodName = serviceMethod.getName()
			if (proxy.metaClass.respondsTo(proxy, methodName, *serviceArgs)) {
				logger.debug("invoke proxy(dubbo) method $methodName ${serviceArgs}")
				return proxy."$methodName"(*serviceArgs)
			} else if (proxy.metaClass.respondsTo(proxy, "${methodName}#invoke", *ivkArgs)) {
				logger.debug("invoke proxy(dubbo) method $methodName#invoke ${ivkArgs}")
				return proxy."${methodName}#invoke"(*ivkArgs)
			}else {
				return ivkProceed.invoke(ivkSelf, ivkArgs)
			}

		}else{
			ivkProceed.invoke(ivkSelf,ivkArgs)
		}
	}

}



