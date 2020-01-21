package edu.harvard.iq.dataverse.interceptors;

import java.lang.reflect.Method;

public class InterceptorCommons {
    public static final String LOG_RECORD_KEY = "LOG_RECORD_KEY";

    static String createName(Method method) {
        Class<?> containingClass = method.getDeclaringClass();
        return containingClass.getSimpleName() + "#" + method.getName();
    }
}
