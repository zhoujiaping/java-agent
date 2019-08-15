import groovy.lang.GroovyShell
import org.sirenia.agent.JavaAgent
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
def invoke(name,params){
	def invokerSelf = params[0]
	def invokerThisMethod = params[1]
	def invokerProceed = params[2]
	def invokerArgs = params[3]
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
		File file = new File(JavaAgent.groovyFileDir,"/${simpleName}.groovy");
		if(!file.exists()){
			println("${simpleName}.groovy not found")
			invokerProceed.invoke(invokerSelf,invokerArgs)
		}else{
			def o = shell.evaluate(file)
			def methodName = serviceMethod.getName()
			o.metaClass.methodMissing = {
				mn,ps->
				invokerProceed.invoke(invokerSelf,invokerArgs)
			}
			o.invokeMethod(methodName, serviceArgs)
		}
	}else{
		invokerProceed.invoke(invokerSelf,invokerArgs)
	}
}
this


