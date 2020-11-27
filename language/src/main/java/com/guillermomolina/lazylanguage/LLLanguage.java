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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.guillermomolina.lazylanguage.builtins.LLBuiltinNode;
import com.guillermomolina.lazylanguage.nodes.LLEvalRootNode;
import com.guillermomolina.lazylanguage.nodes.local.LLLexicalScope;
import com.guillermomolina.lazylanguage.parser.LLParserVisitor;
import com.guillermomolina.lazylanguage.runtime.LLContext;
import com.guillermomolina.lazylanguage.runtime.LLFunction;
import com.guillermomolina.lazylanguage.runtime.LLLanguageView;
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
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(id = LLLanguage.ID, name = "Lazy", defaultMimeType = LLLanguage.MIME_TYPE, characterMimeTypes = LLLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LLFileDetector.class)
@ProvidedTags({ StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class,
        StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class })
public final class LLLanguage extends TruffleLanguage<LLContext> {
    public static final AtomicInteger counter = new AtomicInteger();

    public static final String ID = "lazy";
    public static final String MIME_TYPE = "application/x-lazy";

    private final Shape rootShape;

    public LLLanguage() {
        counter.incrementAndGet();
        this.rootShape = Shape.newBuilder().layout(LLObject.class).addConstantProperty(LLObject.PROTOTYPE, null, 0).build();
    }

    @Override
    protected LLContext createContext(Env env) {
        return new LLContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();

        LLParserVisitor visitor;
        if (request.getArgumentNames().isEmpty()) {
            visitor = new LLParserVisitor(this, source);
        } else {
            throw new NotImplementedException();
        }
        RootNode evalMain = new LLEvalRootNode(this, visitor.parse());
        return Truffle.getRuntime().createCallTarget(evalMain);        
    }

    @Override
    protected Object getLanguageView(LLContext context, Object value) {
        return LLLanguageView.create(value);
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
                        Scope vscope = Scope.newBuilder(nextScope.getName(), nextScope.getVariables(frame))
                                .node(nextScope.getNode()).arguments(nextScope.getArguments(frame))
                                .rootInstance(functionObject).build();
                        previousScope = nextScope;
                        nextScope = null;
                        return vscope;
                    }

                    private Object findFunctionObject() {
                        throw new NotImplementedException();
                        /*
                         * String name = node.getRootNode().getName(); return
                         * context.getTopContext().getFunction(name);
                         */
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

    public LLObject createObject() {
        return createObject(getCurrentContext().getAllocationReporter());
    }

    /**
     * Allocate an empty object. All new objects initially have no properties.
     * Properties are added when they are first stored, i.e., the store triggers a
     * shape change of the object.
     */
    public LLObject createObject(AllocationReporter reporter) {
        reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LLObject object = new LLObject(rootShape);
        reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        return object;
    }

    public LLFunction createFunction(AllocationReporter reporter, String name, RootCallTarget callTarget) {
        reporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
        LLFunction object = new LLFunction(rootShape, this, name, callTarget);
        reporter.onReturnValue(object, 0, AllocationReporter.SIZE_UNKNOWN);
        return object;
    }

    public static LLContext getCurrentContext() {
        return getCurrentContext(LLLanguage.class);
    }

    private static final List<NodeFactory<? extends LLBuiltinNode>> EXTERNAL_BUILTINS = Collections
            .synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends LLBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

}