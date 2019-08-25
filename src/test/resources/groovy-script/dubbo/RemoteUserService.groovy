import org.wt.model.User
class RemoteUserService{
	def logger = org.slf4j.LoggerFactory.getLogger 'RemoteUserService#groovy'
	def login(name,pwd){
		//new User(name:'baizhan',password:'987',nick:'')
		def userService = org.wt.context.AppContextHolder.appContext.getBean('userService')
		userService.login(name,pwd)
	}
}
