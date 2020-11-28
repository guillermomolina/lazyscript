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
package com.guillermomolina.lazylanguage.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.guillermomolina.lazylanguage.LLLanguage;
import com.guillermomolina.lazylanguage.NotImplementedException;
import com.guillermomolina.lazylanguage.nodes.LLExpressionNode;
import com.guillermomolina.lazylanguage.nodes.LLRootNode;
import com.guillermomolina.lazylanguage.nodes.LLStatementNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLBlockNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLBreakNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLContinueNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLFunctionBodyNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLIfNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLReturnNode;
import com.guillermomolina.lazylanguage.nodes.controlflow.LLWhileNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLAddNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLDivNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLEqualNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLInvokeNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLessOrEqualNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLLessThanNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLLogicalAndNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLLogicalNotNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLLogicalOrNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLMulNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLParenExpressionNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLReadPropertyNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLReadPropertyNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLSubNodeGen;
import com.guillermomolina.lazylanguage.nodes.expression.LLWritePropertyNode;
import com.guillermomolina.lazylanguage.nodes.expression.LLWritePropertyNodeGen;
import com.guillermomolina.lazylanguage.nodes.literals.LLBigIntegerLiteralNode;
import com.guillermomolina.lazylanguage.nodes.literals.LLFunctionLiteralNode;
import com.guillermomolina.lazylanguage.nodes.literals.LLLongLiteralNode;
import com.guillermomolina.lazylanguage.nodes.literals.LLStringLiteralNode;
import com.guillermomolina.lazylanguage.nodes.local.LLReadArgumentNode;
import com.guillermomolina.lazylanguage.nodes.local.LLReadLocalVariableNode;
import com.guillermomolina.lazylanguage.nodes.local.LLReadLocalVariableNodeGen;
import com.guillermomolina.lazylanguage.nodes.local.LLWriteLocalVariableNode;
import com.guillermomolina.lazylanguage.nodes.local.LLWriteLocalVariableNodeGen;
import com.guillermomolina.lazylanguage.nodes.util.LLUnboxNodeGen;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Helper class used by the Lazy {@link Parser} to create nodes. The code is
 * factored out of the automatically generated parser to keep the attributed
 * grammar of Lazy small.
 */
public class LLParserVisitor extends LazyLanguageParserBaseVisitor<Node> {

    /**
     * Local variable names that are visible in the current block. Variables are not
     * visible outside of their defining block, to prevent the usage of undefined
     * variables. Because of that, we can decide during parsing if a name references
     * a local variable or is a function name.
     */
    static class LexicalScope {
        public static final String THIS = "this";
        public static final String CONTEXT = "context";
        public static final String SUPER = "super";
        protected final LexicalScope outer;
        protected final Map<String, FrameSlot> locals;
        protected final boolean inLoop;
        protected final List<LLStatementNode> statementNodes;

        LexicalScope(LexicalScope outer, boolean inLoop) {
            this.outer = outer;
            this.inLoop = inLoop;
            this.locals = new HashMap<>();
            this.statementNodes = new ArrayList<>();

            if (outer != null) {
                locals.putAll(outer.locals);
            }
        }
    }

    private static final class BailoutErrorListener extends BaseErrorListener {
        private final Source source;

        BailoutErrorListener(Source source) {
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                String msg, RecognitionException e) {
            Token token = (Token) offendingSymbol;
            throw new LLParseError(source, token, msg);
        }
    }

    /* State while parsing a source unit. */
    private final Source source;
    private final LazyLanguageLexer lexer;
    private final LazyLanguageParser parser;

    /* State while parsing a function. */
    private int functionStartPos;
    private String functionName;
    private int functionBodyStartPos; // includes parameter list
    private FrameDescriptor frameDescriptor;

    /* State while parsing a block. */
    private LexicalScope lexicalScope;
    private final LLLanguage language;

    public LLParserVisitor(LLLanguage language, Source source) {
        this.language = language;
        this.source = source;
        this.lexer = new LazyLanguageLexer(CharStreams.fromString(source.getCharacters().toString()));
        this.parser = new LazyLanguageParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        BailoutErrorListener listener = new BailoutErrorListener(source);
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
    }

