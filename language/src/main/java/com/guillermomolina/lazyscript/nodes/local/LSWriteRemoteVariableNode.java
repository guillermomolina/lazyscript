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
package com.guillermomolina.lazyscript.nodes.local;

import com.guillermomolina.lazyscript.nodes.LSExpressionNode;
import com.guillermomolina.lazyscript.nodes.interop.NodeObjectDescriptor;
import com.guillermomolina.lazyscript.runtime.objects.LSFunction;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.WriteVariableTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Node to read a remote variable from a function's {@link VirtualFrame frame}.
 */
@NodeChild("valueNode")
@NodeField(name = "slot", type = FrameSlot.class)
@NodeField(name = "nameNode", type = LSExpressionNode.class)
@NodeField(name = "depth", type = int.class)
public abstract class LSWriteRemoteVariableNode extends LSExpressionNode {

    protected abstract FrameSlot getSlot();
    
    /**
     * Returns the child node <code>nameNode</code>. The implementation of this method is created by
     * the Truffle DLL based on the {@link NodeChild} annotation on the class.
     */
    public abstract LSExpressionNode getNameNode();

    public abstract int getDepth();

    public interface FrameSet<T> {
        void set(Frame frame, FrameSlot slot, T value);
    }

    @ExplodeLoop
    public <T> T writeUpStack(FrameSlotKind slotKind, FrameSet<T> setter, Frame frame, T value)
            throws FrameSlotTypeException {

        Frame lookupFrame = frame;
        for (int i = 0; i < this.getDepth(); i++) {
            LSFunction function = (LSFunction)lookupFrame.getArguments()[0];
            assert function instanceof LSFunction;
            lookupFrame = function.getEnclosingFrame();
        }

        final FrameSlot slot = this.getSlot();
        final FrameSlotKind kind = lookupFrame.getFrameDescriptor().getFrameSlotKind(slot);
        if(kind != slotKind && kind != FrameSlotKind.Illegal) {
            throw new FrameSlotTypeException();
        }
        
        frame.getFrameDescriptor().setFrameSlotKind(slot, slotKind);

        setter.set(lookupFrame, slot, value);
        return value;
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected long writeLong(VirtualFrame virtualFrame, long value)
            throws FrameSlotTypeException {
        return this.writeUpStack(FrameSlotKind.Long, Frame::setLong, virtualFrame, value);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected Double writeDouble(VirtualFrame virtualFrame, Double value)
            throws FrameSlotTypeException {
        return this.writeUpStack(FrameSlotKind.Double, Frame::setDouble, virtualFrame, value);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected Boolean writeBoolean(VirtualFrame virtualFrame, Boolean value)
            throws FrameSlotTypeException {
        return this.writeUpStack(FrameSlotKind.Boolean, Frame::setBoolean, virtualFrame, value);
    }

    /**
     * Generic write method that works for all possible types.
     * <p>
     * Why is this method annotated with {@link Specialization} and not {@link Fallback}? For a
     * {@link Fallback} method, the Truffle DLL generated code would try all other specializations
     * first before calling this method. We know that all these specializations would fail their
     * guards, so there is no point in calling them. Since this method takes a value of type
     * {@link Object}, it is guaranteed to never fail, i.e., once we are in this specialization the
     * node will never be re-specialized.
     */
    @Specialization(replaces = {"writeLong", "writeDouble", "writeBoolean"})
    protected Object write(VirtualFrame frame, Object value) {
        /*
         * Regardless of the type before, the new and final type of the local variable is Object.
         * Changing the slot kind also discards compiled code, because the variable type is
         * important when the compiler optimizes a method.
         *
         * No-op if kind is already Object.
         */
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);

        frame.setObject(getSlot(), value);
        return value;
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == WriteVariableTag.class || super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        LSExpressionNode nameNode = getNameNode();
        SourceSection nameSourceSection;
        if (nameNode.getSourceCharIndex() == -1) {
            nameSourceSection = null;
        } else {
            SourceSection rootSourceSection = getRootNode().getSourceSection();
            if (rootSourceSection == null) {
                nameSourceSection = null;
            } else {
                Source source = rootSourceSection.getSource();
                nameSourceSection = source.createSection(nameNode.getSourceCharIndex(), nameNode.getSourceLength());
            }
        }
        return NodeObjectDescriptor.writeVariable(getSlot().getIdentifier().toString(), nameSourceSection);
    }
}
