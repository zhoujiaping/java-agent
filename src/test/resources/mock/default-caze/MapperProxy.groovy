package mock.agent

import groovy.transform.Field
import org.sirenia.agent.AssistInvoker
import org.slf4j.Logger

/**
 实现mybatis接口的代理
 */
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

@Field logger = LoggerFactory.getLogger("MapperProxyLogger")
@Field methods
def init(methods){
    this.methods = methods
}
//public Object invoke(Object proxy, Method method, Object[] args);
def "invoke-invoke" (ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs){
    //服务对象，方法名称，方法参数
    def mapperProxy = ivkArgs[0]
    Method method = ivkArgs[1]
    def args = ivkArgs[2]
    java.lang.reflect.Field mapperInterfaceField = ivkSelf.class.declaredFields.find{
        it.name == 'mapperInterface'
    }
    String mapperInterfaceName
    if(!mapperInterfaceField.accessible){
        mapperInterfaceField.accessible = true
        mapperInterfaceName = mapperInterfaceField.get(ivkSelf)
        mapperInterfaceField.accessible = false
    }
    if(mapperInterfaceName.startsWith("interface org.xxx.mapper")){
        def className = mapperInterfaceName
        logger.info "transformer(mapper)=> $className"

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
        def methodName = method.name
        if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
            AssistInvoker.ifNotInvocationHandler(ivkSelf,()->{
                logger.info("ivk proxy(mapper)=====> ${className}#$methodName ${ivkArgs}")
            })
            //logger.info("ivk proxy(mapper)=====> ${className}#$methodName")
            return proxy."$methodName"(*args)
        } else if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
            AssistInvoker.ifNotInvocationHandler(ivkSelf,()->{
                logger.info("ivk proxy(mapper)=====> ${className}#$methodName-invoke ${ivkArgs}")
            })
            //logger.info("ivk proxy(mapper)=====> ${className}#$methodName-invoke")
            return proxy."${methodName}-invoke"(*ivkArgs)
        }else {
            return ivkProceed.invoke(ivkSelf, ivkArgs)
        }
    }else{
        ivkProceed.invoke(ivkSelf,ivkArgs)
    }
}
return this


