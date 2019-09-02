package org.sirenia;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.Test;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class GCLTest {

	@Test
	public void testRsc() {
		
	}

	@Test
	public void test() throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException {
		GroovyClassLoader gcl = new GroovyClassLoader();
		URL url = this.getClass().getResource("./main.groovy");
		String filename = url.getFile();
		File file = new File(filename);
		Class c = gcl.parseClass(file);
		System.out.println(c.getName());
		GroovyObject go = (GroovyObject) c.newInstance();
		go.invokeMethod("main", new Object[0]);
	}
}
