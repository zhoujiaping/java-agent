package org.sirenia.agent.util;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class PackageUtil {

	/**
	 * 在当前项目中寻找指定包下的所有类
	 * 
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursion
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws ClassNotFoundException 
	 */
	public static Set<String> getClassSet(String packageName, boolean recursive) throws IOException, ClassNotFoundException, URISyntaxException {
		Set<String> classList = new HashSet<>();
		// 获取当前线程的类装载器中相应包名对应的资源
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace(".", "/");
		Enumeration<URL> iterator = cl.getResources(path);
		while (iterator.hasMoreElements()) {
			URL url = iterator.nextElement();
			String protocol = url.getProtocol();
			Set<String> childClassList = Collections.emptySet();
			switch (protocol) {
			case "file":
				childClassList = getClassFromFileUrl(url, packageName, recursive);
				break;
			case "jar":
				childClassList = getClassFromJarUrl(url, packageName, recursive);
				break;
			default:
				// 在某些WEB服务器中运行WAR包时，它不会像TOMCAT一样将WAR包解压为目录的，如JBOSS7，它是使用了一种叫VFS的协议
				throw new RuntimeException("unknown protocol " + protocol);
			}
			classList.addAll(childClassList);
		}
		return classList;
	}

	/**
	 * 在给定的文件或文件夹中寻找指定包下的所有类
	 * 
	 * @param filePath
	 *            包的路径
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	private static Set<String> getClassFromFileName(String filePath, String packageName, boolean recursive) throws ClassNotFoundException, IOException {
		Path path = Paths.get(filePath);
		return getClassSetFromPath(path,packageName, packageName, recursive);
	}

	/**
	 * 在给定的文件或文件夹中寻找指定包下的所有类
	 * 
	 * @param url
	 *            包的统一资源定位符
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws URISyntaxException
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	private static Set<String> getClassFromFileUrl(URL url, String packageName, boolean recursive)
			throws URISyntaxException, ClassNotFoundException, IOException {
		Path path = Paths.get(url.toURI());
		return getClassSetFromPath(path,packageName, packageName, recursive);
	}

	/**
	 * 在给定的文件或文件夹中寻找指定包下的所有类
	 * 
	 * @param path
	 *            包的路径
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static Set<String> getClassSetFromPath(Path path,String currentPackage, String packageName, boolean recursive)
			throws IOException, ClassNotFoundException {
		Set<String> classSet = new HashSet<>();
		if (Files.isDirectory(path)) {
			if (!recursive) {
				return Collections.emptySet();
			}
			// 获取目录下的所有文件
			try (Stream<Path> stream = Files.list(path);) {
				Iterator<Path> iterator = stream.iterator();
				while (iterator.hasNext()) {
					Path currentPath = iterator.next();
					String nextPackage = currentPackage+"."+currentPath.toFile().getName();
					classSet.addAll(getClassSetFromPath(currentPath,nextPackage, packageName, recursive));
				}
			}
		} else {
			if(currentPackage.endsWith(".class")){
				String className = currentPackage.replace(".class", "");
				classSet.add(className);
			}
		}
		return classSet;
	}

	/**
	 * 在给定的jar包中寻找指定包下的所有类
	 * 
	 * @param filePath
	 *            包的路径
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	private static Set<String> getClassFromJarFileName(String filePath, String packageName, boolean recursive)
			throws IOException, ClassNotFoundException {
		JarFile jar = new JarFile(filePath);
		return getClassFromJarFile(jar, packageName, recursive);
	}

	/**
	 * 在给定的jar包中寻找指定包下的所有类
	 * 
	 * @param url
	 *            jar包的统一资源定位符
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	private static Set<String> getClassFromJarUrl(URL url, String packageName, boolean recursive) throws IOException, ClassNotFoundException {
		JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
		return getClassFromJarFile(jar, packageName, recursive);
	}

	/**
	 * 在给定的jar包中寻找指定包下的所有类
	 * 
	 * @param jar
	 *            jar对象
	 * @param packageName
	 *            用'.'分隔的包名
	 * @param recursive
	 *            是否递归搜索
	 * @return 该包名下的所有类
	 * @throws ClassNotFoundException 
	 */
	private static Set<String> getClassFromJarFile(JarFile jar, String packageName, boolean recursive) throws ClassNotFoundException {
		Set<String> classList = new HashSet<>();
		// 该迭代器会递归得到该jar底下所有的目录和文件
		Enumeration<JarEntry> iterator = jar.entries();
		while (iterator.hasMoreElements()) {
			// 这里拿到的一般的"aa/bb/.../cc.class"格式的Entry或 "包路径"
			JarEntry jarEntry = iterator.nextElement();
			if (!jarEntry.isDirectory()) {
				String name = jarEntry.getName();
				// 对于拿到的文件,要去除末尾的.class
				int lastDotClassIndex = name.lastIndexOf(".class");
				if (lastDotClassIndex != -1) {
					int lastSlashIndex = name.lastIndexOf("/");
					name = name.replace("/", ".");
					if (name.startsWith(packageName)) {
						if (recursive || packageName.length() == lastSlashIndex) {
							String className = name.substring(0, lastDotClassIndex);
							classList.add(className);
						}
					}
				}
			}
		}
		return classList;
	}
}