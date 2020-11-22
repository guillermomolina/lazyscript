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
package com.guillermomolina.ll.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.guillermomolina.ll.LLLanguage;
import com.guillermomolina.ll.nodes.LLExpressionNode;
import com.guillermomolina.ll.nodes.LLRootNode;
import com.guillermomolina.ll.nodes.LLStatementNode;
import com.guillermomolina.ll.nodes.controlflow.LLBlockNode;
import com.guillermomolina.ll.nodes.controlflow.LLBreakNode;
import com.guillermomolina.ll.nodes.controlflow.LLContinueNode;
import com.guillermomolina.ll.nodes.controlflow.LLDebuggerNode;
import com.guillermomolina.ll.nodes.controlflow.LLFunctionBodyNode;
import com.guillermomolina.ll.nodes.controlflow.LLIfNode;
import com.guillermomolina.ll.nodes.controlflow.LLReturnNode;
import com.guillermomolina.ll.nodes.controlflow.LLWhileNode;
import com.guillermomolina.ll.nodes.expression.LLAddNodeGen;
import com.guillermomolina.ll.nodes.expression.LLBigIntegerLiteralNode;
import com.guillermomolina.ll.nodes.expression.LLDivNodeGen;
import com.guillermomolina.ll.nodes.expression.LLEqualNodeGen;
import com.guillermomolina.ll.nodes.expression.LLFunctionLiteralNode;
import com.guillermomolina.ll.nodes.expression.LLInvokeNode;
import com.guillermomolina.ll.nodes.expression.LLLessOrEqualNodeGen;
import com.guillermomolina.ll.nodes.expression.LLLessThanNodeGen;
import com.guillermomolina.ll.nodes.expression.LLLogicalAndNode;
import com.guillermomolina.ll.nodes.expression.LLLogicalNotNodeGen;
import com.guillermomolina.ll.nodes.expression.LLLogicalOrNode;
import com.guillermomolina.ll.nodes.expression.LLLongLiteralNode;
import com.guillermomolina.ll.nodes.expression.LLMulNodeGen;
import com.guillermomolina.ll.nodes.expression.LLParenExpressionNode;
import com.guillermomolina.ll.nodes.expression.LLReadPropertyNode;
import com.guillermomolina.ll.nodes.expression.LLReadPropertyNodeGen;
import com.guillermomolina.ll.nodes.expression.LLStringLiteralNode;
import com.guillermomolina.ll.nodes.expression.LLSubNodeGen;
import com.guillermomolina.ll.nodes.expression.LLWritePropertyNode;
import com.guillermomolina.ll.nodes.expression.LLWritePropertyNodeGen;
import com.guillermomolina.ll.nodes.local.LLReadArgumentNode;
import com.guillermomolina.ll.nodes.local.LLReadLocalVariableNode;
import com.guillermomolina.ll.nodes.local.LLReadLocalVariableNodeGen;
import com.guillermomolina.ll.nodes.local.LLWriteLocalVariableNode;
import com.guillermomolina.ll.nodes.local.LLWriteLocalVariableNodeGen;
import com.guillermomolina.ll.nodes.util.LLUnboxNodeGen;

/**
 * Helper class used by the LL {@link Parser} to create nodes. The code is factored out of the
 * automatically generated parser to keep the attributed grammar of LL small.
 */
public class LLNodeFactory {

    /**
     * Local variable names that are visible in the current block. Variables are not visible outside
     * of their defining block, to prevent the usage of undefined variables. Because of that, we can
     * decide during parsing if a name references a local variable or is a function name.
     */
    static class LexicalScope {
        protected final LexicalScope outer;
        protected final Map<String, FrameSlot> locals;

        LexicalScope(LexicalScope outer) {
            this.outer = outer;
            this.locals = new HashMap<>();
            if (outer != null) {
                locals.putAll(outer.locals);
            }
        }
    }

    /* State while parsing a source unit. */
    private final Source source;
    private final Map<String, RootCallTarget> allFunctions;

