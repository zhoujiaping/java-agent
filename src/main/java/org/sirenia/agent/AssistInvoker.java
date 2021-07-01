package org.sirenia.agent;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * when javassist compile enhanced class,all class occur in it should be known.
 * so, this class is here, it cannot defined in groovy code.
 * 
 * @author zhoujiaping
 * @date 2019-08-27
 */
public abstract class AssistInvoker {
	public static final String methodSuffix = "_pxy";
	/**
	 *  every class map a AssistInvoker instance.
	 *  so, we can delegate many class to AssistInvoker#invoke,
	 *  but every class will own a AssistInvoker instance.
	 */
	public static final Map<String, AssistInvoker> ivkMap = new ConcurrentHashMap<>();
	public static AssistInvoker defaultIvk = new AssistInvoker() {
		@Override
		public Object invoke1(String selfClassName, Object self, String method, Class<?>[] types, Object[] args)
				throws Throwable {
			Class<?> selfClass = Class.forName(selfClassName);
			Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types);
			if (!proceed.isAccessible()) {
				proceed.setAccessible(true);
			}
			//Method thisMethod = selfClass.getDeclaredMethod(method, types);
			return proceed.invoke(self, args);
		}
	};

	/**
	 * make method name different
	 */
	public static Object invoke(String selfClassName, Object self, String method, Class<?>[] types, Object[] args)
			throws Throwable {
		AssistInvoker ivk = ivkMap.get(selfClassName);
		// System.out.println(selfClass.getName());
		// System.out.println(AssistInvoker.class.getClassLoader());
		if (self != null && !selfClassName.equals(self.getClass().getName())) {
			System.err.println("warn：方法"+method+"执行的对象"+self.getClass().getName()+"不是声明该方法的类"+selfClassName+"的实例！");
		}
		return ivk.invoke1(selfClassName, self, method, types, args);
	}

	public Object invoke1(String selfClassName, Object self, String method, Class<?>[] types, Object[] args)
			throws Throwable {
		Class<?> selfClass = Class.forName(selfClassName);
		Method thisMethod = selfClass.getDeclaredMethod(method, types);
		Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types);
		if (!proceed.isAccessible()) {
			proceed.setAccessible(true);
		}
		return invoke2(self, thisMethod, proceed, args);
	}

	public Object invoke2(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
		return proceed.invoke(self, args);
	}
	public static boolean isExtendsObject(CtMethod method) throws NotFoundException {
        String methodName = method.getName();
        CtClass[] parameterTypes = method.getParameterTypes();
        if(method.getDeclaringClass().getName().equals("java.lang.Object")){
           return true;
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return true;
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return true;
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return true;
        }
        return false;
    }
    public static boolean isExtendsObject(Method method){
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (method.getDeclaringClass() == Object.class) {
            return true;
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return true;
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return true;
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return true;
        }
        return false;
    }
}