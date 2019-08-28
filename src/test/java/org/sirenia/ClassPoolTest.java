package org.sirenia;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.junit.Test;

public class ClassPoolTest {
    @Test
    public void test(){
        ClassPool cp = ClassPool.getDefault();
        cp.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
    }
}
