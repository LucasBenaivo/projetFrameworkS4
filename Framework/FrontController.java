package controller;

import util.*;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
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
        res.setContentType("text/plain");
        PrintWriter out = res.getWriter();
        out.println("Tongasoa ato am FrontController");

        String url = req.getRequestURI();
        boolean urlExists = false;

        for (String key : map.keySet()) {
            if (key.equals(url)) {
                out.println("Votre url : " + url + " est associe a la methode : " + map.get(key).getMethodeName() + " dans la classe : " + map.get(key).getClassName());
                Mapping mapping = map.get(url);

                try {
                    Class<?> c = Class.forName(mapping.getClassName());
                    Method m = c.getDeclaredMethod(mapping.getMethodeName());
                    Object instance = c.getDeclaredConstructor().newInstance();
                    Object result = m.invoke(instance);

                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        RequestDispatcher dispatch = req.getRequestDispatcher(mv.getUrl());
                        HashMap<String, Object> data = mv.getData();
                        for (String keyData : data.keySet()) {
                            req.setAttribute(keyData, data.get(keyData));
                        }
                        dispatch.forward(req, res);
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        out.println("Erreur: Type de retour inconnu");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    out.println("Erreur lors de l'invocation de la methode: " + e.getMessage());
                }

                urlExists = true;
                break;
            }
        }

        if (!urlExists) {
            out.println("Aucune methode n'est associee a l'url : " + url);
        }
    }
}
