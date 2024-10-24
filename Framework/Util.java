package util;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.*;
import javax.servlet.http.Part;
import annotation.*;
public class Util {
    public static List<String> getAllClassesSelonAnnotation(String packageToScan,Class<?>annotation) throws Exception{
        List<String> controllerNames = new ArrayList<>();
        try {
            
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
                            controllerNames.add(clazz.getName());
                        }
                    }
                }
            }
           
        } catch (IOException e) {
            throw new Exception("Package introuvable");
        }
       
        return controllerNames;
    }
    public static HashMap<String, Mapping> getAllMethods(List<String> controllers) throws Exception {
        HashMap<String, Mapping> hm = new HashMap<>();
        try {
            for (String c : controllers) {
                Class<?> clazz = Class.forName(c);
                Method[] methods = clazz.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = m.getAnnotation(Url.class);
                        String lien = urlAnnotation.url();
                        
                        if (!hm.containsKey(lien)) {
                            hm.put(lien, new Mapping(clazz.getName()));
                        }
                        
                        boolean isGet = m.isAnnotationPresent(Get.class);
                        boolean isPost = m.isAnnotationPresent(Post.class);
                        if (!isGet && !isPost) {
                            isGet = true;
                        }
    
                        String verb = null;
                        if (isGet) {
                            verb="GET";
                        }
                        else{
                            verb="POST";
                        }
                        hm.get(lien).addVerbAction(m.getName(), verb);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return hm;
    }
    public static Object convertParameterValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == char.class || type == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Invalid character value: " + value);
            }
            return value.charAt(0);
        } 
        return null;
    }
    

   
}
