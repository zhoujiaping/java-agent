package org.sirenia;

import java.util.List;

public class Dog {
	public String shout(String name){
		return "wang wang wang! "+name;
	}
	public <T> T shoutTwo(T t){
		return t;
	}
	@Deprecated
	public <T> List<T> getList(){
		return null;
	}
}
