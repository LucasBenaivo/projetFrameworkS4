package util;

import java.util.Objects;

public class VerbAction {
    String methodName;
    String verb;

    public VerbAction(String methodName, String verb) {
        this.methodName = methodName;
        this.verb = verb;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerbAction that = (VerbAction) o;
        return Objects.equals(methodName, that.methodName) && Objects.equals(verb, that.verb);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, verb);
    }
}

