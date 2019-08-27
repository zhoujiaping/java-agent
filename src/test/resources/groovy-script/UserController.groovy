import com.alibaba.fastjson.JSONObject
class UserController{
	def logger = org.slf4j.LoggerFactory.getLogger 'User#groovy'
	def 'login#invoke'(self, thisMethod, proceed, args){
		logger.info "wtf"
		def session = args[2]
		//session.setAttribute('whosyourdaddy','开启无敌模式')
		//proceed.invoke(self,args)
		def jsonstr = JSONObject.toJSONString(new org.wt.model.User(name:'yyy'))
		def json = JSONObject.parseObject(jsonstr)
		json.put("mock","MOCK")
		json
	}
	def hello(){
		logger.info 'hello'+"*"*10
	}
}
