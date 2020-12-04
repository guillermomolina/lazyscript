/*
 * Copyright (c) 2012, 2019, Guillermo Adrián Molina. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.guillermomolina.lazyscript.runtime.objects;

import com.guillermomolina.lazyscript.LSLanguage;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.LSObjectUtil;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;

/**
 * Represents an LazyScript object.
 *
 * This class defines operations that can be performed on LazyScript Objects. While we
 * could define all these operations as individual AST nodes, we opted to define
 * those operations by using {@link com.oracle.truffle.api.library.Library a
 * Truffle library}, or more concretely the {@link InteropLibrary}. This has
 * several advantages, but the primary one is that it allows LazyScript objects to be
 * used in the interoperability message protocol, i.e. It allows other languages
 * and tools to operate on LazyScript objects without necessarily knowing they are
 * LazyScript objects.
 *
 * LazyScript Objects are essentially instances of {@link DynamicObject} (objects
 * whose members can be dynamically added and removed). We also annotate the
 * class with {@link ExportLibrary} with value {@link InteropLibrary
 * InteropLibrary.class}. This essentially ensures that the build system and
 * runtime know that this class specifies the interop messages (i.e. operations)
 * that LazyScript can do on {@link LSObject} instances.
 *
 * @see ExportLibrary
 * @see ExportMessage
 * @see InteropLibrary
 */
@ExportLibrary(InteropLibrary.class)
public class LSObject extends DynamicObject {
    protected static final int CACHE_LIMIT = 3;
    public static final String PROTOTYPE = "prototype";
    public static final Shape SHAPE = Shape.newBuilder().layout(LSObject.class)
            .addConstantProperty(LSObject.PROTOTYPE, null, 0).build();;

    public LSObject() {
        super(SHAPE);
    }

    public LSObject(Object prototype) {
        super(SHAPE);
        setPrototype(prototype);
    }

    public Object getPrototype() {
        Object prototype = LSObjectUtil.getProperty(this, PROTOTYPE);
        if (prototype == LSNull.INSTANCE) {
            return null;
        }
        return prototype;
    }

    public void setPrototype(Object prototype) {
        LSObjectUtil.putProperty(this, PROTOTYPE, prototype);
    }

    @TruffleBoundary
    public Object getFunction(String name, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        LSObject object = this;
        while (object != null) {
            Object result = objectLibrary.getOrDefault(object, name, null);
            if (result instanceof LSFunction) {
                return result;
            }
            object = (LSObject) object.getPrototype();
        }
        throw UnknownIdentifierException.create(name);
    }

    @TruffleBoundary
    Object getProperty(String name, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        LSObject object = this;
        while (object != null) {
            Object result = objectLibrary.getOrDefault(object, name, null);
            if (result != null) {
                return result;
            }
            object = (LSObject) object.getPrototype();
        }
        throw UnknownIdentifierException.create(name);
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<LSContext>> getLanguage() {
        return LSLanguage.class;
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        public IsIdenticalOrUndefined() {
        }

        @Specialization
        static TriState doLLObject(LSObject receiver, LSObject other) {
            return TriState.valueOf(receiver == other);
        }

        @Fallback
        static TriState doOther(LSObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    int identityHashCode() {
        return System.identityHashCode(this);
    }

    @ExportMessage
    @TruffleBoundary
    Object toDisplayString(boolean allowSideEffects) {
        return "anObject";
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    void removeMember(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        if (objectLibrary.containsKey(this, member)) {
            objectLibrary.removeKey(this, member);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    Object getMembers(boolean includeInternal, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return new Keys(objectLibrary.getKeyArray(this));
    }

    @ExportMessage(name = "isMemberReadable")
    @ExportMessage(name = "isMemberModifiable")
    @ExportMessage(name = "isMemberRemovable")
    boolean existsMember(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return objectLibrary.containsKey(this, member);
    }

    @ExportMessage
    boolean isMemberInsertable(String member, @CachedLibrary("this") InteropLibrary receivers) {
        return !receivers.isMemberExisting(this, member);
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Keys implements TruffleObject {

        private final Object[] data;

        Keys(Object[] keys) {
            this.data = keys;
        }

        @ExportMessage
        Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (!isArrayElementReadable(index)) {
                throw InvalidArrayIndexException.create(index);
            }
            return data[(int) index];
        }

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return data.length;
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < data.length;
        }
    }

    /**
     * {@link DynamicObjectLibrary} provides the polymorphic inline cache for
     * reading properties.
     */
    // @ExportMessage
    Object readOwnMember(String name, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        Object result = objectLibrary.getOrDefault(this, name, null);
        if (result == null) {
            /* Property does not exist. */
            throw UnknownIdentifierException.create(name);
        }
        return result;
    }

    @ExportMessage
    Object readMember(String name, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        LSObject object = this;
        while (object != null) {
            Object result = objectLibrary.getOrDefault(object, name, null);
            if (result != null) {
                return result;
            }
            object = (LSObject) object.getPrototype();
        }
        throw UnknownIdentifierException.create(name);
    }

    /**
     * {@link DynamicObjectLibrary} provides the polymorphic inline cache for
     * writing properties.
     */
    @ExportMessage
    void writeMember(String name, Object value, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        objectLibrary.put(this, name, value);
    }
}
