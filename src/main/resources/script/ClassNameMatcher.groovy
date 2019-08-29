//unsupport slf4j,because it is not in webappclassloader env

this.classSet = new HashSet()
this.matchReg = /org.wt.*(Mapper|Service|Component|Controller).*/

def classes = """
org.wt.util.CryptUtils
org.wt.model.User
"""
classSet = classes.trim().split(/\s+/).findAll{!it.endsWith("//")} as HashSet
classSet << "com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler" //通过代理dubbo的InvokerInvocationHandler，实现对远程dubbo服务的代理

def match(String className){
    //对java-agent项目中的类直接放行，不代理
    if (className.startsWith("org.sirenia")) {
        return false
    }
    if (className.contains('$')){
        return false
    }
    //这里配置class name regexp to proxy的正则表达式，这样在mock时，不需要重启应用。
    def matchRes = className ==~ matchReg
    //不需要代理的类，放行
    if (!matchRes && !classSet.contains(className)) {
        return false
    }
    return true
}
return this

