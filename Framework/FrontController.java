package controller;

import util.Util;
import util.Mapping;
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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import annotation.*;
import model.*;

public class FrontController extends HttpServlet {
    private HashMap<String, Mapping> map;

    @Override
    public void init() throws ServletException {
        try {
            String packageName = this.getInitParameter("package_name");
            map = Util.getAllClassesSelonAnnotation(packageName, ControllerAnnotation.class);
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

            try {
                Class<?> c = Class.forName(mapping.getClassName());
                Method[]methods=c.getDeclaredMethods();
                Method m = null;
                for (Method method : methods) {
                    if (method.getName().equals(mapping.getMethodeName())) {
                        m = method;
                    }
                }
                

                Parameter[] params = m.getParameters();
                int methodParamCount = params.length;
                List<String> paramNames = Collections.list(req.getParameterNames());
                int requestParamCount = paramNames.size();
                if (methodParamCount != requestParamCount) {
                    out.println("Error: The number of parameters sent (" + requestParamCount + ") does not match the number of parameters required by the method (" + methodParamCount + ").");
                    return;
                }

                Object instance = c.getDeclaredConstructor().newInstance();
                Object result;

                if (methodParamCount < 1) {
                    result = m.invoke(instance);
                } else {
                    Object[] paramValues = new Object[methodParamCount];
                    for (int i = 0; i < params.length; i++) {
                        String paramName = params[i].isAnnotationPresent(Param.class)
                            ? params[i].getAnnotation(Param.class).name()
                            : params[i].getName();

                        String paramValue = req.getParameter(paramName);
                        paramValues[i] = Util.convertParameterValue(paramValue, params[i].getType());
                    }
                    result = m.invoke(instance, paramValues);
                }

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
            } catch (Exception e) {
                e.printStackTrace();
                out.println("Error: " + e.getMessage());
            }
        }

        if (!urlExists) {
            out.println("No method is associated with the URL: " + url);
        }
    }
}
