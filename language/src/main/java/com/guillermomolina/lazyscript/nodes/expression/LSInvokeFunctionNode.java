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
package com.guillermomolina.lazyscript.nodes.expression;

import com.guillermomolina.lazyscript.runtime.LSUndefinedNameException;
import com.guillermomolina.lazyscript.runtime.objects.LSFunction;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * The node for function invocation in LazyScript. Since LazyScript has first
 * class functions, the {@link LSFunction target function} can be computed by an
 * arbitrary expression. This node is responsible for evaluating this
 * expression, as well as evaluating the {@link #argumentNodes arguments}. The
 * actual invocation is delegated to a {@link InteropLibrary} instance.
 *
 * @see InteropLibrary#execute(Object, Object...)
 */
@NodeInfo(shortName = "invoke")
public final class LSInvokeFunctionNode extends LSExpressionNode {

    @Child
    private LSExpressionNode receiver;
    @Child
    private LSExpressionNode functionNode;
    @Children
    private final LSExpressionNode[] argumentNodes;
    @Child
    private InteropLibrary library;

    public LSInvokeFunctionNode(LSExpressionNode receiver, LSExpressionNode functionNode,
            LSExpressionNode[] argumentNodes) {
        this.receiver = receiver;
        this.functionNode = functionNode;
        this.argumentNodes = argumentNodes;
        this.library = InteropLibrary.getFactory().createDispatched(3);
    }

    @ExplodeLoop
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        /*
         * The number of arguments is constant for one invoke node. During compilation,
         * the loop is unrolled and the execute methods of all arguments are inlined.
         * This is triggered by the ExplodeLoop annotation on the method. The compiler
         * assertion below illustrates that the array length is really constant.
         */
        CompilerAsserts.compilationConstant(argumentNodes.length);

        Object[] argumentValues = new Object[argumentNodes.length + 1];
        argumentValues[0] = receiver.executeGeneric(frame);
        for (int i = 0; i < argumentNodes.length; i++) {
            argumentValues[i + 1] = argumentNodes[i].executeGeneric(frame);
        }

        Object function = functionNode.executeGeneric(frame);
        if (!(function instanceof LSFunction)) {
            throw LSUndefinedNameException.undefinedFunction(this, "unnamed");
        }

        try {
            return library.execute(function, argumentValues);
        } catch (ArityException | UnsupportedTypeException | UnsupportedMessageException e) {
            throw LSUndefinedNameException.undefinedFunction(this, ((LSFunction) function).getName());
        }
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.CallTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

}