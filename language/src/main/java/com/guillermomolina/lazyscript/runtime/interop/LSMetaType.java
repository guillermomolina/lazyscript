/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
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
package com.guillermomolina.lazyscript.runtime.interop;

import com.guillermomolina.lazyscript.LSLanguage;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.objects.LSBigInteger;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * The builtin type definitions for LazyScript. LazyScript has no custom types, so it is not possible
 * for a guest program to create new instances of LSType.
 * <p>
 * The isInstance type checks are declared using an functional interface and are expressed using the
 * interoperability libraries. The advantage of this is type checks automatically work for foreign
 * values or primitive values like byte or short.
 * <p>
 * The class implements the interop contracts for {@link InteropLibrary#isMetaObject(Object)} and
 * {@link InteropLibrary#isMetaInstance(Object, Object)}. The latter allows other languages and
 * tools to perform type checks using types of lazy language.
 * <p>
 * In order to assign types to guest language values, LazyScript values implement
 * {@link InteropLibrary#getMetaObject(Object)}. The interop contracts for primitive values cannot
 * be overriden, so in order to assign meta-objects to primitive values, the primitive values are
 * assigned using language views. See {@link LSLanguage#getLanguageView}.
 */
@ExportLibrary(InteropLibrary.class)
public final class LSMetaType implements TruffleObject {

    /*
     * These are the sets of builtin types in lazy languages. In case of lazy language the types
     * nicely match those of the types in InteropLibrary. This might not be the case and more
     * additional checks need to be performed (similar to number checking for LSBigInteger).
     */
    public static final LSMetaType NULL = new LSMetaType("NULL", InteropLibrary::isNull);
    public static final LSMetaType INTEGER = new LSMetaType("INTEGER", InteropLibrary::fitsInLong);
    public static final LSMetaType DECIMAL = new LSMetaType("DECIMAL", InteropLibrary::fitsInDouble);
    public static final LSMetaType BIGINTEGER = new LSMetaType("BIGINTEGER", (l, v) -> v instanceof LSBigInteger);
    public static final LSMetaType STRING = new LSMetaType("STRING", InteropLibrary::isString);
    public static final LSMetaType BOOLEAN = new LSMetaType("BOOLEAN", InteropLibrary::isBoolean);
    public static final LSMetaType FUNCTION = new LSMetaType("FUNCTION", InteropLibrary::isExecutable);
    public static final LSMetaType ARRAY = new LSMetaType("ARRAY", InteropLibrary::hasArrayElements);
    public static final LSMetaType OBJECT = new LSMetaType("OBJECT", InteropLibrary::hasMembers);

    /*
     * This array is used when all types need to be checked in a certain order. While most interop
     * types like number or string are exclusive, others traits like members might not be. For
     * example, an object might be a function.
     */
    @CompilationFinal(dimensions = 1) public static final LSMetaType[] PRECEDENCE = new LSMetaType[]{NULL, INTEGER, DECIMAL, BIGINTEGER, STRING, BOOLEAN, FUNCTION, ARRAY, OBJECT};

    private final String name;
    private final TypeCheck isInstance;

    /*
     * We don't allow dynamic instances of LSType. Real languages might want to expose this for
     * types that are user defined.
     */
    private LSMetaType(String name, TypeCheck isInstance) {
        this.name = name;
        this.isInstance = isInstance;
    }

    /**
     * Checks whether this type is of a certain instance. If used on fast-paths it is required to
     * cast {@link LSMetaType} to a constant.
     */
    public boolean isInstance(Object value, InteropLibrary interop) {
        CompilerAsserts.partialEvaluationConstant(this);
        return isInstance.check(interop, value);
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<LSContext>> getLanguage() {
        return LSLanguage.class;
    }

    /*
     * All LSTypes are declared as interop meta-objects. Other example for meta-objects are Java
     * classes, or JavaScript prototypes.
     */
    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    /*
     * LazyScript does not have the notion of a qualified or lazy name, so we return the same type name
     * for both.
     */
    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    public Object getName() {
        return name;
    }

    @ExportMessage(name = "toDisplayString")
    Object toDisplayString(boolean allowSideEffects) {
        return name;
    }

    @Override
    public String toString() {
        return "LSType[" + name + "]";
    }

    /*
     * The interop message isMetaInstance might be used from other languages or by the {@link
     * LSIsInstanceBuiltin isInstance} builtin. It checks whether a given value, which might be a
     * primitive, foreign or LazyScript value is of a given LazyScript type. This allows other languages to make
     * their instanceOf interopable with foreign values.
     */
    @ExportMessage
    static class IsMetaInstance {

        /*
         * We assume that the same type is checked at a source location. Therefore we use an inline
         * cache to specialize for observed types to be constant. The limit of "3" specifies that we
         * specialize for 3 different types until we rewrite to the doGeneric case. The limit in
         * this example is somewhat arbitrary and should be determined using careful tuning with
         * real world benchmarks.
         */
        @Specialization(guards = "type == cachedType", limit = "3")
        static boolean doCached(LSMetaType type, Object value,
                        @Cached("type") LSMetaType cachedType,
                        @CachedLibrary("value") InteropLibrary valueLib) {
            return cachedType.isInstance.check(valueLib, value);
        }

        @TruffleBoundary
        @Specialization(replaces = "doCached")
        static boolean doGeneric(LSMetaType type, Object value) {
            return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
        }
    }

    /*
     * A convenience interface for type checks. Alternatively this could have been solved using
     * subtypes of LSType.
     */
    @FunctionalInterface
    interface TypeCheck {

        boolean check(InteropLibrary lib, Object value);

    }

}
