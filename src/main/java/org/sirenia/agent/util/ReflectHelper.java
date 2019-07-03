package org.sirenia.agent.util;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * 反射的方法类
 */
public class ReflectHelper {
	/**
	 * 获取obj对象fieldName的Field
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException 
	 */
	public static Field getFieldByFieldName(Object obj, String fieldName) throws NoSuchFieldException {
		Class<?> targetClass = obj.getClass();
		for (Class<?> superClass = targetClass; superClass != Object.class; superClass = superClass
				.getSuperclass()) {
			Field[] fields = superClass.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				Field field = fields[i];
				if(field.getName().equals(fieldName)){
					return field;
				}
			}
		}
		throw new NoSuchFieldException(targetClass.getName()+" has no field named "+fieldName);
	}

	/**
	 * 获取obj对象fieldName的属性值
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object getValueByFieldName(Object obj, String fieldName)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = getFieldByFieldName(obj, fieldName);
		Object value = null;
		if (field != null) {
			if (field.isAccessible()) {
				value = field.get(obj);
			} else {
				field.setAccessible(true);
				value = field.get(obj);
				field.setAccessible(false);
			}
		}
		return value;
	}

	/**
	 * 设置obj对象fieldName的属性值
	 * 
	 * @param obj
	 * @param fieldName
	 * @param value
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static void setValueByFieldName(Object obj, String fieldName, Object value)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = obj.getClass().getDeclaredField(fieldName);
		if (field.isAccessible()) {
			field.set(obj, value);
		} else {
			field.setAccessible(true);
			field.set(obj, value);
			field.setAccessible(false);
		}
	}

	// 处理有多个插件时的情况
	public static Object unwrapProxys(Object target) throws Exception {
		boolean isProxy = false;
		do {
			Class<?> targetClazz = target.getClass();
			isProxy = targetClazz.getName().startsWith("com.sun.proxy.$Proxy");
			if (isProxy) {
				target = ReflectHelper.getValueByFieldName(target, "h");
				target = ReflectHelper.getValueByFieldName(target, "target");
			}
		} while (isProxy);
		return target;
	}
	public static Set<Class<?>> getAllInterfaces(Class<?> type){
		Set<Class<?>> interfaces = new HashSet<>();
		while(type!=null){
			for(Class<?> i : type.getInterfaces()){
				interfaces.add(i);
			}
			type = type.getSuperclass();
		}
		return interfaces;
	}
}
