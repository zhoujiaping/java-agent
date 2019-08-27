package script

import javassist.ClassPool
import javassist.LoaderClassPath
/**
 * 这个是用来兼容dubbo的。
 * 为了简化实现，统一使用全局、单例的ClassPool.
 * 在一个tomcat下面部署多个应用，可能会有问题。
 * 一般一个tomcat部署一个应用的没有什么问题。
 * */
class ClassGenerator{
	def getClassPool(ClassLoader loader) {
		def pool = ClassPool.getDefault()
		pool.appendClassPath(new LoaderClassPath(loader));
		pool
	}
}
