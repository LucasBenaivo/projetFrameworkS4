package controller;

import annotation.*;
import utils.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.HashMap;
import exception.*;
import javax.servlet.http.Part;
import com.google.gson.Gson;

public class FrontController extends HttpServlet {
    private HashMap<String, Mapping> hashMap;

    @Override
    public void init() throws ServletException {
        try {
            String packageName = this.getInitParameter("nom_package");
            hashMap = Scan.getAllClassSelonAnnotation3(this, packageName, AnnotationController.class);
            System.out.println("Initialization completed. HashMap size: " + hashMap.size());
        } catch (PackageNotFoundException e) {
            throw new ServletException("Initialization error - Package not found: " + e.getMessage(), e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String url = request.getRequestURI().substring(request.getContextPath().length());
        System.out.println("Request URI: " + url);

        if (hashMap != null) {
            Mapping mapping = hashMap.get(url);
            if (mapping != null) {
                try {
                    Class<?> clazz = Class.forName(mapping.getClassName());
                    Method method = null;

                    // Cherche la méthode dans la classe correspondante
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().equals(mapping.getMethodName())) {
                            method = m;
                            break;
                        }
                    }

                    if (method == null) {
                        throw new NoSuchMethodException(
                                "Method " + mapping.getMethodName() + " not found in " + clazz.getName());
                    }

                    // Vérifie si la requête correspond au verbe de la méthode
                    String requestMethod = request.getMethod();
                    String methodVerb = mapping.getVerb();

                    if (!methodVerb.equalsIgnoreCase(requestMethod) && !methodVerb.isEmpty()) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "Method not allowed: " + requestMethod + " for URL: " + url);
                        return;
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Object[] parameterValues = Utils.getParameterValues(request, method, Param.class,
                            ParamObject.class);

                    if (request.getContentType() != null && request.getContentType().startsWith("multipart/")) {
                        for (int i = 0; i < parameterValues.length; i++) {
                            if (parameterValues[i] == null && method.getParameterTypes()[i].equals(MyMultiPart.class)) {
                                Part filePart = request.getPart(method.getParameters()[i].getName());
                                if (filePart != null) {
                                    parameterValues[i] = new MyMultiPart(filePart);
                                }
                            }
                        }
                    }

                    // Initialisation de MySession dans un attribut
                    Field sessionField = null;
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.getType().equals(MySession.class)) {
                            sessionField = field;
                            break;
                        }
                    }
                    if (sessionField != null) {
                        sessionField.setAccessible(true);
                        sessionField.set(instance, new MySession(request.getSession()));
                    }

                    // Initialisation de MySession dans un paramètre
                    for (int i = 0; i < parameterValues.length; i++) {
                        if (parameterValues[i] == null && method.getParameterTypes()[i].equals(MySession.class)) {
                            MySession session = new MySession(request.getSession());
                            parameterValues[i] = session;
                        }
                    }

                    Object result = method.invoke(instance, parameterValues);
                    if (method.isAnnotationPresent(ResponseBody.class)) {
                        if (result instanceof ModelView) {
                            ModelView modelView = (ModelView) result;
                            RequestDispatcher dispatch = request.getRequestDispatcher(modelView.getUrl());
                            HashMap<String, Object> data = modelView.getData();
                            Gson gson = new Gson();
                            String jsonResponse = gson.toJson(data);
                            out.println(jsonResponse);
                        } else {
                            Gson gson = new Gson();
                            String jsonResponse = gson.toJson(result);
                            out.println(jsonResponse);
                        }
                    } else if (result instanceof ModelView) {
                        ModelView modelView = (ModelView) result;
                        RequestDispatcher dispatch = request.getRequestDispatcher(modelView.getUrl());
                        HashMap<String, Object> data = modelView.getData();
                        for (String keyData : data.keySet()) {
                            request.setAttribute(keyData, data.get(keyData));
                            System.out.println(keyData);
                        }
                        dispatch.forward(request, response);
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        out.println("Unsupported return type from controller method.");
                    }
                } catch (Exception e) {
                    out.println("Error invoking method: " + e.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No mapping found for URL: " + url);
            }
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "HashMap is null. Initialization may have failed.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
