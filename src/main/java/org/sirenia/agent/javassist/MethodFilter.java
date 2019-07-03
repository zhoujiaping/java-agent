package org.sirenia.agent.javassist;

import javassist.CtMethod;

public interface MethodFilter {
	boolean filter(CtMethod method);
}