/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;

import com.guillermomolina.lazylanguage.LazyLanguage;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.api.utilities.TriState;

@ExportLibrary(InteropLibrary.class)
public final class LLFunction extends LLObject {

    public static final int INLINE_CACHE_SIZE = 2;

    private static final TruffleLogger LOG = TruffleLogger.getLogger(LazyLanguage.ID, LLFunction.class);

    /** The name of the function. */
    private final String name;

    /** The current implementation of this function. */
    private RootCallTarget callTarget;

    /**
     * Manages the assumption that the {@link #callTarget} is stable. We use the utility class
     * {@link CyclicAssumption}, which automatically creates a new {@link Assumption} when the old
     * one gets invalidated.
     */
    private final CyclicAssumption callTargetStable;

    public LLFunction(LazyLanguage language, String name, RootCallTarget callTarget) {
        this.name = name;
        this.callTarget = callTarget;
        this.callTargetStable = new CyclicAssumption(name);
    }

    public String getName() {
        return name;
    }

    protected void setCallTarget(RootCallTarget callTarget) {
        this.callTarget = callTarget;
        /*
         * We have a new call target. Invalidate all code that speculated that the old call target
         * was stable.
         */
        LOG.log(Level.FINE, "Installed call target for: {0}", name);
        callTargetStable.invalidate();
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    /**
     * This method is, e.g., called when using a function literal in a string concatenation. So
     * changing it has an effect on Lazy programs.
     */
    @Override
    public String toString() {
        return "aFunction";
    }

    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    Class<? extends TruffleLanguage<LLContext>> getLanguage() {
        return LazyLanguage.class;
    }

    /**
     * {@link LLFunction} instances are always visible as executable to other languages.
     */
    @ExportMessage
    @TruffleBoundary
    SourceSection getSourceLocation() {
        return getCallTarget().getRootNode().getSourceSection();
    }

    @ExportMessage
    boolean hasSourceLocation() {
        return true;
    }

    /**
     * {@link LLFunction} instances are always visible as executable to other languages.
     */
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return LLType.FUNCTION;
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doLLFunction(LLFunction receiver, LLFunction other) {
            /*
             * LLFunctions are potentially identical to other LLFunctions.
             */
            return receiver == other ? TriState.TRUE : TriState.FALSE;
        }

        @Fallback
        static TriState doOther(LLFunction receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    static int identityHashCode(LLFunction receiver) {
        return System.identityHashCode(receiver);
    }

    @ExportMessage
    Object toDisplayString(boolean allowSideEffects) {
        return "aFunction";
    }

    /**
     * We allow languages to execute this function. We implement the interop execute message that
     * forwards to a function dispatch.
     *
     * Since invocations are potentially expensive (result in an indirect call, which is expensive
     * by itself but also limits function inlining which can hinder other optimisations) if the node
     * turns megamorphic (i.e. cache limit is exceeded) we annotate it with {@ReportPolymorphism}.
     * This ensures that the runtime is notified when this node turns polymorphic. This, in turn,
     * may, under certain conditions, cause the runtime to attempt to make node monomorphic again by
     * duplicating the entire AST containing that node and specialising it for a particular call
     * site.
     */
    @ReportPolymorphism
    @ExportMessage
    abstract static class Execute {

        /**
         * Inline cached specialization of the dispatch.
         *
         * <p>
         * Since Lazy is a quite lazy language, the benefit of the inline cache seems small: after
         * checking that the actual function to be executed is the same as the cachedFuntion, we can
         * safely execute the cached call target. You can reasonably argue that caching the call
         * target is overkill, since we could just retrieve it via {@code function.getCallTarget()}.
         * However, caching the call target and using a {@link DirectCallNode} allows Truffle to
         * perform method inlining. In addition, in a more complex language the lookup of the call
         * target is usually much more complicated than in Lazy.
         * </p>
         *
         * <p>
         * {@code limit = "INLINE_CACHE_SIZE"} Specifies the limit number of inline cache
         * specialization instantiations.
         * </p>
         * <p>
         * {@code guards = "function.getCallTarget() == cachedTarget"} The inline cache check. Note
         * that cachedTarget is a final field so that the compiler can optimize the check.
         * </p>
         * <p>
         * {@code assumptions = "callTargetStable"} Support for function redefinition: When a
         * function is redefined, the call target maintained by the LLFunction object is changed. To
         * avoid a check for that, we use an Assumption that is invalidated by the LLFunction when
         * the change is performed. Since checking an assumption is a no-op in compiled code, the
         * assumption check performed by the DLL does not add any overhead during optimized
         * execution.
         * </p>
         *
         * @see Cached
         * @see Specialization
         *
         * @param function the dynamically provided function
         * @param arguments the arguments to the function
         * @param callTargetStable The assumption object assuming the function was not redefined.
         * @param cachedTarget The call target we aim to invoke
         * @param callNode the {@link DirectCallNode} specifically created for the
         *            {@link CallTarget} in cachedFunction.
         */
        @Specialization(limit = "INLINE_CACHE_SIZE", //
                        guards = "function.getCallTarget() == cachedTarget", //
                        assumptions = "callTargetStable")
        protected static Object doDirect(LLFunction function, Object[] arguments,
                        @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                        @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                        @Cached("create(cachedTarget)") DirectCallNode callNode) {

            /* Inline cache hit, we are safe to execute the cached call target. */
            Object returnValue = callNode.call(arguments);
            return returnValue;
        }

        /**
         * Slow-path code for a call, used when the polymorphic inline cache exceeded its maximum
         * size specified in <code>INLINE_CACHE_SIZE</code>. Such calls are not optimized any
         * further, e.g., no method inlining is performed.
         */
        @Specialization(replaces = "doDirect")
        protected static Object doIndirect(LLFunction function, Object[] arguments,
                        @Cached IndirectCallNode callNode) {
            /*
             * Lazy has a quite lazy call lookup: just ask the function for the current call target,
             * and call it.
             */
            return callNode.call(function.getCallTarget(), arguments);
        }
    }

}
