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
package com.guillermomolina.lazylanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.guillermomolina.lazylanguage.builtins.LLBuiltinNode;
import com.guillermomolina.lazylanguage.builtins.LLDefineFunctionBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLNanoTimeBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLPrintlnBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLReadlnBuiltin;
import com.guillermomolina.lazylanguage.builtins.LLStackTraceBuiltin;
import com.guillermomolina.lazylanguage.nodes.LLEvalRootNode;
import com.guillermomolina.lazylanguage.nodes.LLTypes;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLBlockNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLBreakNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLContinueNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLDebuggerNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLIfNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLReturnNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLWhileNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLAddNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLBigIntegerLiteralNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLDivNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLEqualNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLFunctionLiteralNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLInvokeNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLessOrEqualNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLessThanNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLogicalAndNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLogicalOrNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLMulNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLReadPropertyNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLStringLiteralNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLSubNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLWritePropertyNode;
import com.guillermomolina.lazylanguage.nodes.local.LLLexicalScope;
import com.guillermomolina.lazylanguage.nodes.local.LLReadLocalVariableNode;
import com.guillermomolina.lazylanguage.nodes.local.LLWriteLocalVariableNode;
import com.guillermomolina.lazylanguage.parser.LLNodeFactory;
import com.guillermomolina.lazylanguage.parser.LLParser;
import com.guillermomolina.lazylanguage.parser.LazyLanguageLexer;
import com.guillermomolina.lazylanguage.parser.LazyLanguageParser;
import com.guillermomolina.lazylanguage.runtime.LLBigNumber;
import com.guillermomolina.lazylanguage.runtime.LLContext;
import com.guillermomolina.lazylanguage.runtime.LLFunction;
import com.guillermomolina.lazylanguage.runtime.LLFunctionRegistry;
import com.guillermomolina.lazylanguage.runtime.LLLanguageView;
import com.guillermomolina.lazylanguage.runtime.LLNull;
import com.guillermomolina.lazylanguage.runtime.LLObject;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;

