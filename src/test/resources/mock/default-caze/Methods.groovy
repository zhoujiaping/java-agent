import org.aspectj.lang.ProceedingJoinPoint
import com.alibaba.fastjson.JSONObject
import org.wt.model.Order
import org.wt.model.User

//no def,and no type,the variable will be the binding variable.
logger = org.slf4j.LoggerFactory.getLogger("MethodsLogger")
def proxys = new Expando()
/**
 * 这里使用实现类（对于dubbo远程服务，这里是接口名）的简单类名，如果有简单类名相同的，需要修改ClassProxy或者InvokerInvocationHandler。
 */
proxys."CryptUtils" = new Object(){
    /**
     * mock方法，方法签名可以拷贝类的原有方法。去掉private、static修饰符。
     * @param data
     * @return
     */
    def enc(data){
        //groovy支持多行字符串，我们可以从测试环境拷贝日志里面的json串，贴过来反序列化。
/*def json = """
{"name":"mingren","nick":"qimuwuwukai","password":"1234"}
"""
	return JSONObject.parseObject(json, org.wt.model.User)*/
        data
    }
    /**
     * java版的mock方法
     * @param data
     * @return
     */
    public String dec(String data){
        return data;
    }
}

proxys."OrderServiceImpl" = new Object(){
    def queryAllOrders(){
        /**
         * groovy使我们写测试代码更简洁。这里创建了一个列表，
         * 创建了一个Order对象，设置了它的字段值，将Order对象添加到了列表，然后将其返回。
         */
        [new Order(orderNo:'qwer1234')]
    }
}

proxys."UserController" = new Object(){
    def login(name,pwd,session){
        //可以使用slf4j打印日志哦
        logger.info "###################################login"
        /*
        拦截了controller的方法，获取session对象，往session中设置值。
        我们可以在web项目中提供一个空实现的controller,拦截它，通过这种方式随便修改会话，准备测试环境。
        甚至在mock方法中调用linux shell命令，使用groovy的sql包访问数据库，使用http发送请求（实现远程mock），
        拷贝文件，修改配置，and so on...
         */
        session.setAttribute('whosyourdaddy','开启无敌模式')
        def json = new JSONObject()
        json.put("qwer","QWER1")
        json
    }
}

proxys."UserServiceImpl" = new Object(){
    def login(name,pwd){
        logger.info "$name $pwd"
        new User(name:'baizhan',password:'987',nick:'')
    }
}

proxys.RemoteUserService = new Object(){
    def login(name,pwd){
        logger.info "$name $pwd 456xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        //new User(name:'john',password:'987',nick:'')
        /**
         * 获取spring上下文，通过spring上下文，获取其管理的bean，调用bean的方法。
         * 这种手段在测试中非常有用。某个方法有错误，我们可以从测试环境拷贝入参，然后在页面发起一个get请求，
         * 在这里获取对应的bean，将入参传入，可以方便的调用service的方法。
         */
        def userService = org.wt.context.AppContextHolder.appContext.getBean('userService')
        userService.login(name,pwd)
    }
}
/**
 * 你可以在项目中定义一个spring bean的切面，切面方法中调用joinoint.proceed(),
 * 这样不会影响项目的业务逻辑，
 * 然后在这里拦截，接下来就可以对spring bean进行mock。
 * */
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

return proxys