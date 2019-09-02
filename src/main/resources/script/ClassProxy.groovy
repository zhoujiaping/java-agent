import javassist.CtField

import java.lang.reflect.Modifier
import java.security.ProtectionDomain

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.LoaderClassPath
import javassist.bytecode.AccessFlag

import org.sirenia.agent.AssistInvoker
import org.sirenia.agent.LastModCacheUtil

import java.lang.reflect.Method
import org.sirenia.agent.JavaAgent

class ClassProxy {

	def logger = org.slf4j.LoggerFactory.getLogger('ClassProxy#proxy')
	ClassLoader cl
	def methodSuffix = '_pxy'
	def ivkName = AssistInvoker.class.name
	def shell
	ClassPool pool
	def init(ClassLoader cl0){
		cl = cl0
		pool = ClassPool.getDefault()
		pool.appendClassPath(new LoaderClassPath(cl))
		shell = new GroovyShell(cl0)
	}
	def proxyToClass(String className){
		def ct = proxy(className,null,null)
		if(ct){
			return ct.toClass()
		}
		return Class.forName(className,true,cl)
	}

	def proxy(String className, ProtectionDomain domain, byte[] bytes){
		def ctClass = proxy0(className)
		if(ctClass==null){
			return null
		}
		def ivk = new AssistInvoker(){
			/*code below will be trans to bytecode, it will load by webappclassloader,
			so, donot use code which webappclassloader cannot find!
			*/
			def invoke1(String selfClassName,Object self,String method,Class[] types,Object[] args){
				try{
					return doInvoke(selfClassName,self, method,  types, args)
				}catch(e){
					logger.error("$selfClassName,$self,$method,$types,$args")
					throw e
				}
			}

			private Object doInvoke(String selfClassName,Object self ,String method,Class[] types, Object[] args) {
				//println "ivk=====> $selfClassName,$method,$args"
				Class selfClass = Class.forName(selfClassName)
				logger.info "ivk=====> $selfClassName,$method,$args"
				//println self.getClass().classLoader
				//println selfClass.classLoader
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
					def script = shell.evaluate(file)
					if(!script){
						throw new RuntimeException("evaluate script returns null, forgot return this?")
					}
					script
				} as LastModCacheUtil.OnExpire
				def proxy = LastModCacheUtil.get(file.getAbsolutePath(), onExpire)
				proxy.metaClass.logger = org.slf4j.LoggerFactory.getLogger("${sn}#proxy")
				proxy.metaClass.shell = shell
				def methodName = thisMethod.getName()
				if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
					logger.info("ivk proxy=====> ${cn}#$methodName")
					return proxy."$methodName"(*args)
				} else {
					def ivkArgs = [self, thisMethod, proceed, args]
					if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
						logger.info("ivk proxy=====> ${cn}#$methodName-invoke")
						return proxy."${methodName}-invoke"(*ivkArgs)
					} else {
						return proceed.invoke(self, args)
					}
				}
			}
		}
		AssistInvoker.ivkMap.put(ctClass.name, ivk)
		//ctClass.toClass()
		//ctClass.toBytecode()
		ctClass
	}

    CtClass proxy0(String className){
        CtClass ct = pool.getCtClass(className)
        // 解冻
        if (ct.isFrozen()) {
            ct.defrost()
        }
		if(ct.isInterface()){
			return null
		}
		if(ct.isEnum()){
			return null
		}
		/*
        添加一个标记字段，如果已经被我们代理过，就不再代理。
        */
		try{
			ct.getDeclaredField("_pxy")
			return null
		}catch(javassist.NotFoundException e){
			logger.info("transform => $className")
			CtField ctf = new CtField(CtClass.intType,'_pxy',ct)
			ct.addField(ctf)
		}
		CtMethod[] methods = ct.getDeclaredMethods()// ct.getMethods()
		for (int i = 0;i < methods.length;i++) {
			CtMethod method = methods[i]
			int mod = method.getModifiers()
			if(Modifier.isAbstract(mod)){
				continue
			}
			String methodName = method.getName()
			if(methodName.startsWith('lambda$')){
				continue
			}
			CtMethod copyMethod = CtNewMethod.copy(method, methodName + methodSuffix, ct, null)
			/*设置方法为私有的
				错误方式：copyMethod.setModifiers(Modifier.PRIVATE)
				这样会修改去掉其他修饰符
			*/
			//mod = mod &(~(Modifier.PUBLIC|Modifier.PROTECTED|Modifier.PRIVATE));
			int accMod = AccessFlag.setPrivate(mod)
			copyMethod.setModifiers(mod&accMod)
			
			ct.addMethod(copyMethod)
			String body = ""
			if (Modifier.isStatic(mod)) {
				body = '{return ($r)$proceed("'+className+'",null,"' + methodName + '",$sig,$args);}'
			} else {
				//body = '{return ($r)$proceed($class,$0,"' + methodName + '",$sig,$args);}'
				body = '{return ($r)$proceed("'+className+'",$0,"' + methodName + '",$sig,$args);}'
			}
			method.setBody(body, ivkName, "invoke")
        }
        // Class<?> c = ct.toClass()
        // ct.writeFile("d:/")
        // ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile())
        return ct
    }
}
