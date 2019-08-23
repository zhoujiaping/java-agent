/*
def hello(name){
	//def json = new JSONObject()
	//json.put 'a','A'
	logger.info '#############1234#############'
	//logger.info json.toString()
	new org.wt.Dog().wang('x.x.x.x')
	'proxy '+name
}
this*/
import com.alibaba.fastjson.JSONObject
import org.slf4j.LoggerFactory
class HelloServiceImpl{
	def logger = LoggerFactory.getLogger 'HelloServiceImpl#groovy' 
	/*def hello(name){
		def json = new JSONObject()
		json.put 'a','A'
		logger.info json.toString()
		println 'yyyyyyyyyyyyyyyyy'
		logger.info 'setenv.............'
		'xyxyxz'
	}*/
	def helloDog(){
		logger.info 'helloDog'+"*"*10
		new org.wt.Dog()
	}
}
