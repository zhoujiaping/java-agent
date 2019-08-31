package org.sirenia.agent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
			System.err.println("warn：方法执行的对象不是声明该方法的类的实例！");
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

}