/**
 * Lazy is a lazy language to demonstrate and showcase features of Truffle. The implementation is as
 * lazy and clean as possible in order to help understanding the ideas and concepts of Truffle.
 * The language has first class functions, and objects are key-value stores.
 * <p>
 * Lazy is dynamically typed, i.e., there are no type names specified by the programmer. Lazy is
 * strongly typed, i.e., there is no automatic conversion between types. If an operation is not
 * available for the types encountered at run time, a type error is reported and execution is
 * stopped. For example, {@code 4 - "2"} results in a type error because subtraction is only defined
 * for numbers.
 *
 * <p>
 * <b>Types:</b>
 * <ul>
 * <li>Number: arbitrary precision integer numbers. The implementation uses the Java primitive type
 * {@code long} to represent numbers that fit into the 64 bit range, and {@link LLBigNumber} for
 * numbers that exceed the range. Using a primitive type such as {@code long} is crucial for
 * performance.
 * <li>Boolean: implemented as the Java primitive type {@code boolean}.
 * <li>String: implemented as the Java standard type {@link String}.
 * <li>Function: implementation type {@link LLFunction}.
 * <li>Object: efficient implementation using the object model provided by Truffle. The
 * implementation type of objects is a subclass of {@link DynamicObject}.
 * <li>Null (with only one value {@code null}): implemented as the singleton
 * {@link LLNull#SINGLETON}.
 * </ul>
 * The class {@link LLTypes} lists these types for the Truffle DLL, i.e., for type-specialized
 * operations that are specified using Truffle DLL annotations.
 *
 * <p>
 * <b>Language concepts:</b>
 * <ul>
 * <li>Literals for {@link LLBigIntegerLiteralNode numbers} , {@link LLStringLiteralNode strings},
 * and {@link LLFunctionLiteralNode functions}.
 * <li>Basic arithmetic, logical, and comparison operations: {@link LLAddNode +}, {@link LLSubNode
 * -}, {@link LLMulNode *}, {@link LLDivNode /}, {@link LLLogicalAndNode logical and},
 * {@link LLLogicalOrNode logical or}, {@link LLEqualNode ==}, !=, {@link LLLessThanNode &lt;},
 * {@link LLLessOrEqualNode &le;}, &gt;, &ge;.
 * <li>Local variables: local variables must be defined (via a {@link LLWriteLocalVariableNode
 * write}) before they can be used (by a {@link LLReadLocalVariableNode read}). Local variables are
 * not visible outside of the block where they were first defined.
 * <li>Basic control flow statements: {@link LLBlockNode blocks}, {@link LLIfNode if},
 * {@link LLWhileNode while} with {@link LLBreakNode break} and {@link LLContinueNode continue},
 * {@link LLReturnNode return}.
 * <li>Debugging control: {@link LLDebuggerNode debugger} statement uses
 * {@link DebuggerTags#AlwaysHalt} tag to halt the execution when run under the debugger.
 * <li>Function calls: {@link LLInvokeNode invocations} are efficiently implemented with
 * {@link LLDispatchNode polymorphic inline caches}.
 * <li>Object access: {@link LLReadPropertyNode} and {@link LLWritePropertyNode} use a cached
 * {@link DynamicObjectLibrary} as the polymorphic inline cache for property reads and writes,
 * respectively.
 * </ul>
 *
 * <p>
 * <b>Syntax and parsing:</b><br>
 * The syntax is described as an attributed grammar. The {@link LazyLanguageParser} and
 * {@link LazyLanguageLexer} are automatically generated by ANTLR 4. The grammar contains semantic
 * actions that build the AST for a method. To keep these semantic actions short, they are mostly
 * calls to the {@link LLNodeFactory} that performs the actual node creation. All functions found in
 * the Lazy source are added to the {@link LLFunctionRegistry}, which is accessible from the
 * {@link LLContext}.
 *
 * <p>
 * <b>Builtin functions:</b><br>
 * Library functions that are available to every Lazy source without prior definition are called
 * builtin functions. They are added to the {@link LLFunctionRegistry} when the {@link LLContext} is
 * created. Some of the current builtin functions are
 * <ul>
 * <li>{@link LLReadlnBuiltin readln}: Read a String from the {@link LLContext#getInput() standard
 * input}.
 * <li>{@link LLPrintlnBuiltin println}: Write a value to the {@link LLContext#getOutput() standard
 * output}.
 * <li>{@link LLNanoTimeBuiltin nanoTime}: Returns the value of a high-resolution time, in
 * nanoseconds.
 * <li>{@link LLDefineFunctionBuiltin defineFunction}: Parses the functions provided as a String
 * argument and adds them to the function registry. Functions that are already defined are replaced
 * with the new version.
 * <li>{@link LLStackTraceBuiltin stckTrace}: Print all function activations with all local
 * variables.
 * </ul>
 */
@TruffleLanguage.Registration(id = LLLanguage.ID, name = "Lazy", defaultMimeType = LLLanguage.MIME_TYPE, characterMimeTypes = LLLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LLFileDetector.class)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
                StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class})
public final class LLLanguage extends TruffleLanguage<LLContext> {
    public static final AtomicInteger counter = new AtomicInteger();

    public static final String ID = "lazy";
    public static final String MIME_TYPE = "application/x-lazy";

    private final Shape rootShape;

    public LLLanguage() {
        counter.incrementAndGet();
        this.rootShape = Shape.newBuilder().layout(LLObject.class).build();
    }

