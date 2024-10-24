package controller;

import util.Util;
import util.Mapping;
import util.MySession;
import util.VerbAction;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import annotation.*;
import model.*;
import com.google.gson.Gson;
import model.Fichier;
import javax.servlet.http.Part;
import javax.servlet.annotation.MultipartConfig;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 10,  // 10 MB
    maxFileSize = 1024 * 1024 * 50,        // 50 MB
    maxRequestSize = 1024 * 1024 * 100     // 100 MB
)
public class FrontController extends HttpServlet {
    private List<String> controllers;
    private HashMap<String, Mapping> map;

    @Override
    public void init() throws ServletException {
        try {
            String packageName = this.getInitParameter("package_name");
            controllers = Util.getAllClassesSelonAnnotation(packageName, ControllerAnnotation.class);
            map = Util.getAllMethods(controllers);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        String url = req.getRequestURI();
        boolean urlExists = false;

        if (map.containsKey(url)) {
            urlExists = true;
            Mapping mapping = map.get(url);
            String requestMethod = req.getMethod();

            try {
                Method m = null;
                for (VerbAction action : mapping.getVerbactions()) {
                    if (action.getVerb().equalsIgnoreCase(requestMethod)) {
                        Class<?> c = Class.forName(mapping.getClassName());
                        Method[] methods = c.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.getName().equals(action.getMethodName())) {
                                m = method;
                                break;
                            }
                        }
                        break;
                    }
                }

                if (m == null) {
                    throw new ServletException("Method not found in class " + mapping.getClassName());
                }

                Parameter[] params = m.getParameters();
                Object instance = Class.forName(mapping.getClassName()).getDeclaredConstructor().newInstance();
                Field[] attributs = instance.getClass().getDeclaredFields();
                for (Field field : attributs) {
                    if (field.getType().equals(MySession.class)) {
                        HttpSession httpSession = req.getSession(false);
                        if (httpSession == null) {
                            httpSession = req.getSession(true);
                        }
                        MySession session = new MySession(httpSession);
                        field.setAccessible(true);
                        field.set(instance, session);
                    }
                }

                Object result;

                if (params.length < 1) {
                    result = m.invoke(instance);
                } else {
                    Object[] parameterValues = new Object[params.length];

                    for (int i = 0; i < params.length; i++) {
                        Parameter param = params[i];
                        // Gérer les fichiers
                        if (param.getType().equals(MySession.class)) {
                            HttpSession httpSession = req.getSession(false);
                            if (httpSession == null) {
                                httpSession = req.getSession(true);
                            }
                            MySession session = new MySession(httpSession);
                            parameterValues[i] = session;
                        } else if (param.isAnnotationPresent(Param.class)) {
                            if (req.getContentType() != null && req.getContentType().toLowerCase().startsWith("multipart/")) {
                                Part filePart = req.getPart("file");
                                if (filePart != null) {
                                    Fichier fichier = new Fichier(filePart);
                                    parameterValues[i] = fichier;
                                } else {
                                    throw new ServletException("File part is missing.");
                                }
                            }else{
                                Param paramAnnotation = param.getAnnotation(Param.class);
                                String paramName = paramAnnotation.name();
                                String paramValue = req.getParameter(paramName);
                                parameterValues[i] = Util.convertParameterValue(paramValue, param.getType());
                            }
                            
                        } else if (param.isAnnotationPresent(ParamObject.class)) {
                            ParamObject paramObjectAnnotation = param.getAnnotation(ParamObject.class);
                            String objName = paramObjectAnnotation.objName();
                            Object paramObjectInstance = param.getType().getDeclaredConstructor().newInstance();
                            Field[] fields = param.getType().getDeclaredFields();
                            for (Field field : fields) {
                                String fieldName = field.getName();
                                String paramValue = req.getParameter(objName + "." + fieldName);
                                field.setAccessible(true);
                                field.set(paramObjectInstance, Util.convertParameterValue(paramValue, field.getType()));
                            }
                            parameterValues[i] = paramObjectInstance;
                        } else {
                            String paramName = param.getName();
                            if (paramName == null || paramName.isEmpty()) {
                                throw new RuntimeException("Parameter name could not be determined for parameter index " + i);
                            }
                            String paramValue = req.getParameter(paramName);
                            parameterValues[i] = Util.convertParameterValue(paramValue, param.getType());
                        }
                    }
                    result = m.invoke(instance, parameterValues);
                }

                if (m.isAnnotationPresent(Restapi.class)) {
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        HashMap<String, Object> data = mv.getData();
                        Gson gson = new Gson();
                        String json = gson.toJson(data);
                        res.setContentType("application/json");
                        out.println(json);
                    } else {
                        Gson gson = new Gson();
                        String json = gson.toJson(result);
                        res.setContentType("application/json");
                        out.println(json);
                    }
                } else {
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        String jspPath = mv.getUrl();
                        ServletContext context = getServletContext();
                        String realPath = context.getRealPath(jspPath);

                        if (realPath == null || !new File(realPath).exists()) {
                            throw new ServletException("The JSP page " + jspPath + " does not exist.");
                        }

                        HashMap<String, Object> data = mv.getData();
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }

                        RequestDispatcher dispatch = req.getRequestDispatcher(jspPath);
                        dispatch.forward(req, res);
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        throw new ServletException("Unknown return type: " + result.getClass().getSimpleName());
                    }
                }

            } catch (Exception e) {
                //e.printStackTrace();
                req.setAttribute("error", e.getMessage());
                RequestDispatcher dispatch = req.getRequestDispatcher("/error.jsp");
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                dispatch.forward(req, res);
            }
        }

        if (!urlExists) {
    out.println("Error 404 - Aucune methode associe a l'URL: " + url);
}

    }
}