    /* State while parsing a function. */
    private int functionStartPos;
    private String functionName;
    private int functionBodyStartPos; // includes parameter list
    private int parameterCount;
    private FrameDescriptor frameDescriptor;
    private List<LLStatementNode> methodNodes;

    /* State while parsing a block. */
    private LexicalScope lexicalScope;
    private final LLLanguage language;

    public LLNodeFactory(LLLanguage language, Source source) {
        this.language = language;
        this.source = source;
        this.allFunctions = new HashMap<>();
    }

    public Map<String, RootCallTarget> getAllFunctions() {
        return allFunctions;
    }

    public void startFunction(Token nameToken, Token bodyStartToken) {
        assert functionStartPos == 0;
        assert functionName == null;
        assert functionBodyStartPos == 0;
        assert parameterCount == 0;
        assert frameDescriptor == null;
        assert lexicalScope == null;

        functionStartPos = nameToken.getStartIndex();
        functionName = nameToken.getText();
        functionBodyStartPos = bodyStartToken.getStartIndex();
        frameDescriptor = new FrameDescriptor();
        methodNodes = new ArrayList<>();
        startBlock();
    }

    public void addFormalParameter(Token nameToken) {
        /*
         * Method parameters are assigned to local variables at the beginning of the method. This
         * ensures that accesses to parameters are specialized the same way as local variables are
         * specialized.
         */
        final LLReadArgumentNode readArg = new LLReadArgumentNode(parameterCount);
        LLExpressionNode assignment = createAssignment(createStringLiteral(nameToken, false), readArg, parameterCount);
        methodNodes.add(assignment);
        parameterCount++;
    }

    public void finishFunction(LLStatementNode bodyNode) {
        if (bodyNode == null) {
            // a state update that would otherwise be performed by finishBlock
            lexicalScope = lexicalScope.outer;
        } else {
            methodNodes.add(bodyNode);
            final int bodyEndPos = bodyNode.getSourceEndIndex();
            final SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
            final LLStatementNode methodBlock = finishBlock(methodNodes, functionBodyStartPos, bodyEndPos - functionBodyStartPos);
            assert lexicalScope == null : "Wrong scoping of blocks in parser";

            final LLFunctionBodyNode functionBodyNode = new LLFunctionBodyNode(methodBlock);
            functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());

            final LLRootNode rootNode = new LLRootNode(language, frameDescriptor, functionBodyNode, functionSrc, functionName);
            allFunctions.put(functionName, Truffle.getRuntime().createCallTarget(rootNode));
        }

