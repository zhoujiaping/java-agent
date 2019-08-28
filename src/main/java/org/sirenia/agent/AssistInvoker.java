package org.sirenia.agent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * when javassist compile enhanced class,all class occur in it should be known.
 * so, this class is here, it cannot defined in groovy code.
 * so is AssistProxy.
 * @author zhoujiaping
 * @date 2019-08-27
 */
public abstract class AssistInvoker {
	public static final String methodSuffix = "_pxy";
	public static final Map<String, AssistInvoker> ivkMap = new ConcurrentHashMap<>();
    public static AssistInvoker defaultIvk = new AssistInvoker(){
        public Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args) throws Throwable{
            //Method thisMethod = selfClass.getDeclaredMethod(method, types);
            Method proceed = selfClass.getDeclaredMethod(method+methodSuffix, types);
            if(!proceed.isAccessible()){
                proceed.setAccessible(true);
            }
            return proceed.invoke(self, args);
        }
    };
    public Object invoke(Class<?> selfClass,Object self,String method,Class<?>[] types,Object[] args) throws Throwable{
        Method thisMethod = selfClass.getDeclaredMethod(method, types);
        Method proceed = selfClass.getDeclaredMethod(method+methodSuffix, types);
        if(!proceed.isAccessible()){
            proceed.setAccessible(true);
        }
        return invoke(self,thisMethod,proceed,args);
    }
    public Object invoke(Object self,Method thisMethod,Method proceed,Object[] args) throws Throwable{
        return proceed.invoke(self, args);
    }

}