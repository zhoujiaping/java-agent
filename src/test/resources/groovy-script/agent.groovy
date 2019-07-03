import java.lang.reflect.Method
import java.security.ProtectionDomain
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.sirenia.groovy.GroovyScriptShell
import org.sirenia.javassist.MethodInvoker
import org.sirenia.javassist.JavassistProxy
import org.sirenia.javassist.MyMethodProxy

import groovy.lang.GroovyShell
import javassist.CtClass

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
			if(className == "org.apache.catalina.loader.WebappClassLoaderBase"){
				return null;
				/*
				Thread.currentThread().setContextClassLoader(classLoader);
				println("WebappClassLoaderBase ContextClassLoader: "+classLoader)
				loadedClass.put(className, "");
				MethodInvoker invoker = new MethodInvoker(){
					@Override
					public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
						def res = "qwer"
						println("before invoke WebappClassLoaderBase")
						//调用另一个groovy脚本
						//Object o = shell.evaluate(new File("e:/groovy-script/second.groovy"))
						//o.invokeMethod("hello",["zhou"] as Object[])
						//def resp = args[1];
						//PrintWriter pw = resp.getWriter();
						//pw.print("groovy2");
						//pw.flush();
						return proceed.invoke(self,args);
					}
				};
				println("before proxy WebappClassLoaderBase")
				CtClass ctClass = JavassistProxy.proxy(className, null, invoker);
				System.out.println(className);
				return ctClass.toBytecode();*/
			}
			
			if(className=="org.sirenia.EchoServlet"){
				//return null;
				Thread.currentThread().setContextClassLoader(classLoader);
				println("EchoServlet ContextClassLoader: "+classLoader)
				loadedClass.put(className, "");
				MethodInvoker invoker = new MethodInvoker(){
					@Override
					public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
						def res = "qwer"
						println("before invoke EchoServlet")
						//调用另一个groovy脚本
						Binding binding = new Binding(['args':args]);
						Object o = shell.evaluate(new File("e:/groovy-script/second.groovy"))
						o.invokeMethod(thisMethod.getName(),args)
						return res;
					}
				};
				println("before proxy EchoServlet")
				CtClass ctClass = MyMethodProxy.proxy(className, null, invoker);
				System.out.println(className);
				return ctClass.toBytecode();
			}
			
			return null;
			
		} catch (Exception e) {
			//jvm不会立即打印错误消息，所以要手动调用打印
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}