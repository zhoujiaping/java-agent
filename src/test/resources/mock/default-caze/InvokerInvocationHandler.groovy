package mock.agent

import groovy.transform.Field
import org.sirenia.agent.AssistInvoker
import org.slf4j.Logger

/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
import org.slf4j.LoggerFactory

import java.lang.reflect.Method


@Field logger = LoggerFactory.getLogger("InvokerInvocationHandlerLogger")
@Field methods
@Field includes = """
org.wt.service.HelloService
org.wt.service.RemoteUserService
""".trim().split(/\s+/) as HashSet
/**
 * 为handler动态添加一个方法，ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs是其入参。
 * 参考dubbo源码，InvokerInvocationHandler#invoke。
 * params[] => target,method,args
 */

includes = {
	->
	def xml = "/project/dubbo.xml"
	def parser = new XmlParser()
	def beans = parser.parseText( new File(xml).text)
	beans['dubbo:reference'].collect {
		ref-> ref['@interface']
	} as HashSet
}.call()
def init(methods){
	this.methods = methods
}
def "invoke-invoke" (ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs){
		//dubbo服务对象，方法名称，方法参数
	def serviceTarget = ivkArgs[0]
	Method serviceMethod = ivkArgs[1]
	def serviceArgs = ivkArgs[2]
		//获取dubbo服务对象实现的接口，按接口名匹配
	def matchedInterface = serviceTarget.getClass().getInterfaces().find{
		includes.contains(it.getName())
	}
	if(matchedInterface){
		def className = matchedInterface.getName()
		logger.info "transformer(dubbo)=> $className"

		def sn = className.split(/\./)[-1]
		def proxys = methods.proxys
		def proxy
		if(proxys[className]){
			proxy = proxys[className]
		}else if(proxys[sn]){
			proxy = proxys[sn]
		}else{
			return ivkProceed.invoke(ivkSelf,ivkArgs)
		}
		def methodName = serviceMethod.name
		if (proxy.metaClass.respondsTo(proxy, methodName, *serviceArgs)) {
			AssistInvoker.ifNotInvocationHandler(ivkSelf,()->{
				logger.info("ivk proxy(dubbo)=====> ${className}#$methodName ${ivkArgs}")
			})
			return proxy."$methodName"(*serviceArgs)
		} else if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
			AssistInvoker.ifNotInvocationHandler(ivkSelf,()->{
				logger.info("ivk proxy(dubbo)=====> ${className}#$methodName-invoke ${ivkArgs}")
			})
			return proxy."${methodName}-invoke"(*ivkArgs)
		}else {
			return ivkProceed.invoke(ivkSelf, ivkArgs)
		}

	}else{
		ivkProceed.invoke(ivkSelf,ivkArgs)
	}
}
return this


