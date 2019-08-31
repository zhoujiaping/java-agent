package org.sirenia.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class Monkey {
	public Date luoxuanwan(Date date, int num) throws FileNotFoundException, IOException{
		/*if(naruto==null){
			naruto = new Naruto();
			return naruto.luoxuanwan(date, num);
		}*/
		System.out.println("Monkey: "+this.getClass().getName());
		return new Date();
	}
}
