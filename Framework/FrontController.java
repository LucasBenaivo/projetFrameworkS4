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
import java.util.HashMap;
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

        if (map.containsKey(url)) {
            Mapping mapping = map.get(url);
            String requestMethod = req.getMethod();

            try {
                Method m = null;
                for (VerbAction action : mapping.getVerbactions()) {
                    if (action.getVerb().equalsIgnoreCase(requestMethod)) {
                        Class<?> c = Class.forName(mapping.getClassName());
                        m = c.getDeclaredMethod(action.getMethodName());
                        break;
                    }
                }

                if (m == null) {
                    throw new ServletException("Method not found in class " + mapping.getClassName());
                }

                Parameter[] params = m.getParameters();
                Object instance = Class.forName(mapping.getClassName()).getDeclaredConstructor().newInstance();
                injectSession(instance, req);

                Object result = invokeMethod(m, instance, req, params);
                handleResponse(res, out, result, m);

            } catch (Exception e) {
                req.setAttribute("error", e.getMessage());
                RequestDispatcher dispatch = req.getRequestDispatcher("/error.jsp");
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                dispatch.forward(req, res);
            }
        } else {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("Error 404 - Aucune méthode associée à l'URL: " + url);
        }
    }

    private void injectSession(Object instance, HttpServletRequest req) throws IllegalAccessException {
        Field[] attributs = instance.getClass().getDeclaredFields();
        for (Field field : attributs) {
            if (field.getType().equals(MySession.class)) {
                HttpSession httpSession = req.getSession(true);
                MySession session = new MySession(httpSession);
                field.setAccessible(true);
                field.set(instance, session);
            }
        }
    }

    private Object invokeMethod(Method m, Object instance, HttpServletRequest req, Parameter[] params) throws Exception {
        if (params.length < 1) {
            return m.invoke(instance);
        }

        Object[] parameterValues = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterValues[i] = resolveParameter(req, params[i]);
        }
        return m.invoke(instance, parameterValues);
    }

    private Object resolveParameter(HttpServletRequest req, Parameter param) throws Exception {
        if (param.getType().equals(MySession.class)) {
            return new MySession(req.getSession(true));
        } else if (param.isAnnotationPresent(Param.class)) {
            return Util.convertParameterValue(req.getParameter(param.getAnnotation(Param.class).name()), param.getType());
        } else if (param.isAnnotationPresent(ParamObject.class)) {
            return resolveParamObject(req, param);
        } else {
            return Util.convertParameterValue(req.getParameter(param.getName()), param.getType());
        }
    }

    private Object resolveParamObject(HttpServletRequest req, Parameter param) throws Exception {
        ParamObject paramObjectAnnotation = param.getAnnotation(ParamObject.class);
        Object paramObjectInstance = param.getType().getDeclaredConstructor().newInstance();
        Field[] fields = param.getType().getDeclaredFields();
        for (Field field : fields) {
            String paramValue = req.getParameter(paramObjectAnnotation.objName() + "." + field.getName());
            field.setAccessible(true);
            field.set(paramObjectInstance, Util.convertParameterValue(paramValue, field.getType()));
        }
        return paramObjectInstance;
    }

    private void handleResponse(HttpServletResponse res, PrintWriter out, Object result, Method m) throws IOException, ServletException {
        if (m.isAnnotationPresent(Restapi.class)) {
            res.setContentType("application/json");
            Gson gson = new Gson();
            out.println(gson.toJson(result instanceof ModelView ? ((ModelView) result).getData() : result));
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            String jspPath = mv.getUrl();
            ServletContext context = getServletContext();
            String realPath = context.getRealPath(jspPath);

            if (realPath == null || !new File(realPath).exists()) {
                throw new ServletException("The JSP page " + jspPath + " does not exist.");
            }

            for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }

            RequestDispatcher dispatch = req.getRequestDispatcher(jspPath);
            dispatch.forward(req, res);
        } else if (result instanceof String) {
            out.println(result);
        } else {
            throw new ServletException("Unknown return type: " + result.getClass().getSimpleName());
        }
    }
}
