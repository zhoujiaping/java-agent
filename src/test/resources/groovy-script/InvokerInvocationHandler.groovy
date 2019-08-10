import groovy.lang.GroovyShell
import org.sirenia.agent.JavaAgent
/**
实现远程dubbo服务的代理。远程服务即使没有注册到注册中心也可以。
*/
/**
args[] => target,methodName,args
*/
this._classes = new HashSet()
this._classes.add("org.wt.service.HelloService")
this._shell = new GroovyShell()
def "invoke#invoke"(self,thisMethod,proceed,args){
	def matchedInterface = args[0].getClass().getInterfaces().find{
		_classes.contains(it.getName())
	}
	if(matchedInterface){
		def className = matchedInterface.getName()
		def parts = className.split("\\.")
		def simpleName = parts[-1]
		File file = new File(JavaAgent.groovyFileDir,"/${simpleName}.groovy");
		if(!file.exists()){
			println("${simpleName}.groovy not found")
			proceed.invoke(self,args)
		}else{
			def o = _shell.evaluate(file)
			def method = args[1]
			def methodName = method.getName()
			if(o.metaClass.respondsTo(o,methodName)){
				//println('######################################'+thisMethod.getName())
				return o.invokeMethod(methodName,args[2])
			}else if(o.metaClass.respondsTo(o,methodName+"#invoke")){
				//println("")
				return o.invokeMethod(methodName+"#invoke",args)
			}
			return proceed.invoke(self,args[2])
		}
	}else{
		proceed.invoke(self,args)		
	}
}
this


