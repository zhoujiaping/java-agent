

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;


public class Main {
	static volatile int count = 0;
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, FileNotFoundException {
		One.sayHello("main");
		System.out.println("执行main方法");
		//-javaagent:e:/javaagent.jar
	}

}
