package org.sirenia.util;

import java.util.HashMap;
import java.util.Map;

public class ClassUtil {
	private static final Map<String,Class<?>> typeMap = new HashMap<>();
	static{
		typeMap.put("long", long.class);
		typeMap.put("int", int.class);
		typeMap.put("short", short.class);
		typeMap.put("byte", byte.class);
		typeMap.put("boolean", boolean.class);
		typeMap.put("float", float.class);
		typeMap.put("double", double.class);
		typeMap.put("char", char.class);
	}
	public static Class<?> forName(String name) throws ClassNotFoundException{
		Class<?> c = typeMap.get(name);
		if(c!=null){
			return c;
		}
		return Class.forName(name);
	}
}
