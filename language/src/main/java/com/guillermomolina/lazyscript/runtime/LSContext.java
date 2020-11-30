/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.guillermomolina.lazyscript.runtime;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;

import com.guillermomolina.lazyscript.LazyScriptLanguage;
import com.guillermomolina.lazyscript.NotImplementedException;
import com.guillermomolina.lazyscript.builtins.LSBuiltinNode;
import com.guillermomolina.lazyscript.builtins.LSDefineFunctionBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSEvalBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSGetSizeBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSHasSizeBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSHelloEqualsWorldBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSImportBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSIsExecutableBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSIsInstanceBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSIsNullBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSNanoTimeBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSNewObjectBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSPrintlnBuiltin;
import com.guillermomolina.lazyscript.builtins.LSPrintlnBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSReadlnBuiltin;
import com.guillermomolina.lazyscript.builtins.LSReadlnBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSStackTraceBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSTypeOfBuiltinFactory;
import com.guillermomolina.lazyscript.builtins.LSWrapPrimitiveBuiltinFactory;
import com.guillermomolina.lazyscript.nodes.LSExpressionNode;
import com.guillermomolina.lazyscript.nodes.LSRootNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadArgumentNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;

/**
 * The run-time state of Lazy during execution. The topContext is created by the
 * {@link LazyScriptLanguage}. It is used, for example, by
 * {@link LSBuiltinNode#getContext() builtin functions}.
 * <p>
 * It would be an error to have two different topContext instances during the
 * execution of one script. However, if two separate scripts run in one Java VM
 * at the same time, they have a different topContext. Therefore, the topContext
 * is not a singleton.
 */
public final class LSContext {

    private static final Source BUILTIN_SOURCE = Source.newBuilder(LazyScriptLanguage.ID, "", "Lazy builtin").build();

    private final Env env;
    private final BufferedReader input;
    private final PrintWriter output;
    private final LazyScriptLanguage language;
    @CompilationFinal private AllocationReporter allocationReporter;
    private final Iterable<Scope> topScopes; // Cache the top scopes


    private final LSObject objectPrototype;
    private final LSObject nullPrototype;
    private final LSObject booleanPrototype;
    private final LSObject functionPrototype;
    private final LSObject stringPrototype;
    private final LSObject arrayPrototype;
    private final LSObject numberPrototype;
    private final LSObject integerPrototype;
    private final LSObject realPrototype;
    private final LSObject truePrototype;
    private final LSObject falsePrototype;
    private final LSObject topContext;

    public LSContext(LazyScriptLanguage language, TruffleLanguage.Env env) {
        if (env != null) { // env could still be null
            setAllocationReporter(env);
        }
        this.language = language;
        this.env = env;

        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.topScopes = Collections.singleton(Scope.newBuilder("global", new FunctionsObject()).build());

        this.objectPrototype = createObject(LSNull.INSTANCE);
        this.nullPrototype = createObject(objectPrototype);
        this.booleanPrototype = createObject(objectPrototype);
        this.truePrototype = createObject(booleanPrototype);
        this.falsePrototype = createObject(booleanPrototype);
        this.numberPrototype = createObject(objectPrototype);
        this.integerPrototype = createObject(numberPrototype);
        this.realPrototype = createObject(numberPrototype);
        this.arrayPrototype = createObject(objectPrototype);
        this.stringPrototype = createObject(objectPrototype);
        this.functionPrototype = createObject(objectPrototype);
        this.topContext = createObject(objectPrototype);
        installBuiltins();
    }

    void setAllocationReporter(Env env) {
        CompilerAsserts.neverPartOfCompilation();
        this.allocationReporter = env.lookup(AllocationReporter.class);
    }

    /**
     * Allocate an empty object. All new objects initially have no properties.
     * Properties are added when they are first stored, i.e., the store triggers a
     * shape change of the object.
     */
    public LSObject createObject(Object prototype) {
        allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LSObject object = new LSObject();
        object.setPrototype(prototype);
        allocationReporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        return object;
    }

