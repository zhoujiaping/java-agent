def exp = new Expando() {
    def "hello-world"(int code) {

    }
    def "hello-world"(int code,String name) {
        println "$code $name"
    }
}
exp.metaClass."hello-abc"<<{
    String name->
        println name
}
exp.metaClass."hello-abc"<<{
    String firstname,String lastname->
        println firstname+lastname
}
exp."hello-abc" "zhou"
exp."hello-abc" "zhou","jiaping"

def r1 = exp.metaClass.respondsTo(exp,"hello-abc",*["zhou"])
def r2 = exp.metaClass.respondsTo(exp,"hello-abc",*["z","jp"])
def r3 = exp.metaClass.respondsTo(exp,"hello-world",*[1])
def r4 = exp.metaClass.respondsTo(exp,"hello-world",*[1,"z"])
println r1
println r2
println r3
println r4

exp."hello-world" 1,"hello world"
