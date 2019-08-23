import org.sirenia.agent.util.ClassPoolUtils
/**
 * 这个是用来兼容dubbo的。
 * */
def getClassPool(loader){
	//println("#############################getClassPool")
	ClassPoolUtils.linkClassPool(loader);
}
this
