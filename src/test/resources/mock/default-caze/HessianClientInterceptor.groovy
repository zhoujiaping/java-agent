package mock.agent
/**
 实现远程hessian服务的代理。
 */
import org.slf4j.LoggerFactory

/**
 * 尽量不要给metaClass添加字段来保存数据，容易出bug（比如重新解析类后，即使重新给metaClass.proxys赋值，也会出现proxys还是旧值的情况）。
 */
class HessianClientInterceptor {
    def logger = LoggerFactory.getLogger("HessianClientInterceptor")
    def timestamp = System.currentTimeMillis()
    def includes = """
""".trim().split(/\s+/) as HashSet
    def methods

    def init(methods) {
        this.methods = methods
        this.includes << "xxx"
    }
    //org.aopalliance.intercept.MethodInvocation
    def invoke(methodInvocation) {
        def ivkSelf = IvkJoinPointManager.currentJoinPoint().self
        def (serviceMethod, serviceArgs) = [methodInvocation.method, methodInvocation.arguments]
        def matchedInterface = includes.contains(ivkSelf.serviceInterface.name)
        if (!matchedInterface) {
            IvkJoinPointManager.currentJoinPoint().proceed()
        }
        def className = ivkSelf.serviceInterface.name
        logger.info "transformer(hessian)=> $className"
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
        proxy."${serviceMethod.name}"(*serviceArgs)
    }
}

