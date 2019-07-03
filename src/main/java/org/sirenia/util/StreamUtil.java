package org.sirenia.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {
	public static byte[] getBytes(InputStream in) throws IOException{
		try(BufferedInputStream bis = new BufferedInputStream(in)){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int len = -1;
			byte[] buf = new byte[1024];
			while((len = bis.read(buf))>-1){
				baos.write(buf, 0, len);
			}
			return baos.toByteArray();
		}
	}
}
