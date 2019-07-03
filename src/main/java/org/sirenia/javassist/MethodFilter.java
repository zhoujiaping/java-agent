package org.sirenia.javassist;

import javassist.CtMethod;

public interface MethodFilter {
	boolean filter(CtMethod method);
}