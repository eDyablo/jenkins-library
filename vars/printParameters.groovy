#!groovy
def call()
{
    def paramsString = "";
    params.each{ k, v -> 
        paramsString = paramsString + "\nParameter: ${k} => ${v}" 
    }
    println paramsString
}