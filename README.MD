使用方法：
0、将java-agent打包为e:/javaagent.jar下（打包时需要指定MANIFEST.MF文件）
1、用户的项目中引入
<dependency>
	<groupId>org.codehaus.groovy</groupId>
	<artifactId>groovy-all</artifactId>
	<version>3.0.0-beta-1</version>
	<type>pom</type>
</dependency>
<dependency>
	<groupId>org.javassist</groupId>
	<artifactId>javassist</artifactId>
	<version>3.20.0-GA</version>
</dependency>
2、将src/test/resources/groovy-script/agent.groovy拷贝到e:/groovy-script/agent.groovy
3、用户app启动时添加jvm参数-javaagent:e:/javaagent.jar=e:/groovy-script/agent.groovy
-javaagent:e:/java-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=e:/groovy-script/agent.groovy
4、接下来，修改agent.groovy,可以动态修改类
5、TODO
	agent.groovy中实现远程加载groovy代码
	agent.groovy加载远程groovy代码时判断最后修改时间
	java-agent项目打包方式，改成maven配置方式（已实现）
6、局限
不要随便代理类加载器！！！
比如代理WebappClassLoaderBase#loadClass方法会导致stackoverflow。loadClass方法调用MyMethodProxy#invoke，
MyMethodProxy#invoke又会调用Class#forName，Class#forName又会调用WebappClassLoaderBase#loadClass。
所以，如果需要修改classloader，请通过别的方式实现。
7、web项目中如果有用到dubbo，可能会报错
javassist编译错误，找不到xxx_proxy方法。
解决办法，web项目中配置
<aop:aspectj-autoproxy proxy-target-class="false" />
或者<aop:aspectj-autoproxy proxy-target-class="true" />
虽然具体细节不太清楚。
MyMethodProxy,里面用的ClassPool，和dubbo用的ClassPool是不一样的。
我们增强的字节码，并不会在dubbo的ClassPool中。
dubbo拿到的对象是spring管理的bean，确实是我们增强了的。
但是它增强时，需要读取字节码文件（class文件）。我们增强了class，但是并没有写到class文件中。
估计是使用aop:aspectj-autoproxy会改变一些实现方式。

8、已知异常及解决办法
异常：has illegal modifiers
解决办法：确认javassist包的版本和被代理的包依赖的javassist版本兼容。

软件开发和测试的 30 个最佳实践
https://baijiahao.baidu.com/s?id=1593597517685646565&wfr=spider&for=pc
【译】单元测试最佳实践
https://www.jianshu.com/p/6f496aedd080

