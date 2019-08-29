import java.security.ProtectionDomain
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import org.sirenia.agent.JavaAgent
import org.codehaus.groovy.control.CompilerConfiguration
/*
-javaagent:d:/git-repo/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=/tomcat/groovy
-javaagent:/home/wt/IdeaProjects/java-agent/target/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar
*/
info("init ClassFileTransformer")
config = new CompilerConfiguration()
config.setSourceEncoding("UTF-8")
classLoaderProxyMap = new ConcurrentHashMap()

def shell = new GroovyShell()
File file = new File(JavaAgent.groovyFileDir, "ClassNameMatcher.groovy")
classNameMatcher = shell.evaluate(file)

def info(String msg){
    def time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
    println "[$time] $msg"
}
def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
    return transform0(classLoader, className, domain, bytes)
}

def transform0(ClassLoader classLoader, String className, ProtectionDomain domain, byte[] bytes) {
    if (classLoader.getClass().name.contains('GroovyClassLoader')) {
        return null
    }

    //类名使用/分隔的，替换成.分隔
    className = className.replace("/", ".")

    if (!classNameMatcher.match(className)) {
        return null
    }

    def classProxy = classLoaderProxyMap[classLoader]
    if (!classProxy) {
        File file = new File(JavaAgent.groovyFileDir, "ClassProxy.groovy")
        def clName = classLoader.toString()
        if(clName.length()>80){
            clName = clName.substring(0,80)+'...'
        }
        info "parse ClassProxy for $clName"
        def groovyClassLoader = new GroovyClassLoader(classLoader, config)
        classProxy = groovyClassLoader.parseClass(file).newInstance()
        classLoaderProxyMap[classLoader] = classProxy
        classProxy.init(classLoader)
    }
    def ctClass = classProxy.proxy(className,domain,bytes)
    if(ctClass){
        return ctClass.toBytecode()
    }
    return null
}
return this