    public RootCallTarget parse() {
        RootNode rootNode = (RootNode) visit(parser.module());
        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    private static Interval srcFromContext(ParserRuleContext ctx) {
        int a = ctx.start.getStartIndex();
        int b = ctx.stop.getStopIndex();
        return new Interval(a, b);
    }

    private void setSourceFromContext(LLStatementNode node, ParserRuleContext ctx) {
        Interval sourceInterval = srcFromContext(ctx);
        assert sourceInterval != null;
        if (node == null) {
            throw new LLParseError(source, ctx, "Node is null");
        }
        assert node != null;
        node.setSourceSection(sourceInterval.a, sourceInterval.length());
    }

    public void pushScope(boolean inLoop) {
        lexicalScope = new LexicalScope(lexicalScope, inLoop);
    }

    public void popScope() {
        lexicalScope = lexicalScope.outer;
    }

    @Override
    public Node visitModule(LazyLanguageParser.ModuleContext ctx) {
        assert functionStartPos == 0;
        assert functionName == null;
        assert functionBodyStartPos == 0;
        assert frameDescriptor == null;
        assert lexicalScope == null;

        functionName = source.getName();
        final int extensionIndex = functionName.lastIndexOf(".");
        if (extensionIndex != -1) {
            functionName = functionName.substring(0, extensionIndex);
        }
        frameDescriptor = new FrameDescriptor();
        pushScope(false);

        final LLReadArgumentNode readArg0 = new LLReadArgumentNode(0);
        final LLExpressionNode stringLiteral = createStringLiteral(LexicalScope.THIS, false);
        LLExpressionNode assignment = createAssignment(stringLiteral, readArg0, 0);
        lexicalScope.statementNodes.add(assignment);

        List<LLStatementNode> bodyNodes = lexicalScope.statementNodes;

        for (LazyLanguageParser.StatementContext statement : ctx.statement()) {
            bodyNodes.add((LLStatementNode) visit(statement));
        }

        popScope();

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
        LLStatementNode methodBlock = new LLBlockNode(
                flattenedNodes.toArray(new LLStatementNode[flattenedNodes.size()]));

        setSourceFromContext(methodBlock, ctx);
        assert lexicalScope == null : "Wrong scoping of blocks in parser";

        final LLFunctionBodyNode functionBodyNode = new LLFunctionBodyNode(methodBlock);
        final int bodyEndPos = methodBlock.getSourceEndIndex();
        SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
        functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());
        final LLRootNode rootNode = new LLRootNode(language, frameDescriptor, functionBodyNode, functionSrc,
                functionName);

        functionStartPos = 0;
        functionName = null;
        functionBodyStartPos = 0;
        frameDescriptor = null;
        lexicalScope = null;

        return rootNode;
    }

    @Override
    public Node visitFunction(LazyLanguageParser.FunctionContext ctx) {
        throw new NotImplementedException();
    }

    public Node visitFunction2(LazyLanguageParser.FunctionContext ctx) {
        assert functionStartPos == 0;
        assert functionName == null;
        assert functionBodyStartPos == 0;
        assert frameDescriptor == null;
        assert lexicalScope == null;

        Token nameToken = ctx.IDENTIFIER().getSymbol();
        Token bodyStartToken = ctx.block().getStart();

        functionStartPos = nameToken.getStartIndex();
        functionName = nameToken.getText();
        functionBodyStartPos = bodyStartToken.getStartIndex();
        frameDescriptor = new FrameDescriptor();
        pushScope(false);

        int parameterCount = 0;
        if (ctx.functionParameters() != null) {
            for (TerminalNode nameNode : ctx.functionParameters().IDENTIFIER()) {
                final LLReadArgumentNode readArg = new LLReadArgumentNode(parameterCount);
                final LLExpressionNode stringLiteral = createStringLiteral(nameNode.getSymbol(), false);
                LLExpressionNode assignment = createAssignment(stringLiteral, readArg, parameterCount);
                lexicalScope.statementNodes.add(assignment);
                parameterCount++;
            }
        }

        final LLStatementNode methodBlock = (LLStatementNode) visit(ctx.block());
        if (methodBlock != null) {
            assert lexicalScope == null : "Wrong scoping of blocks in parser";

            final LLFunctionBodyNode functionBodyNode = new LLFunctionBodyNode(methodBlock);
            final int bodyEndPos = methodBlock.getSourceEndIndex();
            SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
            functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());
            final LLRootNode rootNode = new LLRootNode(language, frameDescriptor, functionBodyNode, functionSrc,
                    functionName);
            // allFunctions.put(functionName,
            // Truffle.getRuntime().createCallTarget(rootNode));
        }

