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
package com.guillermomolina.lazyscript.nodes;

import com.guillermomolina.lazyscript.LSLanguage;
import com.guillermomolina.lazyscript.runtime.LSContext;
import com.guillermomolina.lazyscript.runtime.LSObjectUtil;
import com.guillermomolina.lazyscript.runtime.objects.LSFunction;
import com.guillermomolina.lazyscript.runtime.objects.LSNull;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * This class performs two additional tasks:
 *
 * <ul>
 * <li>Lazily registration of functions on first execution. This fulfills the
 * semantics of "evaluating" source code in LazyScript.</li>
 * <li>Conversion of arguments to types understood by LazyScript. The LazyScript
 * source code can be evaluated from a different language, i.e., the caller can
 * be a node from a different language that uses types not understood by
 * LazyScript.</li>
 * </ul>
 */
public final class LSEvalRootNode extends RootNode {
    @Child
    private LSExpressionNode functionNode;
    @Child
    private InteropLibrary library;

    public LSEvalRootNode(LSLanguage language, LSExpressionNode functionNode) {
        super(language);
        this.functionNode = functionNode;
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    public String getName() {
        return "root eval";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (mainCallNode == null) {
            /* The source code did not have a "main" function, so nothing to execute. */
            return LSNull.INSTANCE;
        }

        final String name = "main";
        final LSContext context = lookupContextReference(LSLanguage.class).get();
        final LSFunction mainFunction = context.createFunction(name, rootCallTarget);
        LSObjectUtil.putProperty(context.getTopContext(), name, mainFunction);

        /* Conversion of arguments to types understood by LazyScript. */
        Object[] frameArguments = frame.getArguments();
        Object[] arguments = new Object[frameArguments.length + 2];
        arguments[0] = mainFunction;
        arguments[1] = lookupContextReference(LSLanguage.class).get().getTopContext();
        for (int i = 0; i < frameArguments.length; i++) {
            arguments[i + 1] = LSContext.fromForeignValue(frameArguments[i]);
        }
        return mainCallNode.call(arguments);
    }
}
