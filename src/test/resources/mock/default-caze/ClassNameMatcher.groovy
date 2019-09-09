//unsupport slf4j,because it is not in webappclassloader env
//if you modify this file, it works only when the server restarted!!!
/**
 * includes:哪些类需要被增强
 * excludes:哪些类不需要被增强
 * matchReg:根据正则表达式匹配类名决定哪些类要被增强
 * */
includes = new HashSet()
excludes = """

""".trim().split(/\s+/) as HashSet
matchReg = /org.wt.*(Mapper|Service|Component|Controller).*/
//技巧：通过在类名前面加上一些字符，使它在HashSet中的值，匹配不到任何类名，达到将其注释掉的效果。
def classes = """
org.wt.util.CryptUtils
//org.wt.model.User
org.wt.aop.AspectTest
"""
includes = classes.trim().split(/\s+/) as HashSet
//通过代理dubbo的InvokerInvocationHandler，实现对远程dubbo服务的代理。相当于includes.add(xxx)
includes << "com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler"

boolean match(String className){
    if( className in excludes){
        return false
    }
    //对java-agent项目中的类直接放行，不代理
    /*if (className.startsWith("org.sirenia")) {
        return false
    }*/
    if (className.contains('$')){
        return false
    }
    //这里配置class name regexp to proxy的正则表达式，这样在mock时，不需要重启应用。 ==~ 运算符用于正则匹配
    def matchRes = className ==~ matchReg
    //不需要代理的类，放行
    if (!matchRes && !includes.contains(className)) {
        return false
    }
    return true
}
return this

