package com.guillermomolina.lazylanguage.runtime;

import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

public final class LLObjectUtil {
    private LLObjectUtil() {
    }

    public static Object getFunction(LLObject object, String name) throws UnknownIdentifierException {
        DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
        return object.getFunction(name, objectLibrary);
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
