package util;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import annotation.*;
public class Util {
    public static List<String> getAllClassesSelonAnnotation(String packageToScan,Class<?>annotation) throws Exception{
        List<String> controllerNames = new ArrayList<>();
        try {
            
            //String path = getClass().getClassLoader().getResource(packageToScan.replace('.', '/')).getPath();
            String path = Thread.currentThread().getContextClassLoader().getResource(packageToScan.replace('.', '/')).getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File packageDir = new File(decodedPath);

            File[] files = packageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageToScan + "." + file.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            controllerNames.add(clazz.getSimpleName());
                        }
                    }
                }
            }
           
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        }
        return controllerNames;
    }

   
}
