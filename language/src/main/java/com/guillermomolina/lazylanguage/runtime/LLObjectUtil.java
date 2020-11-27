package com.guillermomolina.lazylanguage.runtime;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class LLObjectUtil {
    private LLObjectUtil() {
    }

    static Shape createObjectShape(LLContext context, DynamicObject prototype) {
        return Shape.newBuilder().layout(LLObject.class).addConstantProperty(LLObject.PROTOTYPE, prototype, 0).build();
    }

    public static void putProperty(DynamicObject obj, Object key, Object value) {
        DynamicObjectLibrary.getUncached().put(obj, key, value);
    }

    public static Object getProperty(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().getOrDefault(obj, key, null);
    }

    public static boolean hasProperty(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().containsKey(obj, key);
    }
}
