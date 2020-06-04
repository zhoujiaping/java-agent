package mock.agent

import org.sirenia.agent.AssistInvoker

/**
实现远程hessian服务的代理。
*/
import org.slf4j.LoggerFactory

/**
 * 尽量不要给metaClass添加字段来保存数据，容易出bug（比如重新解析类后，即使重新给metaClass.proxys赋值，也会出现proxys还是旧值的情况）。
 */
class HessianServiceLogInterceptor{
	def logger = LoggerFactory.getLogger("HessianServiceLogInterceptor")
	def timestamp = System.currentTimeMillis()
	def includes = """
a.b.c.SomeHessianInterface
""".trim().split(/\s+/) as HashSet
	def methods
	def init(methods){
		this.methods = methods
	}
	/**
	 params[] => target,method,args
	 */
	def "invoke-invoke"(ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs){
        def arg = ivkArgs[0]//import org.aopalliance.intercept.MethodInvocation
        def (serviceMethod,serviceArgs) = [arg.method,arg.arguments]
		def matchedInterface = includes.contains(ivkSelf.serviceInterface.name)
		if(matchedInterface){
			def className = ivkSelf.serviceInterface.name
			logger.info "transformer(hessian)=> $className"

			def sn = className.split(/\./)[-1]
			def proxys = methods.proxys
			if(!proxys[sn]){
				return ivkProceed.invoke(ivkSelf,ivkArgs)
			}
			def proxy = proxys[sn]
			def methodName = serviceMethod.name
			if (proxy.metaClass.respondsTo(proxy, methodName, *serviceArgs)) {
                logger.info("ivk proxy(hessian)=====> ${className}#$methodName ${ivkArgs}")
				proxy."$methodName"(*serviceArgs)
			} else if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
                logger.info("ivk proxy(hessian)=====> ${className}#$methodName-invoke ${ivkArgs}")
				proxy."${methodName}-invoke"(*ivkArgs)
			}else {
				ivkProceed.invoke(ivkSelf, ivkArgs)
			}
		}else{
			ivkProceed.invoke(ivkSelf,ivkArgs)
		}
	}
}

