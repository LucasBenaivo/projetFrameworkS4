package utils;

import javax.servlet.http.HttpServletRequest;
import com.thoughtworks.paranamer.*;
import java.lang.reflect.*;

import annotation.*;

public class Utils {
    public static Object[] getParameterValues(HttpServletRequest request, Method method,
            Class<Param> paramAnnotationClass, Class<ParamObject> paramObjectAnnotationClass) {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(paramAnnotationClass)) {
                Param param = parameters[i].getAnnotation(paramAnnotationClass);
                String paramName = param.value();
                String paramValue = request.getParameter(paramName);
                parameterValues[i] = convertParameterValue(paramValue, parameters[i].getType());
            } else if (parameters[i].isAnnotationPresent(paramObjectAnnotationClass)) {
                ParamObject paramObject = parameters[i].getAnnotation(paramObjectAnnotationClass);
                String objName = paramObject.objName();
                try {
                    Object paramObjectInstance = parameters[i].getType().getDeclaredConstructor().newInstance();
                    Field[] fields = parameters[i].getType().getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(objName + "." + fieldName);
                        if (paramValue != null) {
                            field.setAccessible(true);
                            field.set(paramObjectInstance, convertParameterValue(paramValue, field.getType()));
                        }
                    }
                    parameterValues[i] = paramObjectInstance;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create and populate parameter object: " + e.getMessage());
                }
            } else {
                // throw new Exception(
                // "ETU002746 Annotation not found");
                String paramName = parameters[i].getName();
                String paramValue = request.getParameter(paramName);
                parameterValues[i] = convertParameterValue(paramValue,
                        parameters[i].getType());
            }
        }
        return parameterValues;
    }

    /* On traite les types primitives */
    private static Object convertParameterValue(String value, Class<?> type) {
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
