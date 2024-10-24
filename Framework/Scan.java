package utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import javax.servlet.http.*;

import annotation.*;
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

    // public static HashMap<String, Mapping>
    // getAllClassSelonAnnotation2(HttpServlet servlet, String packageName,
    // Class<?> annotation) throws PackageNotFoundException {
    // HashMap<String, Mapping> hMap = new HashMap<>();
    // try {
    // String path =
    // servlet.getClass().getClassLoader().getResource(packageName.replace('.',
    // '/')).getPath();
    // String decodedPath = URLDecoder.decode(path, "UTF-8");
    // File packageDir = new File(decodedPath);

    // if (!packageDir.exists() || !packageDir.isDirectory()) {
    // throw new PackageNotFoundException("Package directory does not exist: " +
    // packageName);
    // }

    // File[] files = packageDir.listFiles();
    // if (files != null) {
    // for (File file : files) {
    // if (file.isFile() && file.getName().endsWith(".class")) {
    // String className = packageName + "." + file.getName().replace(".class", "");
    // Class<?> clazz = Class.forName(className);
    // if
    // (clazz.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class)))
    // {
    // Method[] methods = clazz.getDeclaredMethods();
    // for (Method method : methods) {
    // if (method.isAnnotationPresent(Get.class)) {
    // Get get = method.getAnnotation(Get.class);
    // for (String key : hMap.keySet()) {
    // if (get.value().equals(key))
    // throw new Exception("Duplicate url : " + get.value());
    // }
    // hMap.put(get.value(), new Mapping(method.getName(), clazz.getName()));
    // System.out.println("Mapping added: " + get.value() + " -> " + clazz.getName()
    // + "."
    // + method.getName());
    // }
    // }
    // }
    // }
    // }
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new PackageNotFoundException(e.getMessage());
    // }
    // return hMap;
    // }

    public static HashMap<String, Mapping> getAllClassSelonAnnotation3(HttpServlet servlet, String packageName,
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

                        // Vérifie si la classe a l'annotation spécifiée
                        if (clazz.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            Method[] methods = clazz.getDeclaredMethods();

                            for (Method method : methods) {
                                String url = null;

                                // Vérification de l'annotation @Url
                                if (method.isAnnotationPresent(Url.class)) {
                                    Url urlAnnotation = method.getAnnotation(Url.class);
                                    url = urlAnnotation.value();
                                    System.out.println(url);
                                }

                                // Vérifie si la méthode est annotée avec @Get ou @Post
                                boolean isGet = method.isAnnotationPresent(Get.class);
                                boolean isPost = method.isAnnotationPresent(Post.class);

                                // Si aucune annotation de verbe n'est présente, on suppose que c'est un GET par
                                // défaut
                                if (!isGet && !isPost) {
                                    isGet = true; // Définir GET par défaut
                                }

                                // Si aucune annotation d'URL ni de verbe n'est présente, passer à la méthode
                                // suivante
                                if (url == null && !isGet && !isPost) {
                                    continue;
                                }

                                // Vérification des doublons d'URL
                                if (url != null && hMap.containsKey(url)) {
                                    throw new Exception("Duplicate URL: " + url);
                                }

                                // Ajout de la méthode à la map avec le verbe approprié
                                if (isGet) {
                                    hMap.put(url, new Mapping(method.getName(), clazz.getName(), "GET"));
                                    System.out.println("Mapping added (GET): " + url + " -> " + clazz.getName() + "."
                                            + method.getName());
                                } else if (isPost) {
                                    hMap.put(url, new Mapping(method.getName(), clazz.getName(), "POST"));
                                    System.out.println("Mapping added (POST): " + url + " -> " + clazz.getName() + "."
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
