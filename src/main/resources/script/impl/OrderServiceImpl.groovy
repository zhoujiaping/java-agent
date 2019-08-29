import com.alibaba.fastjson.JSONObject
import org.wt.model.Order

def queryAllOrders(){
	[new Order(orderNo:'qwer1234')]
}
def hello(){
	logger.info 'hello'+"*"*10
}
return this
