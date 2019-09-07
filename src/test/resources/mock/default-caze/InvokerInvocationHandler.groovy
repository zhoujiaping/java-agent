package mock.agent

/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
import org.slf4j.LoggerFactory

def handler = new Expando()
handler.logger = LoggerFactory.getLogger("InvokerInvocationHandlerLogger")
handler.includes = """
org.wt.service.HelloService
org.wt.service.RemoteUserService
""".trim().split(/\s+/) as HashSet
/**
	 params[] => target,method,args
 */
handler.metaClass."invoke-invoke" << {
	ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs ->
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

		if(!proxys[sn]){
			return ivkProceed.invoke(ivkSelf,ivkArgs)
		}
		def proxy = proxys[sn]
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
return handler


