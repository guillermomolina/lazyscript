package com.guillermomolina.lazylanguage.runtime;

import com.guillermomolina.lazylanguage.NotImplementedException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class LLObjectUtil {
    private LLObjectUtil() {
    }

    public static Object getFunction(LLContext context, Object obj, Object key) {
        LLObject object;
        if(obj instanceof LLObject) {
            object = (LLObject)obj;
        } else if (obj instanceof String) {
            object = context.getStringPrototype();
        } else {
            throw new NotImplementedException();
        }
        if(hasProperty(object, key)) {
            return getProperty(object, key);
        }
        Object parent = getProperty(object, LLObject.PROTOTYPE);
        if(parent != null) {
            return getFunction(context, parent, key);
        }
        throw new NotImplementedException();
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
