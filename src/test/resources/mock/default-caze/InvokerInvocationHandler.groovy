package mock.agent
/**
 实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
 */
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

/**
 * 尽量不要给metaClass添加字段来保存数据，容易出bug（比如重新解析类后，即使重新给metaClass.proxys赋值，也会出现proxys还是旧值的情况）。
 */
class InvokerInvocationHandler {
    def logger = LoggerFactory.getLogger("InvokerInvocationHandlerLogger")
    def timestamp = System.currentTimeMillis()
    def includes = """
com.alibaba.dubbo.monitor.MonitorService
""".trim().split(/\s+/) as HashSet
    def methods

    def init(methods) {
        this.methods = methods
        this.includes = {
            ->
            def xml = "dubbo.xml"
            def parser = new XmlParser()
            def beans = parser.parseText(new File(xml).text)
            beans['dubbo:reference'].collect {
                ref -> ref['@interface']
            } as HashSet
        }.call()
    }

    def invoke(Object target, Method method, Object[] args) {
        def joinPoint = IvkJoinPointManager.currentJoinPoint()
        def ivkSelf = joinPoint.self
        def thisMethod = joinPoint.thisMethod
        def ivkProceed = joinPoint.proceed
        def ivkArgs = joinPoint.args
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
        def (serviceTarget, serviceMethod, serviceArgs) = ivkArgs
        Class matchedInterface = serviceTarget.class.interfaces.find {
            includes.contains(it.name)
        }
        if (matchedInterface) {
            def className = matchedInterface.name
            logger.info "transformer(dubbo)=> $className"
            def sn = className.split(/\./)[-1]
            def proxys = methods.proxys
            if (!proxys[sn]) {
                return ivkProceed.invoke(ivkSelf, ivkArgs)
            }
            def proxy = proxys[sn]
            if (!proxy.hasProperty('overwrite-missing-method')) {
                proxy.metaClass.methodMissing = {
                    methodName2, arguments ->
                        ivkProceed.invoke(ivkSelf, ivkArgs)
                }
                proxy.metaClass['overwrite-missing-method'] = true
            }
            proxy."${method.name}"(*args)
        } else {
            ivkProceed.invoke(ivkSelf, ivkArgs)
        }
    }
}