    public LSObject createObject() {
        return createObject(objectPrototype);
    }

    public LSFunction createFunction(RootCallTarget callTarget) {
        allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LSFunction function = new LSFunction(callTarget);
        function.setPrototype(functionPrototype);
        allocationReporter.onReturnValue(function, 0, AllocationReporter.SIZE_UNKNOWN);
        return function;
    }

    public LSArray createArray(final Object[] data) {
        allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LSArray array = new LSArray(data);
        array.setPrototype(arrayPrototype);
        allocationReporter.onReturnValue(array, 0, AllocationReporter.SIZE_UNKNOWN);
        return array;
    }

    /**
     * Return the current Truffle environment.
     */
    public Env getEnv() {
        return env;
    }

    /**
     * Returns the default input, i.e., the source for the {@link LSReadlnBuiltin}.
     * To allow unit testing, we do not use {@link System#in} directly.
     */
    public BufferedReader getInput() {
        return input;
    }

    /**
     * The default default, i.e., the output for the {@link LSPrintlnBuiltin}. To
     * allow unit testing, we do not use {@link System#out} directly.
     */
    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Returns the registry of all functions that are currently defined.
     */
    public LSObject getTopContext() {
        return topContext;
    }

    public Iterable<Scope> getTopScopes() {
        return topScopes;
    }

    /**
     * Adds all builtin functions to the {@link LSFunctionRegistry}. This method
     * lists all {@link LSBuiltinNode builtin implementation classes}.
     */
    private void installBuiltins() {
        LSObjectUtil.putProperty(topContext, "null", LSNull.INSTANCE);
        LSObjectUtil.putProperty(topContext, "true", true);
        LSObjectUtil.putProperty(topContext, "false", false);
        LSObjectUtil.putProperty(topContext, "Object", objectPrototype);
        LSObjectUtil.putProperty(topContext, "Null", nullPrototype);
        LSObjectUtil.putProperty(topContext, "Boolean", booleanPrototype);
        LSObjectUtil.putProperty(topContext, "True", truePrototype);
        LSObjectUtil.putProperty(topContext, "False", falsePrototype);
        LSObjectUtil.putProperty(topContext, "Number", numberPrototype);
        LSObjectUtil.putProperty(topContext, "Integer", integerPrototype);
        LSObjectUtil.putProperty(topContext, "Real", realPrototype);
        LSObjectUtil.putProperty(topContext, "String", stringPrototype);
        LSObjectUtil.putProperty(topContext, "Array", stringPrototype);
        LSObjectUtil.putProperty(topContext, "Number", numberPrototype);

        installBuiltin(LSReadlnBuiltinFactory.getInstance());
        installBuiltin(LSPrintlnBuiltinFactory.getInstance());
        installBuiltin(LSNanoTimeBuiltinFactory.getInstance());
        installBuiltin(LSDefineFunctionBuiltinFactory.getInstance());
        installBuiltin(LSStackTraceBuiltinFactory.getInstance());
        installBuiltin(LSHelloEqualsWorldBuiltinFactory.getInstance());
        installBuiltin(LSNewObjectBuiltinFactory.getInstance());
        installBuiltin(LSEvalBuiltinFactory.getInstance());
        installBuiltin(LSImportBuiltinFactory.getInstance());
        installBuiltin(LSGetSizeBuiltinFactory.getInstance());
        installBuiltin(LSHasSizeBuiltinFactory.getInstance());
        installBuiltin(LSIsExecutableBuiltinFactory.getInstance());
        installBuiltin(LSIsNullBuiltinFactory.getInstance());
        installBuiltin(LSWrapPrimitiveBuiltinFactory.getInstance());
        installBuiltin(LSTypeOfBuiltinFactory.getInstance());
        installBuiltin(LSIsInstanceBuiltinFactory.getInstance());
    }

