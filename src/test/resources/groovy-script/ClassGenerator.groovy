import org.sirenia.agent.util.ClassPoolUtils

import javassist.CtClass
import javassist.ClassPool
/**
 * 这个是用来兼容dubbo的。
 * */
class ClassGenerator{
	def getClassPool(ClassLoader loader) {
		//println("#############################getClassPool")
		def cc = ClassPool.getDefault()//ClassPoolUtils.linkClassPool(loader);
		System.out.println("###############################");
		System.out.println(cc.getClass().getClassLoader());//appclassloader
		System.out.println(CtClass.class.getClassLoader());//appclassloader或者webappclassloader
		cc
	}
}
