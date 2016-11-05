package com.droi.common.factories;

import static com.droi.common.util.Reflection.MethodBuilder;

public class MethodBuilderFactory {
    protected static MethodBuilderFactory instance = new MethodBuilderFactory();

    @Deprecated // for testing
    public static void setInstance(MethodBuilderFactory factory) {
        instance = factory;
    }

    public static MethodBuilder create(Object object, String methodName) {
        return instance.internalCreate(object, methodName);
    }

    protected MethodBuilder internalCreate(Object object, String methodName) {
        return new MethodBuilder(object, methodName);
    }
}

