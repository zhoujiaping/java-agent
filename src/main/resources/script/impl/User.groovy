import com.alibaba.fastjson.JSONObject
class User{
	def logger = org.slf4j.LoggerFactory.getLogger 'User#groovy'
	def getName(){
		'mingren'
	}
	def hello(){
		logger.info 'hello'+"*"*10
	}
}
