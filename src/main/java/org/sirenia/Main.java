package org.sirenia;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;


public class Main {
	static volatile int count = 0;
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, FileNotFoundException, InterruptedException {
		String path = System.getProperty("CLASSPATH");
		path  = System.getenv("CLASSPATH");
		System.setProperty("CLASSPATH", path+";e:/");
		System.out.println(path);
		System.out.println("执行main方法");
		while(true){
			String res = One.sayHello("main");
			System.out.println(res);
			Thread.sleep(1000*10);
		}
		//System.setProperty("", value);
		//-javaagent:e:/javaagent.jar
	}

}
