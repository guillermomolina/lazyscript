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
package com.guillermomolina.lazyscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.guillermomolina.lazyscript.builtins.LSBuiltinNode;
import com.guillermomolina.lazyscript.nodes.LSEvalRootNode;
import com.guillermomolina.lazyscript.nodes.local.LSLexicalScope;
import com.guillermomolina.lazyscript.parser.LSParserVisitor;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.LSLanguageView;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(id = LazyScriptLanguage.ID, name = LazyScriptLanguage.NAME, defaultMimeType = LazyScriptLanguage.MIME_TYPE, characterMimeTypes = LazyScriptLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LSFileDetector.class)
@ProvidedTags({ StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class,
        StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class })
public final class LazyScriptLanguage extends TruffleLanguage<LSContext> {
    public static final AtomicInteger counter = new AtomicInteger();

    public static final String ID = "ls";
    public static final String NAME = "LazyScript";
    public static final String MIME_TYPE = "application/x-lazyscript";

    public LazyScriptLanguage() {
        counter.incrementAndGet();
    }

    @Override
    protected LSContext createContext(Env env) {
        return new LSContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();

        LSParserVisitor visitor;
        if (request.getArgumentNames().isEmpty()) {
            visitor = new LSParserVisitor(this, source);
        } else {
            throw new NotImplementedException();
        }
        RootNode evalMain = new LSEvalRootNode(this, visitor.parse());
        return Truffle.getRuntime().createCallTarget(evalMain);        
    }

    @Override
    protected Object getLanguageView(LSContext context, Object value) {
        return LSLanguageView.create(value);
    }

    @Override
    protected boolean isVisible(LSContext context, Object value) {
        return !InteropLibrary.getFactory().getUncached(value).isNull(value);
    }

    @Override
    public Iterable<Scope> findLocalScopes(LSContext context, Node node, Frame frame) {
        final LSLexicalScope scope = LSLexicalScope.createScope(node);
        return new Iterable<Scope>() {
            @Override
            public Iterator<Scope> iterator() {
                return new Iterator<Scope>() {
                    private LSLexicalScope previousScope;
                    private LSLexicalScope nextScope = scope;

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
    protected Iterable<Scope> findTopScopes(LSContext context) {
        return context.getTopScopes();
    }

    public static LSContext getCurrentContext() {
        return getCurrentContext(LazyScriptLanguage.class);
    }

    private static final List<NodeFactory<? extends LSBuiltinNode>> EXTERNAL_BUILTINS = Collections
            .synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends LSBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

}