    public void installBuiltin(NodeFactory<? extends LSBuiltinNode> factory) {
        /*
         * The builtin node factory is a class that is automatically generated by the
         * Truffle DLL. The signature returned by the factory reflects the signature of
         * the @Specialization
         *
         * methods in the builtin classes.
         */
        int argumentCount = factory.getExecutionSignature().size();
        LSExpressionNode[] argumentNodes = new LSExpressionNode[argumentCount];
        /*
         * Builtin functions are like normal functions, i.e., the arguments are passed
         * in as an Object[] array encapsulated in LSArguments. A LSReadArgumentNode
         * extracts a parameter from this array.
         */
        for (int i = 0; i < argumentCount; i++) {
            argumentNodes[i] = new LSReadArgumentNode(i);
        }
        /* Instantiate the builtin node. This node performs the actual functionality. */
        LSBuiltinNode builtinBodyNode = factory.createNode((Object) argumentNodes);
        builtinBodyNode.addRootTag();
        /*
         * The name of the builtin function is specified via an annotation on the node
         * class.
         */
        String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();
        builtinBodyNode.setUnavailableSourceSection();

        /*
         * Wrap the builtin in a RootNode. Truffle requires all AST to start with a
         * RootNode.
         */
        LSRootNode rootNode = new LSRootNode(language, new FrameDescriptor(), builtinBodyNode,
                BUILTIN_SOURCE.createUnavailableSection());
        RootCallTarget rootCallTarget = Truffle.getRuntime().createCallTarget(rootNode);
        LSFunction rootFunction = createFunction(rootCallTarget);
        LSObjectUtil.putProperty(objectPrototype, name, rootFunction);
    }

    public static NodeInfo lookupNodeInfo(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        NodeInfo info = clazz.getAnnotation(NodeInfo.class);
        if (info != null) {
            return info;
        } else {
            return lookupNodeInfo(clazz.getSuperclass());
        }
    }

    public Object getPrototype(Object obj) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        if (obj instanceof LSObject) {
            return ((LSObject) obj).getPrototype();
        } else if (obj instanceof String) {
            return stringPrototype;
        } else if (obj instanceof LSBigInteger || interop.fitsInLong(obj)) {
            return integerPrototype;
        } else if (interop.fitsInDouble(obj)) {
            return realPrototype;
        } else if (interop.isNull(obj)) {
            return nullPrototype;
        } else if (interop.isBoolean(obj)) {
            return (boolean) obj ? truePrototype : falsePrototype;
        } else {
            throw new NotImplementedException();
        }
    }

    public Object getFunction(Object obj, String name) throws UnknownIdentifierException {
        LSObject object;
        if(obj instanceof LSObject) {
            object = (LSObject)obj;
        } else {
            object = (LSObject)getPrototype(obj);
        }
        return LSObjectUtil.getFunction(object, name);
    }

    /*
     * Methods for object creation / object property access.
     */
    public AllocationReporter getAllocationReporter() {
        return allocationReporter;
    }

    /*
     * Methods for language interoperability.
     */

    public static Object fromForeignValue(Object a) {
        if (a instanceof Long || a instanceof LSBigInteger || a instanceof String || a instanceof Boolean) {
            return a;
        } else if (a instanceof Character) {
            return fromForeignCharacter((Character) a);
        } else if (a instanceof Number) {
            return fromForeignNumber(a);
        } else if (a instanceof TruffleObject) {
            return a;
        } else if (a instanceof LSContext) {
            return a;
        }
        throw shouldNotReachHere("Value is not a truffle value.");
    }

    @TruffleBoundary
    private static long fromForeignNumber(Object a) {
        return ((Number) a).longValue();
    }

    @TruffleBoundary
    private static String fromForeignCharacter(char c) {
        return String.valueOf(c);
    }

    public CallTarget parse(Source source) {
        return env.parsePublic(source);
    }

    /**
     * Returns an object that contains bindings that were exported across all used languages. To
     * read or write from this object the {@link TruffleObject interop} API can be used.
     */
    public TruffleObject getPolyglotBindings() {
        return (TruffleObject) env.getPolyglotBindings();
    }

    public static LSContext getCurrent() {
        return LazyScriptLanguage.getCurrentContext();
    }

}
