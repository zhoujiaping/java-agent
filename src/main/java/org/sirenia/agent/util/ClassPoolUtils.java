package org.sirenia.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassPool;
import javassist.LoaderClassPath;

public abstract class ClassPoolUtils {
	private static final Map<ClassLoader, ClassPool> poolMap = new ConcurrentHashMap<>();
	public static ClassPool linkClassPool(){
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return linkClassPool(cl);
	}
	public static ClassPool linkClassPool(ClassLoader cl){
		if(cl==null){
			return ClassPool.getDefault();
		}
		ClassPool pool = poolMap.get(cl);
		if(pool == null){
			ClassLoader pcl = cl.getParent();
			ClassPool pcp = linkClassPool(pcl);
			pool = new ClassPool(pcp);
			pool.appendClassPath(new LoaderClassPath(cl));
			poolMap.put(cl, pool);
		}
		return pool;
	}
}
