package utils;

public class Mapping {
    String methodName;
    String className;

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public Mapping(String methodName, String className) {
        this.methodName = methodName;
        this.className = className;
    }

}
