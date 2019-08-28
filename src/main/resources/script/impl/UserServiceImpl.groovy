import com.alibaba.fastjson.JSONObject
class HelloServiceImpl{
	def logger = org.slf4j.LoggerFactory.getLogger 'HelloServiceImpl#groovy'
	def login(name,pwd){
		new org.wt.model.User(name:'kakaxi',password:'1234',nick:'qimuwuwukai')
	}
	def hello(){
		logger.info 'hello'+"*"*10
	}
}
