import com.alibaba.fastjson.JSONObject
class HelloServiceImpl{
	def logger = org.slf4j.LoggerFactory.getLogger 'HelloServiceImpl#groovy'
	def hello(name){
		def json = new JSONObject()
		json.put 'a','A'
		logger.info json.toString()
		println 'yyyyyyyyyyyyyyyyy'
		logger.info 'setenv.............'
		'xyxyxz'
	}
	def helloDog(){
		logger.info 'helloDog'+"*"*10
		new org.wt.Dog()
	}
}
