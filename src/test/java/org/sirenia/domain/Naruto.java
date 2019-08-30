package org.sirenia.domain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

public class Naruto {
	public String uuid = UUID.randomUUID().toString();
	//public static Naruto naruto;
	public Date luoxuanwan(Date date, int num) throws FileNotFoundException, IOException{
		/*if(naruto==null){
			naruto = new Naruto();
			return naruto.luoxuanwan(date, num);
		}*/
		return new Date();
	}
	
	private static Date addDate(Date date, int num) throws FileNotFoundException, IOException{
		/*if(naruto==null){
			naruto = new Naruto();
			return naruto.luoxuanwan(date, num);
		}*/
		return new Date();
	}
}
