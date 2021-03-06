package com.guillermomolina.lazyscript.runtime;

import com.guillermomolina.lazyscript.runtime.objects.LSFunction;
import com.guillermomolina.lazyscript.runtime.objects.LSObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

public final class LSObjectUtil {
    private LSObjectUtil() {
    }

    public static LSFunction getFunction(LSObject object, String name) throws UnknownIdentifierException {
        DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
        return (LSFunction)object.getFunction(name, objectLibrary);
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
