package controller;

import annotation.*;
import utils.Mapping;
import utils.ModelView;
import utils.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import exception.*;

public class FrontController extends HttpServlet {
    private HashMap<String, Mapping> hashMap;

    @Override
    public void init() throws ServletException {
        try {
            String packageName = this.getInitParameter("package_name");
            System.out.println(packageName);
            hashMap = Scan.getAllClassSelonAnnotation2(this, packageName, AnnotationController.class);
            // PrintWriter out = response.getWriter();
            for (String key : hashMap.keySet()) {
                System.out.println(key);
            }
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

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Object[] parameterValues = Utils.getParameterValues(request, method, Param.class,
                            ParamObject.class);

                    Object result = method.invoke(instance, parameterValues);
                    out.println(result);
                    System.out.println(result);
                    if (result instanceof ModelView) {
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
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Error invoking method: " + e.getMessage());
                    e.printStackTrace(out);
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
