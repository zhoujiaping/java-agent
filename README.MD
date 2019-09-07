# 1 介绍
## 1.1 用途
* 单元测试mock
* 联调、集成测试mock
* 支持mock静态方法，final方法，私有方法
## 1.2 原理
java5引入了一个api，叫做Instrument，它支持在jvm启动时拦截类的加载，
我们可以在这个时机对加载的字节码进行代理、增强。

该项目就是这么做的，在jvm加载类的时候，通过字节码工具包javassist，
修改了字节码。

方法执行时，并不执行原来的代码，而是动态解析groovy代码，调用groovy方法。

我们可以在相关的groovy代码中配置哪些类的方法执行groovy方法，哪些类的方法执行原来的逻辑。

当然，在groovy方法中，我们可以执行原有的逻辑。

这样就可以在不需用重新启动系统的情况下，修改方法的行为。

具体的实现细节，需要阅读源代码。

要深入理解其中的原理，需要：
* 理解java5的Instrument
* 理解java类加载机制、tomcat类加载机制
* 学习groovy语言
* 学习使用javassist增强字节码
* 了解dubbo消费端执行的逻辑
## 1.3 环境要求
* java8及以上（后续版本会支持java7）
# 2 使用方法
由于其原理是基于jvm级别的代理，所以普通java项目和web项目的使用方法差不多。
这里只介绍web项目部署在tomcat下的使用方法。

比如我们用于https://github.com/zhoujiaping/java-agent-web 这个项目

测试环境和开放环境在配置上有一点点不同，这里只介绍开发环境的使用（原谅我懒......）。
## 2.1 下载,安装
从github上clone两个项目，java-agent（这个实现mock功能的）和java-agent-web（这个用于演示mock的使用），import到idea/eclipse。

对java-agent执行 mvn install（或者mvn package）。

执行后会有两个jar包，较小的那个是没有将依赖一起打包的，较大的一个是将依赖一起打包了的，
我们需要后面那个，比如打包后文件名为java-agent-2.0.1-SNAPSHOT-jar-with-dependencies.jar。

## 2.2 配置jvm启动参数
在java-agent-web这个工程中，有一个文件，src/test/java/org/wt/JettyBootstrap.java。
这个是通过main方法启动web应用的（并不一定要这样启动，这里只是方式之一）。

配置jvm启动参数，例如：
JAVA_OPTS="$JAVA_OPTS -javaagent:【java-agent的target目录】/java-agent-2.0.1-SNAPSHOT-with-dependencies.jar"

在java-agent-web工程中，有一个目录，src/test/resources/mock。该目录存放mock需要的groovy代码。
如果想换一个目录，通过-Dmock.dir=【目录】指定。

编辑agent/MockCaze.groovy文件，配置使用哪个测试用例（将会应用【用例名】-caze目录下的groovy代码）。
比如我们使用default-caze。

编辑default-caze/ClassNameMathcer.groovy，配置规则，哪些类需要增强。

## 2.3 启动
执行JettyBootstrap#main。

## 2.4 编写mock代码
这一步也可以在启动tomcat之前进行。如果你希望mock启动tomcat后立即执行的一些方法，比如spring执行bean的init方法，
你需要将这一步提前。

编辑default-caze/Methods.groovy，定义各个方法的mock逻辑。
如果某些类不需要mock，或者某些方法不需要mock，在Methods.groovy不要配置就行。

调用这个方法的时候，会应用文件中最新的内容,不需要重启应用
（除了agent/ClassFileTransformer.groovy和agent/ClassProxy.groovy，
所有的groovy代码修改后不重启应用也可以生效）。

如果需要mock的是dubbo接口，需要在default-caze/Invocationhandler.groovy中配置。

## 2.5 访问应用
这时候可以分别访问
localhost:8080/login?name=mingren&password=123,
localhost:8080/remote-login?name=mingren&password=123,
localhost:8080/orders
并且修改default-caze目录中的groovy脚本，查看效果。
 
## 2.6 mock开关
如果你想关闭所有mock，可以
* 去调JettyBootstrap#main的jvm启动参数，重启应用。
* 或者其他方式，这里就不详细介绍了。

如果你想关闭对某个类/接口/方法的mock，可以通过修改ClassNameMatcher.groovy,
InvokerInvocationHandler.groovy,Methods.groovy其中的一个或多个实现。

如果你想关闭对某个方法的mock，可以修改方法签名，让它无法匹配。
# 3 使用场景
* 开发自测

在开发过程中，需要对编写的代码进行测试。但是由于系统的复杂性，会遇到一些困难。

比如调用外部接口，我没有有效的测试数据，调用它无法成功。

比如调用外部接口，我需要它返回特定的数据，需要对方配合。

比如准备的数据是明文的，需要先将其加密。

比如某些接口还没有实现，无法进行全流程测试。

比如造的数据不完整，执行时无法关联导致中断。

比如需要测试一些异常场景。

比如有些需要级联mock的场景。

比如......

这些场景，都可以使用这个项目在一定程度上解决

* 系统对接、联调

同上

* sit测试

有时候项目进度滞后，某些团队由于忙别的项目，没有及时开发和联调，
开发的功能没有在开发环境进行联调的情况下，就部署到sit环境。

这时候一般会有很多bug，有些bug会阻塞全流程。

我们不得不等开发人员在本地修复bug，发布基线，部署应用。

这样会使整个工作效率低下，很多时候大家都在等着系统部署。

