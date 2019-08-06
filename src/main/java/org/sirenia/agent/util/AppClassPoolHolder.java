package org.sirenia.agent.util;

import javassist.ClassPool;

public abstract class AppClassPoolHolder {
	private static ClassPool cp;
	public static void set(ClassPool classPool){
		cp = classPool;
	}
	public static ClassPool get(){
		return cp;
	}
	
}
