package org.sirenia.agent.javassist;

import java.lang.reflect.Method;

public interface MethodInvoker {
	Object invoke(Object self, Method thisMethod,Method proceed, Object[] args) throws Throwable;
}
