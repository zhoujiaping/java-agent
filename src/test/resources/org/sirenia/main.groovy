package org.sirenia
import org.sirenia.Person
class abc{
	def main(){
		def p = new Person()
		p.hello()
		println "main ..."
		while(true){
			p.hello()
			Thread.sleep(1000*3)
		}
	}
}