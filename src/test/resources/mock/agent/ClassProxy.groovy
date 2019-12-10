package mock.agent

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
import org.sirenia.agent.LastModCache
import org.sirenia.agent.LastModCache.OnExpire
import java.lang.reflect.Method
import org.sirenia.agent.JavaAgent

class ClassProxy {
	//定义字段，并且自动拥有getter和setter
	def logger = org.slf4j.LoggerFactory.getLogger('ClassProxyLogger')
	def cl
	def shell
	def methodSuffix = '_pxy'
	def ivkName = AssistInvoker.class.name
	def mockCazeFile = new File(JavaAgent.mockDir,"agent/MockCaze.groovy")
	def proxysCache
	def cazeCache = new LastModCache()
	/**
	 * 测试用例名称被修改时的行为，需要重新读取测试用例名称，
	 * 并将之前解析的mock对象清空。
	 * 就把onMockCazeExpire当成一个java8的lambda表达式理解吧。
	 */
	def onMockCazeExpire = {
		proxysCache = new LastModCache()
		def newMockCaze = shell.evaluate(mockCazeFile)
		logger.info "change mock caze => from ${it?.obj?.caze} to ${newMockCaze.caze}"
		//方法最后一个表达式会被作为返回值，return时可选的
		newMockCaze
	} as OnExpire
	def pool

	def init(ClassLoader cl0){
		cl = cl0
		//javasssit api
		pool = ClassPool.getDefault()
		pool.appendClassPath(new LoaderClassPath(cl))
		//创建一个解析groovy脚本的shell对象
		shell = new GroovyShell(cl0)
	}

	def proxy(String className, ProtectionDomain domain, byte[] bytes){
		def ctClass = proxy0(className)
		if(ctClass==null){
			return null
		}
		def ivk = new AssistInvoker(){
			/*code will be trans to bytecode, it will load by webAppClassloader,
			so, do not use code which webAppClassloader cannot find!
			*/
			def invoke1(String selfClassName,Object self,String method,Class[] types,Object[] args){
				try{
					return doInvoke(selfClassName,self, method,  types, args)
				}catch(e){
					AssistInvoker.ifNotInvocationHandler(self,()->{
						logger.error("$selfClassName,$self,$method,$types,$args")
					})
					throw e
				}
			}

			private Object doInvoke(String selfClassName,Object self ,String method,Class[] types, Object[] args) {
				//println "ivk=====> $selfClassName,$method,$args"
				Class selfClass = Class.forName(selfClassName)
				//如果 args里面有一个参数是this，那么打印参数的时候，就会调用它的toString方法，然后会进入invoke方法和toString方法的死循环当中。
				if ("toString" == method && types.length == 0) {
					return self.toString()
				}
				AssistInvoker.ifNotInvocationHandler(self,()->{
					logger.info "ivk=====> $selfClassName,$method,$args"
				})
				//println self.getClass().classLoader
				//println selfClass.classLoader
				/**
				 * selfClassName:被调方法是在哪个类声明的
				 * self:调用该方法的对象，如果时静态方法，则self为null
				 * method:方法名
				 * types:方法入参类型数组
				 * args:方法入参数组
				 */
				Method thisMethod = selfClass.getDeclaredMethod(method, types)
				/**
				 * 通过反射，获取原有方法（被修改为private，并且被修改了名称）
				 */
				Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types)
				if (!proceed.isAccessible()) {
					proceed.setAccessible(true)
				}
				//获取最新的 测试用例名称
				def mockCaze = cazeCache.get(mockCazeFile.getAbsolutePath(), onMockCazeExpire)
				//class name
				def cn = selfClass.name
				//simple name
				def sn = cn.split(/\./)[-1]
				/**
				 * 获取最新的mock方法
				 */
				def methodsFile = new File(JavaAgent.mockDir, "${mockCaze.caze}/Methods.groovy")
				def onMethodsExpire = {
					def newMethods = shell.evaluate(methodsFile)
					if(!newMethods){
						throw new RuntimeException("evaluate Methods.groovy returns null, forgot return a value?")
					}
					newMethods
				} as OnExpire
				def methods = proxysCache.get(methodsFile.getAbsolutePath(),onMethodsExpire)
				def proxys = methods.proxys
				/**
				 * 当前mock对象
				 */
				def proxy
				//由于我们是对类进行增强，所以如果是dubbo接口，就需要在dubbo调用服务之前进行拦截。
				if(sn == 'InvokerInvocationHandler'){
					def dubboHandlerFile = new File(JavaAgent.mockDir, "${mockCaze.caze}/InvokerInvocationHandler.groovy")
					def onDubboHanlderExpire = {
						shell.evaluate(dubboHandlerFile)
					} as OnExpire
					def dubboHanlder = proxysCache.get(dubboHandlerFile.getAbsolutePath(),onDubboHanlderExpire)
					dubboHanlder.init methods
					proxy = dubboHanlder
				}else{
					//如果mock对象集合中找不到对应的mock对象，就调用方法原有逻辑
					if(!proxys[sn]){
						return proceed.invoke(self, args)
					}
					proxy = proxys[sn]
				}

				def methodName = thisMethod.name
				//如果有对应的mock方法，就执行它。  *args：参数展开，学过es6、python、scala的都知道。
				if (proxy.metaClass.respondsTo(proxy, methodName, *args)) {
					logger.info("ivk proxy=====> ${cn}#$methodName")
					return proxy."$methodName"(*args)
				} else {
					def ivkArgs = [self, thisMethod, proceed, args]
					//提供机制，让mock方法里面可以调用方法原有逻辑。
					if (proxy.metaClass.respondsTo(proxy, "${methodName}-invoke", *ivkArgs)) {
						logger.info("ivk proxy=====> ${cn}#$methodName-invoke")
						return proxy."${methodName}-invoke"(*ivkArgs)
					} else {
						return proceed.invoke(self, args)
					}
				}
			}
		}
		/**
		 每个类，对应一个AssistInvoker对象，注册到AssistInvoker.ivkMap中。
		 后面执行的时候，根据类名获取AssistInvoker对象。
		 通过这种方式，将编译期和运行期的行为关联起来。
		 */
		AssistInvoker.ivkMap.put(ctClass.name, ivk)
		//ctClass.toClass()
		//ctClass.toBytecode()
		ctClass
	}
	/**
	 * 该方法调用javassist的api，实现方法拦截功能。
	 * @param className
	 * @return
	 */
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
			//方法中的lambda表达式，不增强
			if(methodName.startsWith('lambda$')){
				continue
			}
			/**
			 * 将原有方法拷贝，改为私有方法，修改名称，提供调用原有方法逻辑的机会。
			 */
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
			//设置方法体，让它调用AssistInvoker#invoke
			method.setBody(body, ivkName, "invoke")
		}
		// Class<?> c = ct.toClass()
		// ct.writeFile("d:/")
		// ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile())
		return ct
	}
}

