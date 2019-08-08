package org.sirenia.agent.util;

public class StringUtils {
	public static String multi(String s,int times){
		String res = "";
		for(int i=0;i<times;i++){
			res+=s;
		}
		return res;
	}
	public static void main(String[] args) {
		System.out.println(multi("hello",2));
	}
}
