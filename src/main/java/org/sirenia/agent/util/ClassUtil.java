package org.sirenia.agent.util;

import java.util.HashMap;
import java.util.Map;

public class ClassUtil {
	private static final Map<String, Class<?>> typeMap = new HashMap<>();
	private static final Map<String, String> arrSuffix = new HashMap<>();

	static {
		typeMap.put("long", long.class);
		typeMap.put("int", int.class);
		typeMap.put("short", short.class);
		typeMap.put("byte", byte.class);
		typeMap.put("boolean", boolean.class);
		typeMap.put("float", float.class);
		typeMap.put("double", double.class);
		typeMap.put("char", char.class);

		arrSuffix.put("long", "J");
		arrSuffix.put("int", "I");
		arrSuffix.put("short", "S");
		arrSuffix.put("byte", "B");
		arrSuffix.put("boolean", "Z");
		arrSuffix.put("float", "F");
		arrSuffix.put("double", "D");
		arrSuffix.put("char", "C");
	}

	/**
	 * javassist的CtClass，在处理数组类型时和jdk有些不一样。 jdk里面数组类型 Class[] =>
	 * [Ljava.lang.Class int[] => [I int[][] => [[I Class[][] =>
	 * [[Ljava.lang.Class int => int
	 */
	public static Class<?> forName(String name, boolean init, ClassLoader cl) throws ClassNotFoundException {
		int i = name.indexOf('[');
		if (i > 0) {
			String part0 = name.substring(0, i);
			String part1 = name.substring(i);
			String suffix = arrSuffix.get(part0);
			if (suffix == null) {
				suffix = "L" + part0 + ";";
			}
			String internalName = StringUtils.multi("[", part1.length() / 2) + suffix;
			return Class.forName(internalName);
		}
		Class<?> c = typeMap.get(name);
		if (c != null) {
			return c;
		}
		return Class.forName(name, init, cl);
	}

	public static Class<?> forName(String name) throws ClassNotFoundException {
		int i = name.indexOf('[');
		if (i > 0) {
			String part0 = name.substring(0, i);
			String part1 = name.substring(i);
			String suffix = arrSuffix.get(part0);
			if (suffix == null) {
				suffix = "L" + part0 + ";";
			}
			String internalName = StringUtils.multi("[", part1.length() / 2) + suffix;
			return Class.forName(internalName);
		}
		Class<?> c = typeMap.get(name);
		if (c != null) {
			return c;
		}
		return Class.forName(name);
	}
}
