package mock.agent

import groovy.transform.Field
import javassist.CtClass

import java.security.ProtectionDomain
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import org.sirenia.agent.JavaAgent
import org.codehaus.groovy.control.CompilerConfiguration
/*
-javaagent:/home/wt/IdeaProjects/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar
-Dmock.dir=/home/wt/mock
*/
/**
 * 由于这个时候，web项目的jar包还没加载，不能使用slf4j。
 * 所以调用自己定义的打印日志的方法。为了减少bug出现的机会，该项目没有实现自己的日志。
 * 这里只是在控制台简单的打印日志。
 */
info"init ClassFileTransformer"
/**
 * 执行groovy脚本的配置
 */
@Field config = new CompilerConfiguration()
config.sourceEncoding = "UTF-8"
/**
 * 为每个Classoader使用一个ClassProxy对象
 */
@Field classLoaderProxyMap = new ConcurrentHashMap()

/**
 * 用于执行groovy脚本
 */
@Field shell = new GroovyShell()
/**
 * 解析groovy脚本，获得当前配置的 测试用例名称。
 */
@Field mockCaze = shell.evaluate(new File(JavaAgent.mockDir,"agent/MockCaze.groovy"))
/**
 * 解析groovy脚本，获得 类名匹配器，只有匹配的类，才会被增强。
 */
@Field classNameMatcher = shell.evaluate(new File(JavaAgent.mockDir, "${mockCaze.caze}/ClassNameMatcher.groovy"))

/**
 * 自定义简单的打印日志方法
 */
def info(String msg){
    def time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
    println "[$time] $msg"
}
/**
 * 类加载的时候，会执行该方法。
 * @param classLoader
 * @param className
 * @param clazz
 * @param domain
 * @param bytes
 * @return
 */
def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
    return transform0(classLoader, className, domain, bytes)
}
/**
 * 对字节码进行转换
 * @param classLoader
 * @param className
 * @param domain
 * @param bytes
 * @return
 */
def transform0(ClassLoader classLoader, String className, ProtectionDomain domain, byte[] bytes) {
    if (classLoader.getClass().name.contains('GroovyClassLoader')) {
        //如果不需要对该类进行增强，就返回null
        return null
    }

    //类名使用/分隔的，替换成.分隔
    className = className.replace("/", ".")

    /**
     * 类名不匹配，则不增强
     */
    if (!classNameMatcher.match(className)) {
        return null
    }

    def classProxy = classLoaderProxyMap[classLoader]
    if (!classProxy) {
        File file = new File(JavaAgent.mockDir, "agent/ClassProxy.groovy")
        def clName = classLoader.toString()
        if(clName.length()>80){
            clName = clName.substring(0,80)+'...'
        }
        // 等价于 info("some msg"),groovy 语法特性
        info "parse ClassProxy for $clName"
        //解析ClassProxy，创建对象，用于对字节码进行增强
        def groovyClassLoader = new GroovyClassLoader(classLoader, config)
        classProxy = groovyClassLoader.parseClass(file).newInstance()
        //缓存起来，下一次同一个类加载器就不需要重复创建ClassProxy
        classLoaderProxyMap[classLoader] = classProxy
        classProxy.init(classLoader)
    }
    //对类进行增强
    def ctClass = classProxy.proxy(className,domain,bytes)
   /* CtClass c = ctClass
    c.writeFile("d:/gen-class")*/
    //有些类命中了匹配规则，但是对其进行增强没有什么意义，或者它是一个接口或者枚举，返回的俄ctClass就会为null
    if(ctClass){
        return ctClass.toBytecode()
    }
    return null
}
return this