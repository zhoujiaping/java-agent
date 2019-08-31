package script

import org.sirenia.agent.JavaAgent
import org.sirenia.agent.AssistInvoker
import org.sirenia.agent.LastModCacheUtil
/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
import org.slf4j.LoggerFactory
includes = """
org.wt.service.HelloService
org.wt.service.RemoteUserService
""".trim().split(/\s+/) as HashSet
/**
	 params[] => target,method,args
 */
def "invoke-invoke"(ivkSelf,ivkThisMethod,ivkProceed,ivkArgs){
	def serviceTarget = ivkArgs[0]
	def serviceMethod = ivkArgs[1]
	def serviceArgs = ivkArgs[2]
	def matchedInterface = serviceTarget.getClass().getInterfaces().find{
		includes.contains(it.getName())
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
			def script = shell.evaluate(file)
			if(!script){
				throw new RuntimeException("evaluate script returns null, forgot return this?")
			}
			script
		} as LastModCacheUtil.OnExpire
		def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
		proxy.metaClass.logger = LoggerFactory.getLogger("${sn}#proxy")
		proxy.metaClass.shell = shell
		def methodName = serviceMethod.getName()
		if (proxy.metaClass.respondsTo(proxy, methodName, *serviceArgs)) {
			logger.info("ivk proxy(dubbo)=====> ${className}#$methodName ${ivkArgs}")
			return proxy."$methodName"(*serviceArgs)
		} else if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
			logger.info("ivk proxy(dubbo)=====> ${className}#$methodName-invoke ${ivkArgs}")
			return proxy."${methodName}-invoke"(*ivkArgs)
		}else {
			return ivkProceed.invoke(ivkSelf, ivkArgs)
		}

	}else{
		ivkProceed.invoke(ivkSelf,ivkArgs)
	}
}
return this



