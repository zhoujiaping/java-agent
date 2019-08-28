import java.lang.reflect.Modifier
import java.util.Map

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.LoaderClassPath
import org.sirenia.agent.AssistInvoker
import org.codehaus.groovy.control.CompilerConfiguration
import org.sirenia.agent.LastModCacheUtil

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.agent.JavaAgent

class ClassProxy {

	def logger = org.slf4j.LoggerFactory.getLogger('ClassProxy')
	def cl
	def groovyClassLoader
	def methodSuffix = AssistInvoker.methodSuffix
	def ivkName = AssistInvoker.class.name
	ClassPool pool
	def init(ClassLoader cl0){
		cl = cl0
		def config = new CompilerConfiguration()
		config.setSourceEncoding("UTF-8")
		groovyClassLoader = new GroovyClassLoader(cl0, config)
		pool = ClassPool.getDefault()
		pool.appendClassPath(new LoaderClassPath(cl))
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
				Method thisMethod = selfClass.getDeclaredMethod(method, types)
				Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types)
				if (!proceed.isAccessible()) {
					proceed.setAccessible(true)
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
		def ctClass = proxy(className,ivk)
		ctClass.toBytecode()
	}

    CtClass proxy(String className, AssistInvoker ivk){
        CtClass ct = pool.getCtClass(className)
        if(ct.isInterface()){
            return ct
        }
        // 解冻
        if (ct.isFrozen()) {
            ct.defrost()
        }
		/*TODO
        dubbo/spring 使用javassist时，会读取class文件，而我们修改的class并没有写到class文件中，
        所以我们修改的内容，在这种情况会失效。我们需要重新代理。
        添加一个标记字段，如果已经被我们代理过，就不再代理。
        * */
		int mod = method.getModifiers()
		CtMethod[] methods = ct.getDeclaredMethods()// ct.getMethods()
		for (int i = 0;i < methods.length;i++) {
			CtMethod method = methods[i]
			String methodName = method.getName()
			if(methodName.startsWith('lambda$')){
				continue
			}
			CtMethod copyMethod = CtNewMethod.copy(method, methodName + methodSuffix, ct, null)
			copyMethod.setModifiers(Modifier.PRIVATE)
			ct.addMethod(copyMethod)
            String body = ""
            if (Modifier.isStatic(mod)) {
                body = '{return ($r)$proceed($class,null,"'+ methodName+'",$sig,$args);}'
            } else {
                body = '{return ($r)$proceed($class,$0,"'+methodName+'",$sig,$args);}'
            }
            String delegateName = """($ivkName)${ivkName}.ivkMap.get("$ct.name")"""
            method.setBody(body, delegateName, "invoke")
        }
        if (ivk) {
            AssistInvoker.ivkMap.put(ct.getName(), ivk)
        }
        // Class<?> c = ct.toClass()
        // ct.writeFile("d:/")
        // ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile())
        return ct
    }
}
