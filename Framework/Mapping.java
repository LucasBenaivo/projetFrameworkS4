package utils;

public class Mapping {
    String methodName;
    String className;
    String verb;

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getVerb() {
        return verb;
    }

    public Mapping(String methodName, String className) {
        this.methodName = methodName;
        this.className = className;
    }

    public Mapping(String methodName, String className, String verb) {
        this.methodName = methodName;
        this.className = className;
        this.verb = verb;
    }
}
