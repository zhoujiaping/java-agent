import java.lang.reflect.Method
import java.security.ProtectionDomain
/**
 * 类名随便
 * @author Administrator
 *
 */
class MyMethodInvoker{
	 def invoke(Object self, Method thisMethod, Method proceed, Object[] args){
		 proceed.invoke(self,args)
	 }

}