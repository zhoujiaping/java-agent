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
			// MethodHandle
			Class<?> selfClass = Class.forName(selfClassName);
			Method proceed = selfClass.getDeclaredMethod(method + methodSuffix, types);
			if (!proceed.isAccessible()) {
				proceed.setAccessible(true);
			}
			//Method thisMethod = selfClass.getDeclaredMethod(method, types);
			return proceed(self,proceed,args);
			//return proceed.invoke(self, args);
		}
	};

	/*
	 * 解决 子类、父类有相同方法时，使用子类对象，调用父类方法，实际上调用的还是子类方法的问题。
	 * https://www.jianshu.com/p/63691220f81f http://www.it1352.com/956952.html
	 */
	public static Object proceed(Object self, Method method, Object args) throws Throwable {
		Class<?> decCls = method.getDeclaringClass();
		MethodType mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
		Class<Lookup> lookupClass = MethodHandles.Lookup.class;
		Constructor<Lookup> constructor = lookupClass.getDeclaredConstructor(Class.class, int.class);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		int mod = method.getModifiers();
		Lookup lookup = constructor.newInstance(decCls,
				Lookup.PRIVATE | Lookup.PACKAGE | Lookup.PROTECTED | Lookup.PUBLIC);
		MethodHandle mh;
		if (Modifier.isStatic(mod)) {
			mh = lookup.findStatic(decCls, method.getName(), mt);
		} else {
			mh = lookup.findSpecial(decCls, method.getName(), mt, decCls);
		}
		return mh.bindTo(self).invokeWithArguments(args);
	}

	/**
	 * make method name different
	 */
	public static Object invoke(String selfClassName, Object self, String method, Class<?>[] types, Object[] args)
			throws Throwable {
		AssistInvoker ivk = ivkMap.get(selfClassName);
		// System.out.println(selfClass.getName());
		// System.out.println(AssistInvoker.class.getClassLoader());
		if (self != null && !selfClassName.equals(self.getClass().getName())) {
			System.out.println("");
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
		return proceed(self,proceed,args);
		//return proceed.invoke(self, args);
	}

}