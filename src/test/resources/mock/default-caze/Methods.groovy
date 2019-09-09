import groovy.transform.Field
import org.aspectj.lang.ProceedingJoinPoint
import com.alibaba.fastjson.JSONObject
import org.wt.model.Order
import org.wt.model.User


//去binding，使用binding容易导致一系列问题。
@Field logger = org.slf4j.LoggerFactory.getLogger("MethodsLogger")
@Field proxys = [:]
//def proxys = new Expando()
@Field timestamp = System.currentTimeMillis()

proxys."CryptUtils" = new Object(){
    def enc(data){
/*def json = """
{"name":"mingren","nick":"qimuwuwukai","password":"1234"}
"""
	return JSONObject.parseObject(json, org.wt.model.User)*/
        data
    }
}

proxys."OrderServiceImpl" = new Object(){
    def queryAllOrders(){
        [new Order(orderNo:'qwer1234')]
    }
}

proxys."UserController" = new Object(){
    def login(name,pwd,session){
        logger.info "###################################login"
        session.setAttribute('whosyourdaddy','开启无敌模式')
        def json = new JSONObject()
        json.put("qwer","QWER1")
        json
    }
}

proxys."UserServiceImpl" = new Object(){
    def login(name,pwd){
        logger.info "$name $pwd"
        new User(name:'baizhan',password:'987',nick:'monkey')
    }
}

proxys.RemoteUserService = new Object(){
    def login(name,pwd){
        logger.info "$name $pwd 4501xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        //new User(name:'john',password:'987',nick:'')
        def userService = org.wt.context.AppContextHolder.appContext.getBean('userService')
        userService.login(name,pwd)
    }
}

proxys.AspectTest = new Object(){
    /**
     * if you just want to proxy spring beans,
     * you can config a aspect,then proxy the aspect.
     * some times AssistProxy does not works
     * (because there is limitation for javassist),
     * this is the way to resolve the problem.
     * @param joinPoint
     * @return
     */
    def around(ProceedingJoinPoint joinPoint){
        logger.info("###############"+joinPoint)
        return joinPoint.proceed()
    }
}

return this