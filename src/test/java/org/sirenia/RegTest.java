package org.sirenia;

import java.util.stream.Stream;

import org.junit.Test;

public class RegTest {
	@Test
	public void test(){
		String text = " com.a,com.b\t  \ncom.c\r\ncom.d\rcom.e ";
		String[] classArr = text.trim().split("[,\\s]+");
		Stream.of(classArr).forEach(System.out::println);
	}
}
