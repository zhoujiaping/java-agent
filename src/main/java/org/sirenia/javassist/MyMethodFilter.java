package org.sirenia.javassist;

import javassist.CtMethod;

public interface MyMethodFilter {
	boolean filter(CtMethod method);
}