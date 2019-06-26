import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.groovy.GroovyScriptShell
import org.sirenia.javassist.MethodInvoker
import org.sirenia.javassist.MyMethodProxy

import groovy.lang.GroovyShell
import javassist.CtClass

class MyClassFileTransformer{
	GroovyShell shell = new GroovyShell()
	def loadedClass = new ConcurrentHashMap<>()
	def transform(ClassLoader classLoader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes){
		if(className == null){
			return null;
		}
		className = className.replace("/", ".");
		if(!className.contains("One")){
			return null;
		}
		if(loadedClass.containsKey(className)){
			return null;
		}
		loadedClass.put(className, "");
		MethodInvoker invoker = new MethodInvoker(){
			@Override
			public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
				def res = "qwer"
				//调用另一个groovy脚本
				Object o = shell.evaluate(new File("e:/second.groovy"))
				o.invokeMethod("hello",["zhou"] as Object[])
				return res;
			}
		};
		try {
			CtClass ctClass = MyMethodProxy.proxy(className, null, invoker);
			System.out.println(className);
			return ctClass.toBytecode();
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}