package org.sirenia.domain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Naruto extends Shuimen{
	public String uuid = UUID.randomUUID().toString();
	//public static Naruto naruto;
	public Date luoxuanwan(Date date, int num) throws FileNotFoundException, IOException{
		/*if(naruto==null){
			naruto = new Naruto();
			return naruto.luoxuanwan(date, num);
		}*/
		System.out.println("Naruto: "+this.getClass().getName());
		//return new Date();
		return super.luoxuanwan(date, num);
	}
	
	private static Date addDate(Date date, int num) throws FileNotFoundException, IOException{
		Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH,num);
        return calendar.getTime();
	}
}
