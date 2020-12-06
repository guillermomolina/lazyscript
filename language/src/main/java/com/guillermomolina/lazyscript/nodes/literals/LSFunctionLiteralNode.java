/*
 * Copyright (c) 2012, 2018, Guillermo Adrián Molina. All rights reserved.
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
package com.guillermomolina.lazyscript.nodes.literals;

import com.guillermomolina.lazyscript.LSLanguage;
import com.guillermomolina.lazyscript.nodes.LSExpressionNode;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.objects.LSFunction;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;


/**
 * Constant literal for a {@link LSFunction function} value, created when a function name occurs as
 * a literal in LS source code. Note that function redefinition can change the {@link CallTarget
 * call target} that is executed when calling the function, but the {@link LSFunction} for a name
 * never changes. This is guaranteed by the {@link LSFunctionRegistry}.
 */
@NodeInfo(shortName = "function")
public final class LSFunctionLiteralNode extends LSExpressionNode {

    @CompilationFinal private boolean scopeSet = false;

    /** The name of the function. */
    private final String functionName;

    private final RootCallTarget callTarget;

    /**
     * The resolved function. During parsing (in the constructor of this node), we do not have the
     * {@link LSContext} available yet, so the lookup can only be done at {@link #executeGeneric
     * first execution}. The {@link CompilationFinal} annotation ensures that the function can still
     * be constant folded during compilation.
     */
    @CompilationFinal private LSFunction cachedFunction;

    /**
     * The stored context reference. Caching the context reference in a field like this always
     * ensures the most efficient context lookup. The {@link LSContext} must not be stored in the
     * AST in the multi-context case.
     */
    @CompilationFinal private ContextReference<LSContext> contextRef;

    /**
     * It is always safe to store the language in the AST if the language supports
     * {@link ContextPolicy#SHARED shared}.
     */
    @CompilationFinal private LSLanguage language;

    public LSFunctionLiteralNode(String functionName, final RootCallTarget callTarget) {
        this.functionName = functionName;
        this.callTarget = callTarget;
    }

    @Override
    public LSFunction executeGeneric(VirtualFrame frame) {
        ContextReference<LSContext> contextReference = contextRef;
        if (contextReference == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextReference = contextRef = lookupContextReference(LSLanguage.class);
        }
        LSLanguage l = language;
        if (l == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            l = language = lookupLanguageReference(LSLanguage.class).get();
        }
        CompilerAsserts.partialEvaluationConstant(l);

        LSFunction function;
        if (l.isSingleContext()) {
            function = this.cachedFunction;
            if (function == null) {
                /* We are about to change a @CompilationFinal field. */
                CompilerDirectives.transferToInterpreterAndInvalidate();
                /* First execution of the node: lookup the function in the function registry. */
                this.cachedFunction = function = getContext().createFunction(functionName, callTarget);
            }
        } else {
            /*
             * We need to rest the cached function otherwise it might cause a memory leak.
             */
            if (this.cachedFunction != null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.cachedFunction = null;
            }
            // in the multi-context case we are not allowed to store
            // LSFunction objects in the AST. Instead we always perform the lookup in the hash map.
            function = getContext().createFunction(functionName, callTarget);
        }
        if (!isScopeSet()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            function.setLexicalScope(frame.materialize());
            this.scopeSet = true;
        }
        return function;
    }

    protected boolean isScopeSet() {
        return this.scopeSet;
    }
}
