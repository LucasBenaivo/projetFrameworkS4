package utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import javax.servlet.http.*;

import annotation.Get;
import utils.Mapping;
import exception.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Scan {
    public static List<Class<?>> getAllClassSelonAnnotation(HttpServlet servlet, String packageName,
            Class<?> annotation) throws Exception {
        List<Class<?>> res = new ArrayList<>();

        String path = servlet.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);

        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> classe = Class.forName(className);
                        if (classe.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            res.add(classe);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return res;
    }

    public static HashMap<String, Mapping> getAllClassSelonAnnotation2(HttpServlet servlet, String packageName,
            Class<?> annotation) throws PackageNotFoundException {
        HashMap<String, Mapping> hMap = new HashMap<>();
        try {
            String path = servlet.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File packageDir = new File(decodedPath);

            if (!packageDir.exists() || !packageDir.isDirectory()) {
                throw new PackageNotFoundException("Package directory does not exist: " + packageName);
            }

            File[] files = packageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            Method[] methods = clazz.getDeclaredMethods();
                            for (Method method : methods) {
                                if (method.isAnnotationPresent(Get.class)) {
                                    Get get = method.getAnnotation(Get.class);
                                    for (String key : hMap.keySet()) {
                                        if (get.value().equals(key))
                                            throw new Exception("Duplicate url : " + get.value());
                                    }
                                    hMap.put(get.value(), new Mapping(method.getName(), clazz.getName()));
                                    System.out.println("Mapping added: " + get.value() + " -> " + clazz.getName() + "."
                                            + method.getName());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new PackageNotFoundException(e.getMessage());
        }
        return hMap;
    }

}