    @Override
    protected LLContext createContext(Env env) {
        return new LLContext(this, env, new ArrayList<>(EXTERNAL_BUILTINS));
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        Map<String, RootCallTarget> functions;
        /*
         * Parse the provided source. At this point, we do not have a LLContext yet. Registration of
         * the functions with the LLContext happens lazily in LLEvalRootNode.
         */
        if (request.getArgumentNames().isEmpty()) {
            LLParser parser = new LLParser(this, source);
            functions = parser.getAllFunctions();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("function main(");
            String sep = "";
            for (String argumentName : request.getArgumentNames()) {
                sb.append(sep);
                sb.append(argumentName);
                sep = ",";
            }
            sb.append(") { return ");
            sb.append(source.getCharacters());
            sb.append(";}");
            String language = source.getLanguage() == null ? ID : source.getLanguage();
            Source decoratedSource = Source.newBuilder(language, sb.toString(), source.getName()).build();
            LLParser parser = new LLParser(this, decoratedSource);
            functions = parser.getAllFunctions();
        }

        String functionName = source.getName();
        final int extensionIndex = functionName.lastIndexOf(".");
        if (extensionIndex != -1) {
            functionName = functionName.substring(0, extensionIndex);
        }
        RootCallTarget main = functions.get(functionName);
        RootNode evalMain;
        if (main != null) {
            /*
             * We have a main function, so "evaluating" the parsed source means invoking that main
             * function. However, we need to lazily register functions into the LLContext first, so
             * we cannot use the original LLRootNode for the main function. Instead, we create a new
             * LLEvalRootNode that does everything we need.
             */
            evalMain = new LLEvalRootNode(this, main, functions);
        } else {
            /*
             * Even without a main function, "evaluating" the parsed source needs to register the
             * functions into the LLContext.
             */
            evalMain = new LLEvalRootNode(this, null, functions);
        }
        return Truffle.getRuntime().createCallTarget(evalMain);
    }

    @Override
    protected Object getLanguageView(LLContext context, Object value) {
        return LLLanguageView.create(value);
    }

    /*
     * Still necessary for the old Lazy TCK to pass. We should remove with the old TCK. New language
     * should not override this.
     */
    @Override
    protected Object findExportedSymbol(LLContext context, String globalName, boolean onlyExplicit) {
        return context.getFunctionRegistry().lookup(globalName, false);
    }

    @Override
    protected boolean isVisible(LLContext context, Object value) {
        return !InteropLibrary.getFactory().getUncached(value).isNull(value);
    }

    @Override
    public Iterable<Scope> findLocalScopes(LLContext context, Node node, Frame frame) {
        final LLLexicalScope scope = LLLexicalScope.createScope(node);
        return new Iterable<Scope>() {
            @Override
            public Iterator<Scope> iterator() {
                return new Iterator<Scope>() {
                    private LLLexicalScope previousScope;
                    private LLLexicalScope nextScope = scope;

                    @Override
                    public boolean hasNext() {
                        if (nextScope == null) {
                            nextScope = previousScope.findParent();
                        }
                        return nextScope != null;
                    }

                    @Override
                    public Scope next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Object functionObject = findFunctionObject();
                        Scope vscope = Scope.newBuilder(nextScope.getName(), nextScope.getVariables(frame)).node(nextScope.getNode()).arguments(nextScope.getArguments(frame)).rootInstance(
                                        functionObject).build();
                        previousScope = nextScope;
                        nextScope = null;
                        return vscope;
                    }

                    private Object findFunctionObject() {
                        String name = node.getRootNode().getName();
                        return context.getFunctionRegistry().getFunction(name);
                    }
                };
            }
        };
    }

    @Override
    protected Iterable<Scope> findTopScopes(LLContext context) {
        return context.getTopScopes();
    }

    public Shape getRootShape() {
        return rootShape;
    }

    /**
     * Allocate an empty object. All new objects initially have no properties. Properties are added
     * when they are first stored, i.e., the store triggers a shape change of the object.
     */
    public LLObject createObject(AllocationReporter reporter) {
        reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LLObject object = new LLObject(rootShape);
        reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        return object;
    }

    public static LLContext getCurrentContext() {
        return getCurrentContext(LLLanguage.class);
    }

    private static final List<NodeFactory<? extends LLBuiltinNode>> EXTERNAL_BUILTINS = Collections.synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends LLBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

}