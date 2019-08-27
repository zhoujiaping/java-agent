package org.sirenia.agent.javassist

import org.codehaus.groovy.control.CompilerConfiguration
import org.sirenia.agent.AssistInvoker
import org.sirenia.agent.AssistProxy
import org.sirenia.agent.LastModCacheUtil

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.agent.JavaAgent

class ClassProxy {

	def logger = org.slf4j.LoggerFactory.getLogger('ClassProxy')
	ClassLoader cl
	def groovyClassLoader
	def parseTimeMap = new ConcurrentHashMap()

	def init(ClassLoader cl0){
		cl = cl0
		def config = new CompilerConfiguration()
		config.setSourceEncoding("UTF-8")
		groovyClassLoader = new GroovyClassLoader(cl0, config)
	}

	def proxy(String className){
		def parts = className.split(/\./)
		def simpleName = parts[-1]

		def ivk = new AssistInvoker(){
			/*code below will be trans to bytecode, it will load by webappclassloader,
			so, donot use code which webappclassloader cannot find!
			*/
			Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args){
				try{
					return doInvoke(selfClass, method, args, types, self)
				}catch(e){
					logger.error("$selfClass,$self,$method,$types,$args")
					throw e
				}
			}

			private Object doInvoke(Class<?> selfClass, String method, Object[] args, Class<?>[] types, self) {
				logger.debug "ivk=====> $selfClass,$method,$args"
				Method thisMethod = selfClass.getDeclaredMethod(method, types);
				Method proceed = selfClass.getDeclaredMethod(method + AssistProxy.methodSuffix, types);
				if (!proceed.isAccessible()) {
					proceed.setAccessible(true);
				}
				//
				def cn = selfClass.name
				def sn = cn.split(/\./)[-1]
				File file
				if(sn =='ClassGenerator' || sn == 'InvokerInvocationHandler'){
					file = new File(JavaAgent.groovyFileDir, "${sn}.groovy")
				}else{
					file = new File(JavaAgent.groovyFileDir, "impl/${sn}.groovy")
				}
				if (!file.exists()) {
					logger.info("${file} not found")
					return proceed.invoke(self, args)
				}

				def onExpire = {
					groovyClassLoader.parseClass(file).newInstance()
				} as LastModCacheUtil.OnExpire
				def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
				proxy.metaClass.logger = org.slf4j.LoggerFactory.getLogger("${sn}#proxy")
				proxy.metaClass.groovyClassLoader = groovyClassLoader
				def methodName = thisMethod.getName()
				if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
					logger.debug("invoke proxy method $methodName ${args}")
					return proxy."$methodName"(*args)
				} else {
					def ivkArgs = [self, thisMethod, proceed, args]
					if (proxy.metaClass.respondsTo(proxy, "${methodName}#invoke", *ivkArgs)) {
						logger.debug("invoke proxy method $methodName#invoke ${args}")
						return proxy."${methodName}#invoke"(*ivkArgs)
					} else {
						return proceed.invoke(self, args)
					}
				}
			}
		}
		def ctClass = AssistProxy.proxy(cl,className,ivk)
		ctClass.toBytecode()
	}
}