如果能够对这种bug进行热修复，避免频繁的发布基线-部署应用，那将会提升效率。

开发人员对发现的问题进行热修复，同时将bug记录下来，测试人员可以继续测试，同时开发人员在本地修复bug。

经过一轮测试，下一轮测试先将相应的mock关闭。再进行新一轮的测试。

有时候，有些方法没有打印日志，有些bug很难找到原因。通常的做法是开发人员添加日志，重新发布一个版本。
可以使用java-agent统一打印日志。开发人员先通过应用打印的日志定位问题，如果还是定位不到，就通过统一日志定位问题。
同时将缺少日志也作为一种bug记录下来，后续修复。收集的日志，还可以用于开发人员自测和联调。

# 4 演示
* 普通类普通方法

对User#getName进行mock

* 普通类静态方法

对CryptUtils#enc进行mock

* 普通类final方法

同上

* 有声明接口的spring bean

对UserServiceImpl#login进行mock

* 无声明接口的spring bean

对OrderServiceImpl#queryAllOrders进行mock

* controller方法会话

对UserController#login进行mock

* dubbo服务

对RemoteService#login进行mock

* 调用原有逻辑

对UserController#login进行mock

* mock文件中使用目标项目中的类

* mock文件中使用spring上下文 

* and so on...

# 5 注意事项

* 不要轻易代理容器（比如tomcat）的类

* 不支持mock接口默认方法

* 默认我们的web项目中使用了slf4j。如果没有，则需要修改groovy代码。

* 默认我们的web项目中使用了javassist，并且其api和ClassProxy.groovy中调用的api兼容。
如果不是，需要修改groovy代码。

# 6 最佳实践

* 每个web应用，使用一个mock目录。

* mock范围，不要太大，也不要太小。太大了会影响性能，太小了会导致添加新的mock方法可能需要重启。
建议在开发环境mock大范围，测试环境mock小范围。

* 建议对业务中定义的所有spring bean纳入mock范围。

* 对于已经发现的bug，开发人员通过mock，在本地联调通过后再发布，避免同一个bug出现多次的问题。

* mock方法中打印标记日志，区分正常的方法调用。

* and so on（欢迎参与最佳实践讨论）


# 7 其他
* 局限

不要随便代理类加载器！！！
比如代理WebappClassLoaderBase#loadClass方法会导致stackoverflow。loadClass方法调用MyMethodProxy#invoke，
MyMethodProxy#invoke又会调用Class#forName，Class#forName又会调用WebappClassLoaderBase#loadClass。
所以，如果需要修改classloader，请通过别的方式实现。

* 已经解决的问题
异常：has illegal modifiers
解决办法：确认javassist包的版本和被代理的包依赖的javassist版本兼容。
（已解决，java-agent不自己引入javassist，而是使用web工程提供的javassist）

java.lang.VerifyError: Inconsistent stackmap frames at branch target 48
https://blog.csdn.net/u013476542/article/details/53242050
如果是jdk7：-XX:-UseSplitVerifier
如果是jdk8：-noverify

Caused by: java.lang.ClassFormatError: Arguments can't fit into locals in class file 
不能代理常量类（已解决，代理时已经做了判断）

子类、父类有相同方法的问题
class parentClass{
    retType method(argTypes){
        ...
        super.method(args);//如果parentClass被代理，那么这里的super就会变成this。
        ...
    }
}
class subClass{
    retType method(argTypes){
        ...
        ...
    }
}
如果代理parentClass，并且在代理方法中调用proceed.invoke(m,args),
将会导致死循环或者bug。
解决方法：
使用MethodHandles可以解决，但是比较麻烦，容易引入bug。
所以该项目不支持这种情况做代理。


软件开发和测试的 30 个最佳实践
https://baijiahao.baidu.com/s?id=1593597517685646565&wfr=spider&for=pc
【译】单元测试最佳实践
https://www.jianshu.com/p/6f496aedd080



# 8 经验
类加载器
boot
system
app <- java-agent,groovy-all
webapp <- java-agent、groovy-all、java-agent-web、web-lib

transformer,不能拦截java-agent项目中的类，因为java-agent项目中的类，
是在transformer之前就已经加载的。所以，java-agent项目中使用javassist，
要么在java-agent中直接依赖（这样可能会和java-agent-web中的javassist版本不一致），
要么延迟加载。所以通过写在groovy中，在transformer需要时再加载是比较合适的。

在集成开发环境中，由于集成开发工具的特殊处理，这种错误会被修复（虽然不知道怎么实现的）。
但是部署到外部的tomcat环境中时，这种错误将导致代理失败，并且没有任何错误日志！
所以，java-agent项目中的java类，需要尽可能的简单，尽可能将逻辑放在groovy中实现。

webappclassloader会先尝试自己加载类，加载不到才委托给父类加载器，违背了双亲委托机制。
不同类加载器加载的类，即使类名相同，也是不同的类，变量赋值时无法通过类型检查。
so，java-agent中不要依赖太多的包，避免产生和java-agent-web项目的类加载目录
中有同名的类）。
在java-agent中，需要访问java-agent-web、web-lib中的类的代码，需要设置在webappclassloader
环境中执行。
所以，我们通过groovyclassloader,将我们写的代码，放在webappclassloader环境下执行。

版本日志
0.0.1-SNAPSHOT
实现了一个基本的mock框架。
2.0.1-SNAPSHOT
修改了groovy代码的管理方式，添加了切换测试用例的支持。