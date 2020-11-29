/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.guillermomolina.lazylanguage.nodes.expression;

import com.guillermomolina.lazylanguage.LazyLanguage;
import com.guillermomolina.lazylanguage.nodes.LLExpressionNode;
import com.guillermomolina.lazylanguage.nodes.util.LLToMemberNode;
import com.guillermomolina.lazylanguage.runtime.LLObject;
import com.guillermomolina.lazylanguage.runtime.LLUndefinedNameException;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * The node for reading a property of an object. When executed, this node:
 * <ol>
 * <li>evaluates the object expression on the left hand side of the object
 * access operator</li>
 * <li>evaluated the property name</li>
 * <li>reads the named property</li>
 * </ol>
 */
@NodeInfo(shortName = ".")
@NodeChild("receiverNode")
@NodeChild("nameNode")
public abstract class LLReadPropertyNode extends LLExpressionNode {

    static final int LIBRARY_LIMIT = 3;

    @Specialization(guards = "arrays.hasArrayElements(receiver)", limit = "LIBRARY_LIMIT")
    protected Object readArray(Object receiver, Object index, @CachedLibrary("receiver") InteropLibrary arrays,
            @CachedLibrary("index") InteropLibrary numbers) {
        try {
            return arrays.readArrayElement(receiver, numbers.asLong(index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            // read was not successful. In Lazy we only have basic support for errors.
            throw LLUndefinedNameException.undefinedProperty(this, index);
        }
    }

    @Specialization(guards = "objects.hasMembers(receiver)", limit = "LIBRARY_LIMIT")
    protected Object readObject(Object receiver, Object name, @CachedLibrary("receiver") InteropLibrary objects,
            @Cached LLToMemberNode asMember) {
        try {
            return objects.readMember(receiver, asMember.execute(name));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            // read was not successful. In Lazy we only have basic support for errors.
            throw LLUndefinedNameException.undefinedProperty(this, name);
        }
    }

    @Specialization(limit = "LIBRARY_LIMIT")
    protected Object readNonObject(Object receiver, Object name, @CachedLibrary("receiver") InteropLibrary objects,
            @Cached LLToMemberNode asMember) {
        try {
            Object prototype = lookupContextReference(LazyLanguage.class).get().getPrototype(receiver);
            if(name instanceof String && ((String)name).compareTo(LLObject.PROTOTYPE) == 0) {
                return prototype;
            }
            return objects.readMember(prototype, asMember.execute(name));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            // read was not successful. In Lazy we only have basic support for errors.
            throw LLUndefinedNameException.undefinedProperty(this, name);
        }
    }

}
