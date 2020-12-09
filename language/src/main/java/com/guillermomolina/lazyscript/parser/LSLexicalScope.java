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
package com.guillermomolina.lazyscript.parser;

import java.util.logging.Level;

import com.guillermomolina.lazyscript.LSLanguage;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import org.antlr.v4.runtime.misc.Pair;

public class LSLexicalScope {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(LSLanguage.ID, LSLexicalScope.class);

    public static final String THIS = "this";

    public static final int LEVEL_UNDEFINED = -1;

    private final LSLexicalScope outer;
    private final FrameDescriptor frameDescriptor;
    private int parameterCount;
    private final boolean inLoop;

    LSLexicalScope(LSLexicalScope outer, boolean inLoop) {
        this.outer = outer;
        this.inLoop = inLoop;
        this.parameterCount = 0;
        if (inLoop) {
            this.frameDescriptor = outer.frameDescriptor;
        } else {
            this.frameDescriptor = new FrameDescriptor();
        }
    }

    public LSLexicalScope getOuter() {
        return outer;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    private FrameSlot getLocalVariable(final String name) {
        return frameDescriptor.findFrameSlot(name);
    }

    public FrameSlot getThisVariable() {
        return getLocalVariable(THIS);
    }

    public boolean hasLocalVariable(final String name) {
        return getLocalVariable(name) != null;
    }

    public FrameSlot findOrAddVariable(final String name) {
        FrameSlot frameSlot = getLocalVariable(name);
        if (frameSlot == null) {
            LOG.log(Level.FINE, "Adding local variable named: {0}", name);
            frameSlot = frameDescriptor.addFrameSlot(name, FrameSlotKind.Illegal);
        }
        return frameSlot;
    }

    public FrameSlot addVariable(final String name) {
        FrameSlot frameSlot = getLocalVariable(name);
        if (frameSlot != null) {
            throw new UnsupportedOperationException("Variable named: " + name + " already defined");
        }
        LOG.log(Level.FINE, "Adding local variable named: {0}", name);
        frameSlot = frameDescriptor.addFrameSlot(name, FrameSlotKind.Illegal);
        return frameSlot;
    }

    public FrameSlot addParameter(final String name) {
        if (parameterCount != 0 && name.equals(THIS)) {
            throw new UnsupportedOperationException("\"this\" must always be the first parameter");
        }
        if (hasLocalVariable(name)) {
            throw new UnsupportedOperationException("Parameter named: " + name + " already defined");
        }
        LOG.log(Level.FINE, "Adding parameter index: " + parameterCount + " named: " + name);
        return frameDescriptor.addFrameSlot(name, parameterCount++, FrameSlotKind.Illegal);
    }

    public Pair<Integer, FrameSlot> getVariable(String name) {
        int depth = 0;
        LSLexicalScope current = this;
        FrameSlot frameSlot = current.frameDescriptor.findFrameSlot(name);
        while (frameSlot == null) {
            depth++;
            current = current.outer;
            if (current == null) {
                return new Pair<>(LEVEL_UNDEFINED, null);
            }
            frameSlot = current.frameDescriptor.findFrameSlot(name);
        }
        return new Pair<>(depth, frameSlot);
    }

    public boolean isInLoop() {
        return inLoop;
    }
}
