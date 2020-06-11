package mock.agent
/**
 实现mybatis接口的代理
 */
import org.slf4j.LoggerFactory

import java.lang.reflect.Field
import java.lang.reflect.Method

class MapperProxy {
    def logger = LoggerFactory.getLogger("MapperProxyLogger")
    def methods

    def init(methods) {
        this.methods = methods
    }

    def invoke(Object pxy, Method method, Object[] args) {
        def ivkSelf = IvkJoinPointManager.currentJoinPoint().self
        //服务对象，方法名称，方法参数
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
        Field mapperInterfaceField = ivkSelf.class.declaredFields.find {
            it.name == 'mapperInterface'
        }
        String mapperInterfaceName
        if (!mapperInterfaceField.accessible) {
            mapperInterfaceField.accessible = true
            mapperInterfaceName = mapperInterfaceField.get(ivkSelf).name
            mapperInterfaceField.accessible = false
        }
        if (!(mapperInterfaceName ==~ /com.sfpay.msfs.jyd.mapper.*Mapper/)) {
            return IvkJoinPointManager.currentJoinPoint().proceed()
        }
        def className = mapperInterfaceName
        logger.info "transformer(mapper)=> $className"
        def simpleName = className.split(/\./)[-1]
        def proxys = methods.proxys
        def proxy
        if (proxys[className]) {
            proxy = proxys[className]
        } else if (proxys[simpleName]) {
            proxy = proxys[simpleName]
        } else {
            return IvkJoinPointManager.currentJoinPoint().proceed()
        }
        if (!proxy.hasProperty('overwrite-missing-method')) {
            proxy.metaClass.methodMissing = {
                methodName2, arguments ->
                    IvkJoinPointManager.currentJoinPoint().proceed()
            }
            proxy.metaClass['overwrite-missing-method'] = true
        }
        proxy."${methodName}"(*args)
    }
}



