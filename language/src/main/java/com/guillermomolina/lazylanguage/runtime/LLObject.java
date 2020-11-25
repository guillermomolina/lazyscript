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
package com.guillermomolina.lazylanguage.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;

/**
 * Represents an Lazy object.
 *
 * This class defines operations that can be performed on Lazy Objects. While we could define all
 * these operations as individual AST nodes, we opted to define those operations by using
 * {@link com.oracle.truffle.api.library.Library a Truffle library}, or more concretely the
 * {@link InteropLibrary}. This has several advantages, but the primary one is that it allows Lazy
 * objects to be used in the interoperability message protocol, i.e. It allows other languages and
 * tools to operate on Lazy objects without necessarily knowing they are Lazy objects.
 *
 * Lazy Objects are essentially instances of {@link DynamicObject} (objects whose members can be
 * dynamically added and removed). We also annotate the class with {@link ExportLibrary} with value
 * {@link InteropLibrary InteropLibrary.class}. This essentially ensures that the build system and
 * runtime know that this class specifies the interop messages (i.e. operations) that Lazy can do on
 * {@link LLObject} instances.
 *
 * @see ExportLibrary
 * @see ExportMessage
 * @see InteropLibrary
 */
@ExportLibrary(InteropLibrary.class)
public final class LLObject extends LLObjectBase {
    public LLObject(Shape shape) {
        super(shape);
    }
}