        functionStartPos = 0;
        functionName = null;
        functionBodyStartPos = 0;
        frameDescriptor = null;
        lexicalScope = null;

        return null;
    }

    @Override
    public Node visitBlock(LazyLanguageParser.BlockContext ctx) {
        List<LLStatementNode> bodyNodes = lexicalScope.statementNodes;

        for (LazyLanguageParser.StatementContext statement : ctx.statement()) {
            bodyNodes.add((LLStatementNode) visit(statement));
        }

        popScope();

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
        setSourceFromContext(blockNode, ctx);
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

    @Override
    public Node visitStatement(LazyLanguageParser.StatementContext ctx) {
        // Tricky: avoid calling visit on ctx.SEMI()
        if (ctx.getChild(0) != null && ctx.getChild(0) != ctx.SEMI()) {
            return visit(ctx.getChild(0));
        }
        throw new LLParseError(source, ctx, "Malformed statement");
    }

    @Override
    public Node visitExpression(LazyLanguageParser.ExpressionContext ctx) {
        LLExpressionNode leftNode = null;
        for (final LazyLanguageParser.LogicTermContext context : ctx.logicTerm()) {
            final LLExpressionNode rightNode = (LLExpressionNode) visit(context);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
                final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);
                leftNode = new LLLogicalOrNode(leftUnboxed, rightUnboxed);
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitLogicTerm(LazyLanguageParser.LogicTermContext ctx) {
        LLExpressionNode leftNode = null;
        for (final LazyLanguageParser.LogicFactorContext context : ctx.logicFactor()) {
            final LLExpressionNode rightNode = (LLExpressionNode) visit(context);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
                final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);
                leftNode = new LLLogicalAndNode(leftUnboxed, rightUnboxed);
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitLogicFactor(LazyLanguageParser.LogicFactorContext ctx) {
        LLExpressionNode leftNode = (LLExpressionNode) visit(ctx.left);
        if (ctx.op != null) {
            final LLExpressionNode rightNode = (LLExpressionNode) visit(ctx.right);
            final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
            final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);
            final String operator = ctx.op.getText();
            switch (operator) {
                case "<":
                    leftNode = LLLessThanNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case "<=":
                    leftNode = LLLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case ">":
                    leftNode = LLLogicalNotNodeGen.create(LLLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                case ">=":
                    leftNode = LLLogicalNotNodeGen.create(LLLessThanNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                case "==":
                    leftNode = LLEqualNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case "!=":
                    leftNode = LLLogicalNotNodeGen.create(LLEqualNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                default:
                    throw new LLParseError(source, ctx, "Invalid logic operator: " + operator);
            }
            setSourceFromContext(leftNode, ctx);
            leftNode.addExpressionTag();
        }
        return leftNode;
    }

    @Override
    public Node visitArithmetic(LazyLanguageParser.ArithmeticContext ctx) {
        LLExpressionNode leftNode = null;
        int index = 0;
        for (final LazyLanguageParser.TermContext termCtx : ctx.term()) {
            final LLExpressionNode rightNode = (LLExpressionNode) visit(termCtx);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LazyLanguageParser.TermOperatorContext operatorCtx = ctx.termOperator(index++);
                final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
                final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);
                if (operatorCtx.ADD() != null) {
                    leftNode = LLAddNodeGen.create(leftUnboxed, rightUnboxed);
                } else if (operatorCtx.SUB() != null) {
                    leftNode = LLSubNodeGen.create(leftUnboxed, rightUnboxed);
                } else {
                    throw new LLParseError(source, ctx, "Invalid term operator: " + operatorCtx.getText());
                }
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitTerm(LazyLanguageParser.TermContext ctx) {
        LLExpressionNode leftNode = null;
        int index = 0;
        for (final LazyLanguageParser.FactorContext factorCtx : ctx.factor()) {
            final LLExpressionNode rightNode = (LLExpressionNode) visit(factorCtx);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LazyLanguageParser.FactorOperatorContext operatorCtx = ctx.factorOperator(index++);
                final LLExpressionNode leftUnboxed = LLUnboxNodeGen.create(leftNode);
                final LLExpressionNode rightUnboxed = LLUnboxNodeGen.create(rightNode);
                if (operatorCtx.MUL() != null) {
                    leftNode = LLMulNodeGen.create(leftUnboxed, rightUnboxed);
                } else if (operatorCtx.DIV() != null) {
                    leftNode = LLDivNodeGen.create(leftUnboxed, rightUnboxed);
                } else {
                    throw new LLParseError(source, ctx, "Invalid factor operator: " + operatorCtx.getText());
                }
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitFactor(LazyLanguageParser.FactorContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            LLExpressionNode assignmentName = createStringLiteral(ctx.IDENTIFIER().getSymbol(), false);
            if (ctx.memberExpression() != null) {
                return createMemberExpression(ctx.memberExpression(), null, null, assignmentName);
            } else {
                return createRead(assignmentName);
            }
        } else if (ctx.STRING_LITERAL() != null) {
            return createStringLiteral(ctx.STRING_LITERAL().getSymbol(), true);
        } else if (ctx.NUMERIC_LITERAL() != null) {
            return createNumericLiteral(ctx.NUMERIC_LITERAL().getSymbol());
        }
        int start = ctx.start.getStartIndex();
        int length = ctx.stop.getStopIndex() - start + 1;
        LLExpressionNode expressionNode = (LLExpressionNode) visit(ctx.expression());
        return createParenExpression(expressionNode, start, length);
    }

    public LLExpressionNode createMemberExpression(LazyLanguageParser.MemberExpressionContext ctx,
            LLExpressionNode receiver, LLExpressionNode assignmentReceiver, LLExpressionNode assignmentName) {
        if (ctx.LPAREN() != null) {
            return createCallMemberExpression(ctx, receiver, assignmentReceiver, assignmentName);
        }
        if (ctx.DOT() != null) {
            return createDotMemberExpression(ctx, receiver, assignmentName);
        }
        if (ctx.LBRACK() != null) {
            return createArrayMemberExpression(ctx, receiver, assignmentName);
        }
        throw new LLParseError(source, ctx, "Invalid member expression");
    }

    public LLExpressionNode createCallMemberExpression(LazyLanguageParser.MemberExpressionContext ctx,
            LLExpressionNode r, LLExpressionNode functionReceiver, LLExpressionNode functionName) {
        LLExpressionNode receiver;
        if (functionReceiver == null) {
            throw new NotImplementedException();
            //receiver = new LLReadArgumentNode(0);
        } else {
            receiver = functionReceiver;
        }
        List<LLExpressionNode> parameters = new ArrayList<>();
        parameters.add(receiver);
        if (ctx.parameterList() != null) {
            for (LazyLanguageParser.ExpressionContext expression : ctx.parameterList().expression()) {
                parameters.add((LLExpressionNode) visit(expression));
            }
        }
        LLExpressionNode result = createCall(functionName, parameters, ctx.RPAREN().getSymbol());
        if (ctx.memberExpression() != null) {
            return createMemberExpression(ctx.memberExpression(), result, receiver, null);
        }
        return result;
    }

    public LLExpressionNode createAssignmentMemberExpression(LazyLanguageParser.MemberExpressionContext ctx,
            LLExpressionNode receiver, LLExpressionNode assignmentReceiver, LLExpressionNode assignmentName) {
        if (assignmentName == null) {
            throw new LLParseError(source, ctx.expression(), "invalid assignment target");
        }
        LLExpressionNode result = (LLExpressionNode) visit(ctx.expression());
        if (assignmentReceiver == null) {
            result = createAssignment(assignmentName, result);
        } else {
            result = createWriteProperty(assignmentReceiver, assignmentName, result);
        }
        if (ctx.memberExpression() != null) {
            return createMemberExpression(ctx.memberExpression(), result, receiver, null);
        }
        return result;
    }

    public LLExpressionNode createDotMemberExpression(LazyLanguageParser.MemberExpressionContext ctx,
            LLExpressionNode r, LLExpressionNode assignmentName) {
        LLExpressionNode receiver = r == null ? createRead(assignmentName) : r;
        LLExpressionNode nestedAssignmentName = createStringLiteral(ctx.IDENTIFIER().getSymbol(), false);
        LLExpressionNode result = createReadProperty(receiver, nestedAssignmentName);
        if (ctx.memberExpression() != null) {
            return createMemberExpression(ctx.memberExpression(), result, receiver, nestedAssignmentName);
        }
        return result;
    }

    public LLExpressionNode createArrayMemberExpression(LazyLanguageParser.MemberExpressionContext ctx,
            LLExpressionNode r, LLExpressionNode assignmentName) {
        LLExpressionNode receiver = r == null ? createRead(assignmentName) : r;
        LLExpressionNode nestedAssignmentName = (LLExpressionNode) visit(ctx.expression());
        LLExpressionNode result = createReadProperty(receiver, nestedAssignmentName);
        if (ctx.memberExpression() != null) {
            return createMemberExpression(ctx.memberExpression(), result, receiver, nestedAssignmentName);
        }
        return result;
    }

    /**
     * Returns an {@link LLInvokeNode} for the given parameters.
     *
     * @param functionNode   The function being called
     * @param parameterNodes The parameters of the function call
     * @param finalToken     A token used to determine the end of the
     *                       sourceSelection for this call
     * @return An LLInvokeNode for the given parameters. null if functionNode or any
     *         of the parameterNodes are null.
     */
    public LLExpressionNode createCall(LLExpressionNode functionName, List<LLExpressionNode> parameterNodes,
            Token finalToken) {
        if (functionName == null || containsNull(parameterNodes)) {
            return null;
        }

        final LLExpressionNode result = new LLInvokeNode(functionName,
                parameterNodes.toArray(new LLExpressionNode[parameterNodes.size()]));

        final int startPos = functionName.getSourceCharIndex();
        final int endPos = finalToken.getStartIndex() + finalToken.getText().length();
        result.setSourceSection(startPos, endPos - startPos);
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns an {@link LLWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode  The name of the variable being assigned
     * @param valueNode The value to be assigned
     * @return An LLExpressionNode for the given parameters. null if nameNode or
     *         valueNode is null.
     */
    public LLExpressionNode createAssignment(LLExpressionNode nameNode, LLExpressionNode valueNode) {
        return createAssignment(nameNode, valueNode, null);
    }

    /**
     * Returns an {@link LLWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode      The name of the variable being assigned
     * @param valueNode     The value to be assigned
     * @param argumentIndex null or index of the argument the assignment is
     *                      assigning
     * @return An LLExpressionNode for the given parameters. null if nameNode or
     *         valueNode is null.
     */
    public LLExpressionNode createAssignment(LLExpressionNode nameNode, LLExpressionNode valueNode,
            Integer argumentIndex) {
        if (nameNode == null || valueNode == null) {
            return null;
        }

        String name = ((LLStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(name, argumentIndex, FrameSlotKind.Illegal);
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
     * Returns a {@link LLReadLocalVariableNode} if this read is a local variable or
     * a {@link LLFunctionLiteralNode} if this read is global. In Lazy, the only
     * global names are functions.
     *
     * @param nameNode The name of the variable/function being read
     * @return either:
     *         <ul>
     *         <li>A LLReadLocalVariableNode representing the local variable being
     *         read.</li>
     *         <li>A LLFunctionLiteralNode representing the function
     *         definition.</li>
     *         <li>null if nameNode is null.</li>
     *         </ul>
     */
    public LLExpressionNode createRead(LLExpressionNode nameNode) {
        if (nameNode == null) {
            return null;
        }

        String name = ((LLStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = lexicalScope.locals.get(name);
        final LLExpressionNode result;
        if (frameSlot != null) {
            result = LLReadLocalVariableNodeGen.create(frameSlot);
        } else {
            frameSlot = lexicalScope.locals.get(LexicalScope.THIS);
            final LLExpressionNode thisNode = LLReadLocalVariableNodeGen.create(frameSlot);
            result = LLReadPropertyNodeGen.create(thisNode, nameNode);
        }
        result.setSourceSection(nameNode.getSourceCharIndex(), nameNode.getSourceLength());
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitBreakStatement(LazyLanguageParser.BreakStatementContext ctx) {
        if (lexicalScope.inLoop) {
            final LLBreakNode breakNode = new LLBreakNode();
            setSourceFromContext(breakNode, ctx);
            return breakNode;
        }
        throw new LLParseError(source, ctx, "break used outside of loop");
    }

    @Override
    public Node visitContinueStatement(LazyLanguageParser.ContinueStatementContext ctx) {
        if (lexicalScope.inLoop) {
            final LLContinueNode continueNode = new LLContinueNode();
            setSourceFromContext(continueNode, ctx);
            return continueNode;
        }
        throw new LLParseError(source, ctx, "continue used outside of loop");
    }

    @Override
    public Node visitWhileStatement(LazyLanguageParser.WhileStatementContext ctx) {
        LLExpressionNode conditionNode = (LLExpressionNode) visit(ctx.condition);

        pushScope(true);
        LLStatementNode blockNode = (LLStatementNode) visit(ctx.block());

        if (conditionNode == null || blockNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final LLWhileNode whileNode = new LLWhileNode(conditionNode, blockNode);
        setSourceFromContext(whileNode, ctx);
        return whileNode;
    }

    @Override
    public Node visitIfStatement(LazyLanguageParser.IfStatementContext ctx) {
        LLExpressionNode conditionNode = (LLExpressionNode) visit(ctx.condition);

        pushScope(lexicalScope.inLoop);
        LLStatementNode thenPartNode = (LLStatementNode) visit(ctx.then);

        LLStatementNode elsePartNode = null;
        if (ctx.ELSE() != null) {
            pushScope(lexicalScope.inLoop);
            elsePartNode = (LLStatementNode) visit(ctx.block(1));
        }

        if (conditionNode == null || thenPartNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final LLIfNode ifNode = new LLIfNode(conditionNode, thenPartNode, elsePartNode);
        setSourceFromContext(ifNode, ctx);
        return ifNode;
    }

    @Override
    public Node visitReturnStatement(LazyLanguageParser.ReturnStatementContext ctx) {
        LLExpressionNode valueNode = null;
        if (ctx.expression() != null) {
            valueNode = (LLExpressionNode) visit(ctx.expression());
        }
        final LLReturnNode returnNode = new LLReturnNode(valueNode);
        setSourceFromContext(returnNode, ctx);
        return returnNode;
    }

    public LLExpressionNode createStringLiteral(Token literalToken, boolean removeQuotes) {
        /* Remove the trailing and ending " */
        String literal = literalToken.getText();
        final LLExpressionNode result = createStringLiteral(literal, removeQuotes);
        srcFromToken(result, literalToken);
        return result;
    }

    public LLExpressionNode createStringLiteral(String literal, boolean removeQuotes) {
        /* Remove the trailing and ending " */
        if (removeQuotes) {
            assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
            literal = literal.substring(1, literal.length() - 1);
        }

        final LLStringLiteralNode result = new LLStringLiteralNode(literal.intern());
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
     * @param nameNode     The name of the property being accessed
     * @return An LLExpressionNode for the given parameters. null if receiverNode or
     *         nameNode is null.
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
     * @param nameNode     The name of the property being assigned
     * @param valueNode    The value to be assigned
     * @return An LLExpressionNode for the given parameters. null if receiverNode,
     *         nameNode or valueNode is null.
     */
    public LLExpressionNode createWriteProperty(LLExpressionNode receiverNode, LLExpressionNode nameNode,
            LLExpressionNode valueNode) {
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
