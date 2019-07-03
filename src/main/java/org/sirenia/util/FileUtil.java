package org.sirenia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {
	public static byte[] getBytes(File file) throws IOException{
		return StreamUtil.getBytes(new FileInputStream(file));
	}
}
