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
package com.guillermomolina.lazylanguage.runtime;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;

import com.guillermomolina.lazylanguage.LLLanguage;
import com.guillermomolina.lazylanguage.NotImplementedException;
import com.guillermomolina.lazylanguage.builtins.LLBuiltinNode;
import com.guillermomolina.lazylanguage.builtins.LLDefineFunctionBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLEvalBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLGetSizeBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLHasSizeBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLHelloEqualsWorldBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLImportBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLIsExecutableBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLIsInstanceBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLIsNullBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLNanoTimeBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLNewObjectBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLPrintlnBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLPrintlnBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLReadlnBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLReadlnBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLStackTraceBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLTypeOfBuiltinFactory;
import com.guillermomolina.lazylanguage.builtins.LLWrapPrimitiveBuiltinFactory;
import com.guillermomolina.lazylanguage.nodes.LLExpressionNode;
import com.guillermomolina.lazylanguage.nodes.LLRootNode;
import com.guillermomolina.lazylanguage.nodes.local.LLReadArgumentNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;

/**
 * The run-time state of Lazy during execution. The topContext is created by the {@link LLLanguage}. It
 * is used, for example, by {@link LLBuiltinNode#getContext() builtin functions}.
 * <p>
 * It would be an error to have two different topContext instances during the execution of one script.
 * However, if two separate scripts run in one Java VM at the same time, they have a different
 * topContext. Therefore, the topContext is not a singleton.
 */
public final class LLContext {

    private static final Source BUILTIN_SOURCE = Source.newBuilder(LLLanguage.ID, "", "Lazy builtin").build();

    private final Env env;
    private final BufferedReader input;
    private final PrintWriter output;
    private final LLLanguage language;
    private final AllocationReporter allocationReporter;
    private final Iterable<Scope> topScopes; // Cache the top scopes

    private final LLObject objectPrototype;
    private final LLObject nullPrototype;
    private final LLObject booleanPrototype;
    private final LLObject functionPrototype;
    private final LLObject stringPrototype;
    private final LLObject numberPrototype;
    private final LLObject integerPrototype;
    private final LLObject bigIntegerPrototype;
    private final LLObject floatPrototype;
    private final LLObject truePrototype;
    private final LLObject falsePrototype;
    private final LLObject topContext;

    public LLContext(LLLanguage language, TruffleLanguage.Env env) {
        this.env = env;
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new PrintWriter(env.out(), true);
        this.language = language;
        this.allocationReporter = env.lookup(AllocationReporter.class);
        this.topScopes = Collections.singleton(Scope.newBuilder("global", new FunctionsObject()).build());

        this.objectPrototype = createObject(null);
        this.nullPrototype = createObject(objectPrototype);
        this.booleanPrototype = createObject(objectPrototype);
        this.truePrototype = createObject(booleanPrototype);
        this.falsePrototype = createObject(booleanPrototype);
        this.numberPrototype = createObject(objectPrototype);
        this.integerPrototype = createObject(numberPrototype);
        this.bigIntegerPrototype = createObject(numberPrototype);
        this.floatPrototype = createObject(numberPrototype);
        this.stringPrototype = createObject(objectPrototype);
        this.functionPrototype = createObject(objectPrototype);
        this.topContext = createObject(objectPrototype);
        installBuiltins();
    }

    public LLObject createObject(LLObject prototype) {
        LLObject object = language.createObject(allocationReporter);
        object.setPrototype(prototype);
        return object;
    }

    public LLFunction createFunction(String name, RootCallTarget callTarget) {
        LLFunction function = language.createFunction(allocationReporter, name, callTarget);
        function.setPrototype(functionPrototype);
        return function;
    }

    /**
     * Return the current Truffle environment.
     */
    public Env getEnv() {
        return env;
    }

    /**
     * Returns the default input, i.e., the source for the {@link LLReadlnBuiltin}. To allow unit
     * testing, we do not use {@link System#in} directly.
     */
    public BufferedReader getInput() {
        return input;
    }

    /**
     * The default default, i.e., the output for the {@link LLPrintlnBuiltin}. To allow unit
     * testing, we do not use {@link System#out} directly.
     */
    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Returns the registry of all functions that are currently defined.
     */
    public LLObject getTopContext() {
        return topContext;
    }

    public Iterable<Scope> getTopScopes() {
        return topScopes;
    }

