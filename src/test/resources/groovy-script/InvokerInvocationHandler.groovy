import org.sirenia.agent.JavaAgent
import org.sirenia.agent.util.LastModCacheUtil

/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
/**
params[] => target,method,args
*/
classes = """
org.wt.service.HelloService
org.wt.service.HelloService
org.wt.service.HelloService
org.wt.service.HelloService
org.wt.service.HelloService
org.wt.service.HelloService

org.wt.service.HelloService
""".trim().split(/\s+/) as HashSet
shell = new GroovyShell()
def "invoke#invoke"(self,thisMethod,proceed,args){
	def serviceTarget = args[0]
	def serviceMethod = args[1]
	def serviceArgs = args[2]
	def matchedInterface = serviceTarget.getClass().getInterfaces().find{
		classes.contains(it.getName())
	}
	if(matchedInterface){
		def className = matchedInterface.getName()
		def parts = className.split(/\./)
		def simpleName = parts[-1]
		File file = new File(JavaAgent.groovyFileDir,"/${simpleName}.groovy")
		if(!file.exists()){
			println("${simpleName}.groovy not found")
			proceed.invoke(self,args)
		}else{
			def proxy = LastModCacheUtil.get(file.getAbsolutePath(),()->{
				shell.evaluate(file)
			})
			def methodName = serviceMethod.getName()
			if(proxy.metaClass.respondsTo(proxy,methodName,*serviceArgs)){
				proxy."$methodName"(*serviceArgs)
			}else if(proxy.metaClass.respondsTo(proxy,"${methodName}#invoke",*args)){
				proxy."${methodName}#invoke"(*args)
			}else{
				proceed.invoke(self, args)
			}
		}
	}else{
		proceed.invoke(self,args)
	}
}
this


