package mock.agent

import org.sirenia.agent.AssistInvoker

/**
 实现mybatis接口的代理
 */
import org.slf4j.LoggerFactory

import java.lang.reflect.Field
import java.lang.reflect.Method

class MapperProxy{
    def logger = LoggerFactory.getLogger("MapperProxyLogger")
    def methods
    def init(methods){
        this.methods = methods
    }
    //public Object invoke(Object proxy, Method method, Object[] args);
    def "invoke-invoke" (ivkSelf ,ivkThisMethod,ivkProceed,ivkArgs){
        //服务对象，方法名称，方法参数
        Method method = ivkArgs[1]
        def args = ivkArgs[2]
        String methodName = method.name
        def parameterTypes = method.parameterTypes
        if (method.declaringClass == Object) {
            return method.invoke(ivkSelf, args)
        }
        if ("toString" == methodName && parameterTypes.length == 0) {
            return ivkSelf.toString()
        }
        if ("hashCode" == methodName && parameterTypes.length == 0) {
            return ivkSelf.hashCode()
        }
        if ("equals" == methodName && parameterTypes.length == 1) {
            return ivkSelf == args[0]
        }
        Field mapperInterfaceField = ivkSelf.class.declaredFields.find{
            it.name == 'mapperInterface'
        }
        String mapperInterfaceName
        if(!mapperInterfaceField.accessible){
            mapperInterfaceField.accessible = true
            mapperInterfaceName = mapperInterfaceField.get(ivkSelf).name
            mapperInterfaceField.accessible = false
        }
        if(mapperInterfaceName ==~ /com.xx.xx.xx.mapper.*Mapper/){
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
            if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
                logger.info("ivk proxy(mapper)=====> ${className}#$methodName ${ivkArgs}")
                return proxy."$methodName"(*args)
            } else if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
                logger.info("ivk proxy(mapper)=====> ${className}#$methodName-invoke ${ivkArgs}")
                return proxy."${methodName}-invoke"(*ivkArgs)
            }else {
                return ivkProceed.invoke(ivkSelf, ivkArgs)
            }
        }else{
            ivkProceed.invoke(ivkSelf,ivkArgs)
        }
    }
}



