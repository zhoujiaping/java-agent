import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.agent.groovy.GroovyScriptShell
import org.sirenia.agent.javassist.MethodInvoker
import org.sirenia.agent.javassist.JavassistProxy
import org.sirenia.agent.javassist.MyMethodProxy
import org.sirenia.agent.javassist.MethodFilter

import groovy.lang.GroovyShell
import javassist.CtClass
import javassist.CtMethod

class MyClassFileTransformer{
	GroovyShell shell = new GroovyShell()
	def loadedClass = new ConcurrentHashMap<>()
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		try{
			if(className == null){
				return null;
			}
			className = className.replace("/", ".");
			if(loadedClass.containsKey(className)){
				return null;
			}
			//配置哪些类需要被代理，如果不配置，groovy的类也会被代理。而groovy的类我们不需要代理。
			if(!className.startsWith("org.sirenia")){
				return null;
			}
			def parts = className.split('\\.')
			def simpleName = parts[-1]
			File file = new File("e:/groovy-script/${simpleName}.groovy");
			if(!file.exists()){
				return null;
			}
			loadedClass.put(className, "");
			MethodInvoker invoker = new MethodInvoker(){
						@Override
						public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
							//调用另一个groovy脚本
							Object o = shell.evaluate(file)
							if(o[thisMethod.getName()] == null){
								return proceed.invoke(self,args)
							}
							return o.invokeMethod(thisMethod.getName(),args)
						}
					};
			CtClass ctClass = MyMethodProxy.proxy(className, null, invoker);
			return ctClass.toBytecode();
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}