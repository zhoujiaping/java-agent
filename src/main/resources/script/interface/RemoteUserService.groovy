import org.wt.model.User
class Mock{
	def login(name,pwd){
		logger.info "$name $pwd"
		//new User(name:'baizhan',password:'987',nick:'')
		def userService = org.wt.context.AppContextHolder.appContext.getBean('userService')
		userService.login(name,pwd)
	}
}