    /**
     * Adds all builtin functions to the {@link LLFunctionRegistry}. This method lists all
     * {@link LLBuiltinNode builtin implementation classes}.
     */
    private void installBuiltins() {
        LLObjectUtil.putProperty(topContext, "null", LLNull.INSTANCE);
        LLObjectUtil.putProperty(topContext, "true", true);
        LLObjectUtil.putProperty(topContext, "false", false);
        LLObjectUtil.putProperty(topContext, "Object", objectPrototype);
        LLObjectUtil.putProperty(topContext, "Null", nullPrototype);
        LLObjectUtil.putProperty(topContext, "Boolean", booleanPrototype);
        LLObjectUtil.putProperty(topContext, "True", truePrototype);
        LLObjectUtil.putProperty(topContext, "False", falsePrototype);
        LLObjectUtil.putProperty(topContext, "Number", numberPrototype);
        LLObjectUtil.putProperty(topContext, "Integer", integerPrototype);
        LLObjectUtil.putProperty(topContext, "BigInteger", bigIntegerPrototype);
        LLObjectUtil.putProperty(topContext, "Float", floatPrototype);
        LLObjectUtil.putProperty(topContext, "String", stringPrototype);
        LLObjectUtil.putProperty(topContext, "Number", numberPrototype);

        installBuiltin(LLReadlnBuiltinFactory.getInstance());
        installBuiltin(LLPrintlnBuiltinFactory.getInstance());
        installBuiltin(LLNanoTimeBuiltinFactory.getInstance());
        installBuiltin(LLDefineFunctionBuiltinFactory.getInstance());
        installBuiltin(LLStackTraceBuiltinFactory.getInstance());
        installBuiltin(LLHelloEqualsWorldBuiltinFactory.getInstance());
        installBuiltin(LLNewObjectBuiltinFactory.getInstance());
        installBuiltin(LLEvalBuiltinFactory.getInstance());
        installBuiltin(LLImportBuiltinFactory.getInstance());
        installBuiltin(LLGetSizeBuiltinFactory.getInstance());
        installBuiltin(LLHasSizeBuiltinFactory.getInstance());
        installBuiltin(LLIsExecutableBuiltinFactory.getInstance());
        installBuiltin(LLIsNullBuiltinFactory.getInstance());
        installBuiltin(LLWrapPrimitiveBuiltinFactory.getInstance());
        installBuiltin(LLTypeOfBuiltinFactory.getInstance());
        installBuiltin(LLIsInstanceBuiltinFactory.getInstance());
    }

    public void installBuiltin(NodeFactory<? extends LLBuiltinNode> factory) {
        /*
         * The builtin node factory is a class that is automatically generated by the Truffle DLL.
         * The signature returned by the factory reflects the signature of the @Specialization
         *
         * methods in the builtin classes.
         */
        int argumentCount = factory.getExecutionSignature().size();
        LLExpressionNode[] argumentNodes = new LLExpressionNode[argumentCount];
        /*
         * Builtin functions are like normal functions, i.e., the arguments are passed in as an
         * Object[] array encapsulated in LLArguments. A LLReadArgumentNode extracts a parameter
         * from this array.
         */
        for (int i = 0; i < argumentCount; i++) {
            argumentNodes[i] = new LLReadArgumentNode(i);
        }
        /* Instantiate the builtin node. This node performs the actual functionality. */
        LLBuiltinNode builtinBodyNode = factory.createNode((Object) argumentNodes);
        builtinBodyNode.addRootTag();
        /* The name of the builtin function is specified via an annotation on the node class. */
        String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();
        builtinBodyNode.setUnavailableSourceSection();

        /* Wrap the builtin in a RootNode. Truffle requires all AST to start with a RootNode. */
        LLRootNode rootNode = new LLRootNode(language, new FrameDescriptor(), builtinBodyNode, BUILTIN_SOURCE.createUnavailableSection(), name);
        RootCallTarget rootCallTarget = Truffle.getRuntime().createCallTarget(rootNode);
        LLFunction rootFunction = createFunction(name, rootCallTarget);
        LLObjectUtil.putProperty(objectPrototype, name, rootFunction);
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

    public LLObject getPrototype(Object obj) {
        if(obj instanceof LLObject) {
            return ((LLObject)obj).getPrototype();
        } else if (obj instanceof String) {
            return stringPrototype;
        } else if (obj instanceof LLBigInteger) {
            return bigIntegerPrototype;
        } else if (obj == LLNull.INSTANCE) {
            return nullPrototype;
        } else if (obj.equals(true)) {
            return truePrototype;
        } else if (obj.equals(false)) {
            return falsePrototype;
        } else {
            throw new NotImplementedException();
        }
    }

    public Object getFunction(Object obj, Object name) {
        LLObject object;
        if(obj instanceof LLObject) {
            object = (LLObject)obj;
        } else {
            object = getPrototype(obj);
        }
        if(LLObjectUtil.hasProperty(object, name)) {
            return LLObjectUtil.getProperty(object, name);
        }
        Object parent = object.getPrototype();
        if(parent != null) {
            return getFunction(parent, name);
        }
        throw new NotImplementedException();
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
        if (a instanceof Long || a instanceof LLBigInteger || a instanceof String || a instanceof Boolean) {
            return a;
        } else if (a instanceof Character) {
            return fromForeignCharacter((Character) a);
        } else if (a instanceof Number) {
            return fromForeignNumber(a);
        } else if (a instanceof TruffleObject) {
            return a;
        } else if (a instanceof LLContext) {
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

    public static LLContext getCurrent() {
        return LLLanguage.getCurrentContext();
    }

}
