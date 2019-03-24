package fr.free.nrw.commons.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Returns a new instance of proxy with overriden invocationhanlder() returning appropriate values
 * for different datatypes
 * See https://stackoverflow.com/questions/52083338/expected-to-unbox-a-string-primitive-type-but-was-returned-null
 */
public class CustomProxy extends Proxy {
    protected CustomProxy(InvocationHandler h) {
        super(h);
    }

    public static Object newInstance(ClassLoader loader, Class<?>[] interfaces) {
        return Proxy.newProxyInstance(loader, interfaces, (o, method, objects) -> {
            if (String.class == method.getReturnType()) {
                return "";
            } else if (Integer.class == method.getReturnType()) {
                return Integer.valueOf(0);
            } else if (int.class == method.getReturnType()) {
                return 0;
            } else if (Boolean.class == method.getReturnType()) {
                return Boolean.FALSE;
            } else if (boolean.class == method.getReturnType()) {
                return false;
            } else {
                return null;
            }
        });
    }
}
