import com.alibaba.fastjson.JSONObject
import org.wt.model.Order
class OrderServiceImpl{
	def logger = org.slf4j.LoggerFactory.getLogger 'OrderServiceImpl#groovy'
	def queryAllOrders(){
		[new Order(orderNo:'qwer1234')]
	}
	def hello(){
		logger.info 'hello'+"*"*10
	}
}
