package mock.agent

import javassist.CtField
import java.lang.reflect.Modifier
import java.security.ProtectionDomain
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.LoaderClassPath
import javassist.bytecode.AccessFlag
import org.sirenia.agent.AssistInvoker
import org.sirenia.agent.LastModCache
import org.sirenia.agent.LastModCache.OnExpire
import java.lang.reflect.Method
import org.sirenia.agent.JavaAgent

class ClassProxy {

    def logger = org.slf4j.LoggerFactory.getLogger('ClassProxyLogger')
    def timestamp = System.currentTimeMillis()
    def gcl
    def shell
    def methodSuffix = '_pxy'
    def ivkName = AssistInvoker.class.name
    def mockCazeFile = new File(JavaAgent.mockDir, "agent/MockCaze.groovy")
    def proxysCache
    def cazeCache = new LastModCache()
    def onMockCazeExpire = {
        proxysCache = new LastModCache()
        def newMockCaze = gcl.parseClass(mockCazeFile).newInstance()
        logger.info "change mock caze => from ${it?.obj?.caze} to ${newMockCaze.caze}"
        newMockCaze
    } as OnExpire
    def thirtyProxyFac = {
        interfaceName, mockCazeName, methods ->
            def simpleName = interfaceName.split(/\./)[-1]
            def file = new File(JavaAgent.mockDir, "${mockCazeName}/${simpleName}.groovy")
            def onExpire = {
                gcl.parseClass(file).newInstance()
            } as OnExpire
            def pxy = proxysCache.get(file.getAbsolutePath(), onExpire)
            pxy.init methods
            pxy
    }
    def thirtyProxyFacMap = {
        def map = [:]
        ['org.apache.ibatis.binding.MapperProxy',
         'com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler',
         'org.springframework.remoting.caucho.HessianClientInterceptor'].each {
            map[it] = thirtyProxyFac
        }
        map
    }.call()


    def pool

    def init(GroovyClassLoader gcl) {
        pool = ClassPool.getDefault()
        pool.appendClassPath(new LoaderClassPath(gcl.parent))
        this.gcl = gcl
        shell = new GroovyShell(gcl.parent)
    }

    def proxy(String className, ProtectionDomain domain, byte[] bytes) {
        def ctClass = proxy0(className)
        if (ctClass == null) {
            return null
        }
        def ivk = new AssistInvoker() {
            /*code will be trans to bytecode, it will load by webAppClassloader,
            so, do not use code which webAppClassloader cannot find!
            */

            def invoke1(String selfClassName, Object self, String method, Class[] types, Object[] args) {
                try {
                    return doInvoke(selfClassName, self, method, types, args)
                } catch (e) {
                    logger.error("$selfClassName,$self,$method,$types,$args")
                    throw e
                }
            }

            private Object doInvoke(String selfClassName, Object self, String method, Class[] types, Object[] args) {
                Class selfClass = Class.forName(selfClassName)
                Method thisMethod = selfClass.getDeclaredMethod(method, types)
                Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types)
                if (!proceed.isAccessible()) {
                    proceed.setAccessible(true)
                }
                def mockCaze = cazeCache.get(mockCazeFile.getAbsolutePath(), onMockCazeExpire)
                def selfClassSimpleName = selfClassName.split(/\./)[-1]

                def methodsFile = new File(JavaAgent.mockDir, "${mockCaze.caze}/Methods.groovy")
                def onMethodsExpire = {
                    def newMethods = shell.evaluate(methodsFile)
                    if (!newMethods) {
                        throw new RuntimeException("evaluate Methods.groovy returns null, forgot return a value?")
                    }
                    newMethods
                } as OnExpire
                def methods = proxysCache.get(methodsFile.getAbsolutePath(), onMethodsExpire)
                def proxy
                if (thirtyProxyFacMap[selfClassName]) {
                    proxy = thirtyProxyFacMap[selfClassName].call(selfClassName, mockCaze.caze, methods)
                } else {
                    //如果mock对象集合中找不到对应的mock对象，就调用方法原有逻辑
                    def proxys = methods.proxys
                    if (proxys[selfClassName]) {
                        proxy = proxys[selfClassName]
                    } else if (proxys[selfClassSimpleName]) {
                        proxy = proxys[selfClassSimpleName]
                    } else {
                        return proceed.invoke(self, args)
                    }
                }
                if (!proxy.hasProperty('overwrite-missing-method')) {
                    proxy.metaClass.methodMissing = {
                        methodName, arguments ->
                            IvkJoinPointManager.currentJoinPoint().proceed()
                    }
                    proxy.metaClass['overwrite-missing-method'] = true
                }
                IvkJoinPointManager.withInvokerStackManage(new IvkJoinPoint([
                        self      : self,
                        thisMethod: thisMethod,
                        proceed   : proceed,
                        args      : args
                ])) {
                    proxy."${thisMethod.name}"(*args)
                }
            }
        }
        AssistInvoker.ivkMap.put(ctClass.name, ivk)
        //ctClass.toClass()
        //ctClass.toBytecode()
        ctClass
    }

    CtClass proxy0(String className) {
        CtClass ct = pool.getCtClass(className)
        // 解冻
        if (ct.isFrozen()) {
            ct.defrost()
        }
        if (ct.isInterface()) {
            return null
        }
        if (ct.isEnum()) {
            return null
        }
        /*
        添加一个标记字段，如果已经被我们代理过，就不再代理。
        */
        try {
            ct.getDeclaredField("_pxy")
            return null
        } catch (javassist.NotFoundException e) {
            logger.info("transform => $className")
            CtField ctf = new CtField(CtClass.intType, '_pxy', ct)
            ct.addField(ctf)
        }
        CtMethod[] methods = ct.getDeclaredMethods()// ct.getMethods()
        for (int i = 0; i < methods.length; i++) {
            CtMethod method = methods[i]
            int mod = method.modifiers
            if (Modifier.isAbstract(mod)) {
                continue
            }
            String methodName = method.name
            if (methodName.startsWith('lambda$')) {
                continue
            }
            /*
            不要代理继承自Object的任何方法，代理这些方法容易出问题,比如代理toString方法容易进入死循环。
             */
            if (AssistInvoker.isExtendsObject(method)) {
                continue
            }
            CtMethod copyMethod = CtNewMethod.copy(method, methodName + methodSuffix, ct, null)
            /*设置方法为私有的
                错误方式：copyMethod.setModifiers(Modifier.PRIVATE)
                这样会修改去掉其他修饰符
            */
            //mod = mod &(~(Modifier.PUBLIC|Modifier.PROTECTED|Modifier.PRIVATE));
            int accMod = AccessFlag.setPrivate(mod)
            copyMethod.modifiers = mod & accMod

            ct.addMethod(copyMethod)
            String body = ""
            if (Modifier.isStatic(mod)) {
                body = '{return ($r)$proceed("' + className + '",null,"' + methodName + '",$sig,$args);}'
            } else {
                //body = '{return ($r)$proceed($class,$0,"' + methodName + '",$sig,$args);}'
                body = '{return ($r)$proceed("' + className + '",$0,"' + methodName + '",$sig,$args);}'
            }
            method.setBody(body, ivkName, "invoke")
        }
        // Class<?> c = ct.toClass()
        // ct.writeFile("d:/")
        // ct.writeFile(CtClass.class.getClassLoader().getResource(".").getFile())
        return ct
    }
}
