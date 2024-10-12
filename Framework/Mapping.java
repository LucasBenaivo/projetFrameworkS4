package util;

import java.util.HashSet;
import java.util.Set;

public class Mapping {
    String className;
    Set<VerbAction> verbactions;

    public Mapping(String className) {
        this.className = className;
        this.verbactions = new HashSet<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<VerbAction> getVerbactions() {
        return verbactions;
    }

    public void addVerbAction(String methodName, String verb) throws Exception {
        // Vérifiez si le verbe est déjà utilisé
        for (VerbAction action : verbactions) {
            if (action.getVerb().equals(verb)) {
                throw new Exception("HTTP verb " + verb + " already used by method: " + action.getMethodName());
            }
        }       
        verbactions.add(new VerbAction(methodName, verb));
    }
}
