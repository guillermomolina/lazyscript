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
package com.guillermomolina.lazyscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.guillermomolina.lazyscript.builtins.LSBuiltinNode;
import com.guillermomolina.lazyscript.nodes.LSEvalRootNode;
import com.guillermomolina.lazyscript.parser.LSParserVisitor;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.LSObjectUtil;
import com.guillermomolina.lazyscript.runtime.LSScopeUtil;
import com.guillermomolina.lazyscript.runtime.interop.LSLanguageView;
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
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(id = LSLanguage.ID, name = LSLanguage.NAME, defaultMimeType = LSLanguage.MIME_TYPE, characterMimeTypes = LSLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LSFileDetector.class)
@ProvidedTags({ StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class,
        StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class,
        StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class })
public final class LSLanguage extends TruffleLanguage<LSContext> {
    public static final AtomicInteger counter = new AtomicInteger();

    public static final String ID = "ls";
    public static final String NAME = "LazyScript";
    public static final String MIME_TYPE = "application/x-lazyscript";
    private static final Source BUILTIN_SOURCE = Source.newBuilder(SLLanguage.ID, "", "SL builtin").build();

    private final Assumption singleContext = Truffle.getRuntime().createAssumption("Single SL context.");

    private final Map<NodeFactory<? extends SLBuiltinNode>, RootCallTarget> builtinTargets = new ConcurrentHashMap<>();
    private final Map<String, RootCallTarget> undefinedFunctions = new ConcurrentHashMap<>();

    public LSLanguage() {
        counter.incrementAndGet();
    }

    @Override
    protected LSContext createContext(Env env) {
        return new LSContext(this, env);
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
    /**
     * SLLanguage specifies the {@link ContextPolicy#SHARED} in
     * {@link Registration#contextPolicy()}. This means that a single {@link TruffleLanguage}
     * instance can be reused for multiple language contexts. Before this happens the Truffle
     * framework notifies the language by invoking {@link #initializeMultipleContexts()}. This
     * allows the language to invalidate certain assumptions taken for the single context case. One
     * assumption SL takes for single context case is located in {@link SLEvalRootNode}. There
     * functions are only tried to be registered once in the single context case, but produce a
     * boundary call in the multi context case, as function registration is expected to happen more
     * than once.
     *
     * Value identity caches should be avoided and invalidated for the multiple contexts case as no
     * value will be the same. Instead, in multi context case, a language should only use types,
     * shapes and code to speculate.
     *
     * For a new language it is recommended to start with {@link ContextPolicy#EXCLUSIVE} and as the
     * language gets more mature switch to {@link ContextPolicy#SHARED}.
     */
    @Override
    protected void initializeMultipleContexts() {
        singleContext.invalidate();
    }

    public boolean isSingleContext() {
        return singleContext.isValid();
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
    protected Object getScope(LSContext context) {
        return context.getFunctionRegistry().getFunctionsObject();
    }

    public static LSContext getCurrentContext() {
        return getCurrentContext(LSLanguage.class);
    }

    private static final List<NodeFactory<? extends LSBuiltinNode>> EXTERNAL_BUILTINS = Collections
            .synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends LSBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

}