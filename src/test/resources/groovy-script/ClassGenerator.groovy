import org.sirenia.agent.util.ClassPoolUtils
/**
 * 这个是用来兼容dubbo的。
 * */
class ClassGenerator{
	def getClassPool(ClassLoader loader) {
		//println("#############################getClassPool")
		ClassPoolUtils.linkClassPool(loader);
	}
}