        functionStartPos = 0;
        functionName = null;
        functionBodyStartPos = 0;
        parameterCount = 0;
        frameDescriptor = null;
        lexicalScope = null;
    }

    public void startBlock() {
        lexicalScope = new LexicalScope(lexicalScope);
    }

    public LLStatementNode finishBlock(List<LLStatementNode> bodyNodes, int startPos, int length) {
        lexicalScope = lexicalScope.outer;

        if (containsNull(bodyNodes)) {
            return null;
        }

        List<LLStatementNode> flattenedNodes = new ArrayList<>(bodyNodes.size());
        flattenBlocks(bodyNodes, flattenedNodes);
        for (LLStatementNode statement : flattenedNodes) {
            if (statement.hasSource() && !isHaltInCondition(statement)) {
                statement.addStatementTag();
            }
        }
        LLBlockNode blockNode = new LLBlockNode(flattenedNodes.toArray(new LLStatementNode[flattenedNodes.size()]));
        blockNode.setSourceSection(startPos, length);
        return blockNode;
    }

    private static boolean isHaltInCondition(LLStatementNode statement) {
        return (statement instanceof LLIfNode) || (statement instanceof LLWhileNode);
    }

    private void flattenBlocks(Iterable<? extends LLStatementNode> bodyNodes, List<LLStatementNode> flattenedNodes) {
        for (LLStatementNode n : bodyNodes) {
            if (n instanceof LLBlockNode) {
                flattenBlocks(((LLBlockNode) n).getStatements(), flattenedNodes);
            } else {
                flattenedNodes.add(n);
            }
        }
    }

    /**
     * Returns an {@link LLDebuggerNode} for the given token.
     *
     * @param debuggerToken The token containing the debugger node's info.
     * @return A LLDebuggerNode for the given token.
     */
    LLStatementNode createDebugger(Token debuggerToken) {
        final LLDebuggerNode debuggerNode = new LLDebuggerNode();
        srcFromToken(debuggerNode, debuggerToken);
        return debuggerNode;
    }

    /**
     * Returns an {@link LLBreakNode} for the given token.
     *
     * @param breakToken The token containing the break node's info.
     * @return A LLBreakNode for the given token.
     */
    public LLStatementNode createBreak(Token breakToken) {
        final LLBreakNode breakNode = new LLBreakNode();
        srcFromToken(breakNode, breakToken);
        return breakNode;
    }

    /**
     * Returns an {@link LLContinueNode} for the given token.
     *
     * @param continueToken The token containing the continue node's info.
     * @return A LLContinueNode built using the given token.
     */
    public LLStatementNode createContinue(Token continueToken) {
        final LLContinueNode continueNode = new LLContinueNode();
        srcFromToken(continueNode, continueToken);
        return continueNode;
    }

    /**
     * Returns an {@link LLWhileNode} for the given parameters.
     *
     * @param whileToken The token containing the while node's info
     * @param conditionNode The conditional node for this while loop
     * @param bodyNode The body of the while loop
     * @return A LLWhileNode built using the given parameters. null if either conditionNode or
     *         bodyNode is null.
     */
    public LLStatementNode createWhile(Token whileToken, LLExpressionNode conditionNode, LLStatementNode bodyNode) {
        if (conditionNode == null || bodyNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final int start = whileToken.getStartIndex();
        final int end = bodyNode.getSourceEndIndex();
        final LLWhileNode whileNode = new LLWhileNode(conditionNode, bodyNode);
        whileNode.setSourceSection(start, end - start);
        return whileNode;
    }

    /**
     * Returns an {@link LLIfNode} for the given parameters.
     *
     * @param ifToken The token containing the if node's info
     * @param conditionNode The condition node of this if statement
     * @param thenPartNode The then part of the if
     * @param elsePartNode The else part of the if (null if no else part)
     * @return An LLIfNode for the given parameters. null if either conditionNode or thenPartNode is
     *         null.
     */
    public LLStatementNode createIf(Token ifToken, LLExpressionNode conditionNode, LLStatementNode thenPartNode, LLStatementNode elsePartNode) {
        if (conditionNode == null || thenPartNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final int start = ifToken.getStartIndex();
        final int end = elsePartNode == null ? thenPartNode.getSourceEndIndex() : elsePartNode.getSourceEndIndex();
        final LLIfNode ifNode = new LLIfNode(conditionNode, thenPartNode, elsePartNode);
        ifNode.setSourceSection(start, end - start);
        return ifNode;
    }

    /**
     * Returns an {@link LLReturnNode} for the given parameters.
     *
     * @param t The token containing the return node's info
     * @param valueNode The value of the return (null if not returning a value)
     * @return An LLReturnNode for the given parameters.
     */
    public LLStatementNode createReturn(Token t, LLExpressionNode valueNode) {
        final int start = t.getStartIndex();
        final int length = valueNode == null ? t.getText().length() : valueNode.getSourceEndIndex() - start;
        final LLReturnNode returnNode = new LLReturnNode(valueNode);
        returnNode.setSourceSection(start, length);
        return returnNode;
    }

    /**
     * Returns the corresponding subclass of {@link LLExpressionNode} for binary expressions. </br>
     * These nodes are currently not instrumented.
     *
     * @param opToken The operator of the binary expression
     * @param leftNode The left node of the expression
     * @param rightNode The right node of the expression
     * @return A subclass of LLExpressionNode using the given parameters based on the given opToken.
     *         null if either leftNode or rightNode is null.
     */
    public LLExpressionNode createBinary(Token opToken, LLExpressionNode leftNode, LLExpressionNode rightNode) {
        if (leftNode == null || rightNode == null) {
            return null;
        }
        final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
        final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);

        final LLExpressionNode result;
        switch (opToken.getText()) {
            case "+":
                result = LLAddNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "*":
                result = LLMulNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "/":
                result = LLDivNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "-":
                result = LLSubNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "<":
                result = LLLessThanNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "<=":
                result = LLLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case ">":
                result = LLLogicalNotNodeGen.create(LLLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            case ">=":
                result = LLLogicalNotNodeGen.create(LLLessThanNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            case "==":
                result = LLEqualNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case "!=":
                result = LLLogicalNotNodeGen.create(LLEqualNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            case "&&":
                result = new LLLogicalAndNode(leftUnboxed, rightUnboxed);
                break;
            case "||":
                result = new LLLogicalOrNode(leftUnboxed, rightUnboxed);
                break;
            default:
                throw new RuntimeException("unexpected operation: " + opToken.getText());
        }

        int start = leftNode.getSourceCharIndex();
        int length = rightNode.getSourceEndIndex() - start;
        result.setSourceSection(start, length);
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns an {@link LLInvokeNode} for the given parameters.
     *
     * @param functionNode The function being called
     * @param parameterNodes The parameters of the function call
     * @param finalToken A token used to determine the end of the sourceSelection for this call
     * @return An LLInvokeNode for the given parameters. null if functionNode or any of the
     *         parameterNodes are null.
     */
    public LLExpressionNode createCall(LLExpressionNode functionNode, List<LLExpressionNode> parameterNodes, Token finalToken) {
        if (functionNode == null || containsNull(parameterNodes)) {
            return null;
        }

        final LLExpressionNode result = new LLInvokeNode(functionNode, parameterNodes.toArray(new LLExpressionNode[parameterNodes.size()]));

        final int startPos = functionNode.getSourceCharIndex();
        final int endPos = finalToken.getStartIndex() + finalToken.getText().length();
        result.setSourceSection(startPos, endPos - startPos);
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns an {@link LLWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode The name of the variable being assigned
     * @param valueNode The value to be assigned
     * @return An LLExpressionNode for the given parameters. null if nameNode or valueNode is null.
     */
    public LLExpressionNode createAssignment(LLExpressionNode nameNode, LLExpressionNode valueNode) {
        return createAssignment(nameNode, valueNode, null);
    }

    /**
     * Returns an {@link LLWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode The name of the variable being assigned
     * @param valueNode The value to be assigned
     * @param argumentIndex null or index of the argument the assignment is assigning
     * @return An LLExpressionNode for the given parameters. null if nameNode or valueNode is null.
     */
    public LLExpressionNode createAssignment(LLExpressionNode nameNode, LLExpressionNode valueNode, Integer argumentIndex) {
        if (nameNode == null || valueNode == null) {
            return null;
        }

        String name = ((LLStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(
                        name,
                        argumentIndex,
                        FrameSlotKind.Illegal);
        lexicalScope.locals.put(name, frameSlot);
        final LLExpressionNode result = LLWriteLocalVariableNodeGen.create(valueNode, frameSlot, nameNode);

        if (valueNode.hasSource()) {
            final int start = nameNode.getSourceCharIndex();
            final int length = valueNode.getSourceEndIndex() - start;
            result.setSourceSection(start, length);
        }
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns a {@link LLReadLocalVariableNode} if this read is a local variable or a
     * {@link LLFunctionLiteralNode} if this read is global. In LL, the only global names are
     * functions.
     *
     * @param nameNode The name of the variable/function being read
     * @return either:
     *         <ul>
     *         <li>A LLReadLocalVariableNode representing the local variable being read.</li>
     *         <li>A LLFunctionLiteralNode representing the function definition.</li>
     *         <li>null if nameNode is null.</li>
     *         </ul>
     */
    public LLExpressionNode createRead(LLExpressionNode nameNode) {
        if (nameNode == null) {
            return null;
        }

        String name = ((LLStringLiteralNode) nameNode).executeGeneric(null);
        final LLExpressionNode result;
        final FrameSlot frameSlot = lexicalScope.locals.get(name);
        if (frameSlot != null) {
            /* Read of a local variable. */
            result = LLReadLocalVariableNodeGen.create(frameSlot);
        } else {
            /* Read of a global name. In our language, the only global names are functions. */
            result = new LLFunctionLiteralNode(name);
        }
        result.setSourceSection(nameNode.getSourceCharIndex(), nameNode.getSourceLength());
        result.addExpressionTag();
        return result;
    }

    public LLExpressionNode createStringLiteral(Token literalToken, boolean removeQuotes) {
        /* Remove the trailing and ending " */
        String literal = literalToken.getText();
        if (removeQuotes) {
            assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
            literal = literal.substring(1, literal.length() - 1);
        }

        final LLStringLiteralNode result = new LLStringLiteralNode(literal.intern());
        srcFromToken(result, literalToken);
        result.addExpressionTag();
        return result;
    }

    public LLExpressionNode createNumericLiteral(Token literalToken) {
        LLExpressionNode result;
        try {
            /* Try if the literal is small enough to fit into a long value. */
            result = new LLLongLiteralNode(Long.parseLong(literalToken.getText()));
        } catch (NumberFormatException ex) {
            /* Overflow of long value, so fall back to BigInteger. */
            result = new LLBigIntegerLiteralNode(new BigInteger(literalToken.getText()));
        }
        srcFromToken(result, literalToken);
        result.addExpressionTag();
        return result;
    }

    public LLExpressionNode createParenExpression(LLExpressionNode expressionNode, int start, int length) {
        if (expressionNode == null) {
            return null;
        }

        final LLParenExpressionNode result = new LLParenExpressionNode(expressionNode);
        result.setSourceSection(start, length);
        return result;
    }

    /**
     * Returns an {@link LLReadPropertyNode} for the given parameters.
     *
     * @param receiverNode The receiver of the property access
     * @param nameNode The name of the property being accessed
     * @return An LLExpressionNode for the given parameters. null if receiverNode or nameNode is
     *         null.
     */
    public LLExpressionNode createReadProperty(LLExpressionNode receiverNode, LLExpressionNode nameNode) {
        if (receiverNode == null || nameNode == null) {
            return null;
        }

        final LLExpressionNode result = LLReadPropertyNodeGen.create(receiverNode, nameNode);

        final int startPos = receiverNode.getSourceCharIndex();
        final int endPos = nameNode.getSourceEndIndex();
        result.setSourceSection(startPos, endPos - startPos);
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns an {@link LLWritePropertyNode} for the given parameters.
     *
     * @param receiverNode The receiver object of the property assignment
     * @param nameNode The name of the property being assigned
     * @param valueNode The value to be assigned
     * @return An LLExpressionNode for the given parameters. null if receiverNode, nameNode or
     *         valueNode is null.
     */
    public LLExpressionNode createWriteProperty(LLExpressionNode receiverNode, LLExpressionNode nameNode, LLExpressionNode valueNode) {
        if (receiverNode == null || nameNode == null || valueNode == null) {
            return null;
        }

        final LLExpressionNode result = LLWritePropertyNodeGen.create(receiverNode, nameNode, valueNode);

        final int start = receiverNode.getSourceCharIndex();
        final int length = valueNode.getSourceEndIndex() - start;
        result.setSourceSection(start, length);
        result.addExpressionTag();

        return result;
    }

    /**
     * Creates source description of a single token.
     */
    private static void srcFromToken(LLStatementNode node, Token token) {
        node.setSourceSection(token.getStartIndex(), token.getText().length());
    }

    /**
     * Checks whether a list contains a null.
     */
    private static boolean containsNull(List<?> list) {
        for (Object e : list) {
            if (e == null) {
                return true;
            }
        }
        return false;
    }

}
