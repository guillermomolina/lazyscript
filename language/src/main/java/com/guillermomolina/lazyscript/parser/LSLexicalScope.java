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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.guillermomolina.lazyscript.LSLanguage;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public class LSLexicalScope {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(LSLanguage.ID, LSLexicalScope.class);

    public static final String THIS = "this";
    public static final String SUPER = "super";

    private final LSLexicalScope outer;
    private int parameterCount;
    private final Map<String, FrameSlot> locals;
    private final boolean inLoop;
    private final FrameDescriptor frameDescriptor;

    LSLexicalScope(LSLexicalScope outer, boolean inLoop) {
        this.outer = outer;
        this.inLoop = inLoop;
        this.parameterCount = 0;
        this.locals = new HashMap<>();
        if (inLoop) {
            this.frameDescriptor = outer.frameDescriptor;
            locals.putAll(outer.locals);
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

    public boolean hasVariable(final String name) {
        return locals.containsKey(name);
    }

    public FrameSlot addParameter(final String name) {
        if(parameterCount == 0 && !name.equals(THIS)) {
            throw new UnsupportedOperationException("First parameter must always be \"this\"");
        }
        LOG.log(Level.FINE, "Adding parameter index: " + parameterCount + " named: " + name);
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, parameterCount, FrameSlotKind.Illegal);
        FrameSlot existingSlot = locals.put(name, frameSlot);
        if(existingSlot != null) {
            throw new UnsupportedOperationException("Parameter already defined");
        }
        parameterCount++;
        return frameSlot;
    }

    public FrameSlot findOrAddVariable(final String name) {
        FrameSlot frameSlot = locals.get(name);
        if(frameSlot == null) {
            LOG.log(Level.FINE, "Adding local variable named: {0}", name);
            frameSlot = frameDescriptor.findOrAddFrameSlot(name, null, FrameSlotKind.Illegal);
            locals.put(name, frameSlot);    
        }
        return frameSlot;
    }

    public FrameSlot getLocal(final String name) {
        return locals.get(name);
    }

    public boolean isInLoop() {
        return inLoop;
    }
}
