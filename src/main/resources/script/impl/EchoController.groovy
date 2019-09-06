import org.sirenia.agent.JavaAgent

/**
 * some times, we write a script,
 * then we wanna to execute it,
 *
 */
import xxx.xxx.ApplicationContextHolder
app = ApplicationContextHolder.applicationContext
shell = new GroovyShell()
base = JavaAgent.groovyFileDir
//sessionRedis = null
def echo(req,resp,session){
    //do what we wanna do
    //evaluate a script?
    //change session?
    //run a shell command?
	//call a service method?
}

return this