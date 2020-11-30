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
package com.guillermomolina.lazyscript.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.guillermomolina.lazyscript.LazyScriptLanguage;
import com.guillermomolina.lazyscript.NotImplementedException;
import com.guillermomolina.lazyscript.nodes.LSExpressionNode;
import com.guillermomolina.lazyscript.nodes.LSRootNode;
import com.guillermomolina.lazyscript.nodes.LSStatementNode;
import com.guillermomolina.lazyscript.nodes.arithmetic.LSAddNodeGen;
import com.guillermomolina.lazyscript.nodes.arithmetic.LSDivNodeGen;
import com.guillermomolina.lazyscript.nodes.arithmetic.LSMulNodeGen;
import com.guillermomolina.lazyscript.nodes.arithmetic.LSSubNodeGen;
import com.guillermomolina.lazyscript.nodes.controlflow.LSBlockNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSBreakNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSContinueNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSFunctionBodyNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSIfNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSReturnNode;
import com.guillermomolina.lazyscript.nodes.controlflow.LSWhileNode;
import com.guillermomolina.lazyscript.nodes.expression.LSInvokeFunctionNode;
import com.guillermomolina.lazyscript.nodes.expression.LSInvokeMethodNode;
import com.guillermomolina.lazyscript.nodes.expression.LSParenExpressionNode;
import com.guillermomolina.lazyscript.nodes.literals.LSArrayLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSBigIntegerLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSFunctionLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSLongLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSStringLiteralNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadArgumentNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadLocalVariableNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadLocalVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.local.LSWriteLocalVariableNode;
import com.guillermomolina.lazyscript.nodes.local.LSWriteLocalVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSEqualNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLessOrEqualNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLessThanNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalAndNode;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalNotNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalOrNode;
import com.guillermomolina.lazyscript.nodes.property.LSReadPropertyNode;
import com.guillermomolina.lazyscript.nodes.property.LSReadPropertyNodeGen;
import com.guillermomolina.lazyscript.nodes.property.LSWritePropertyNode;
import com.guillermomolina.lazyscript.nodes.property.LSWritePropertyNodeGen;
import com.guillermomolina.lazyscript.nodes.util.LSUnboxNodeGen;
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
public class LSParserVisitor extends LazyScriptParserBaseVisitor<Node> {

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
        protected final List<LSStatementNode> statementNodes;
        FrameDescriptor frameDescriptor;

        LexicalScope(LexicalScope outer, boolean inLoop) {
            this.outer = outer;
            this.inLoop = inLoop;
            this.locals = new HashMap<>();
            this.statementNodes = new ArrayList<>();
            this.frameDescriptor = new FrameDescriptor();

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
            throw new LSParseError(source, token, msg);
        }
    }

    private LexicalScope lexicalScope;
    private final LazyScriptLanguage language;
    private final Source source;

    public LSParserVisitor(LazyScriptLanguage language, Source source) {
        this.language = language;
        this.source = source;
    }

    public RootCallTarget parse() {
        LazyScriptLexer lexer = new LazyScriptLexer(CharStreams.fromString(source.getCharacters().toString()));
        LazyScriptParser parser = new LazyScriptParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        BailoutErrorListener listener = new BailoutErrorListener(source);
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
        RootNode rootNode = (RootNode) visit(parser.module());
        return Truffle.getRuntime().createCallTarget(rootNode);
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

    private void setSourceFromContext(LSStatementNode node, ParserRuleContext ctx) {
        assert ctx != null;
        if (node == null) {
            throw new LSParseError(source, ctx, "Node is null");
        }
        Interval sourceInterval = ctx.getSourceInterval();
        if (sourceInterval != null) {
            node.setSourceSection(sourceInterval.a, sourceInterval.length());
        }
    }

    public void pushScope(boolean inLoop) {
        lexicalScope = new LexicalScope(lexicalScope, inLoop);
    }

    public void popScope() {
        lexicalScope = lexicalScope.outer;
    }

    @Override
    public Node visitModule(LazyScriptParser.ModuleContext ctx) {
        assert lexicalScope == null;

        /* State while parsing a function. */
        pushScope(false);

        final LSReadArgumentNode readArg0 = new LSReadArgumentNode(0);
        final LSExpressionNode stringLiteral = createStringLiteral(LexicalScope.THIS, false);
        LSExpressionNode assignment = createAssignment(stringLiteral, readArg0, 0);
        lexicalScope.statementNodes.add(assignment);

        List<LSStatementNode> bodyNodes = lexicalScope.statementNodes;

        for (LazyScriptParser.StatementContext statement : ctx.statement()) {
            bodyNodes.add((LSStatementNode) visit(statement));
        }

        FrameDescriptor frameDescriptor = lexicalScope.frameDescriptor;
        popScope();

        if (containsNull(bodyNodes)) {
            return null;
        }

        List<LSStatementNode> flattenedNodes = new ArrayList<>(bodyNodes.size());
        flattenBlocks(bodyNodes, flattenedNodes);
        for (LSStatementNode statement : flattenedNodes) {
            if (statement.hasSource() && !isHaltInCondition(statement)) {
                statement.addStatementTag();
            }
        }
        LSStatementNode methodBlock = new LSBlockNode(
                flattenedNodes.toArray(new LSStatementNode[flattenedNodes.size()]));

        setSourceFromContext(methodBlock, ctx);
        assert lexicalScope == null : "Wrong scoping of blocks in parser";

        final LSFunctionBodyNode functionBodyNode = new LSFunctionBodyNode(methodBlock);
        setSourceFromContext(functionBodyNode, ctx);
        final LSRootNode rootNode = new LSRootNode(language, frameDescriptor, functionBodyNode,
                functionBodyNode.getSourceSection());

        return rootNode;
    }

    @Override
    public Node visitFunctionStatement(LazyScriptParser.FunctionStatementContext ctx) {
        pushScope(false);
        
        LSReadArgumentNode readArg = new LSReadArgumentNode(0);
        LSExpressionNode stringLiteral = createStringLiteral(LexicalScope.THIS, false);
        LSExpressionNode assignment = createAssignment(stringLiteral, readArg, 0);
        lexicalScope.statementNodes.add(assignment);
        int parameterCount = 1;
        if (ctx.functionParameters() != null) {
            for (TerminalNode nameNode : ctx.functionParameters().IDENTIFIER()) {
                readArg = new LSReadArgumentNode(parameterCount);
                stringLiteral = createStringLiteral(nameNode.getText(), false);
                assignment = createAssignment(stringLiteral, readArg, parameterCount);
                lexicalScope.statementNodes.add(assignment);
                parameterCount++;
            }
        }

        final LSStatementNode methodBlock = (LSStatementNode) visit(ctx.block());
        if (methodBlock == null) {
            throw new NotImplementedException();
        }

        FrameDescriptor frameDescriptor = lexicalScope.frameDescriptor;
        popScope();

        final LSFunctionBodyNode functionBodyNode = new LSFunctionBodyNode(methodBlock);
        final int functionStartPos = methodBlock.getSourceCharIndex();
        final int bodyEndPos = methodBlock.getSourceEndIndex();
        SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
        functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());
        RootNode rootNode = new LSRootNode(language, frameDescriptor, functionBodyNode, functionSrc);
        LSExpressionNode function = new LSFunctionLiteralNode(Truffle.getRuntime().createCallTarget(rootNode));
        setSourceFromContext(function, ctx);
        function.addExpressionTag();

        final LSExpressionNode nameNode = createStringLiteral(ctx.IDENTIFIER().getText(), false);
        LSExpressionNode result = createAssignment(nameNode, function);
        result.addStatementTag();
        return result;
    }

    @Override
    public Node visitFunctionExpression(LazyScriptParser.FunctionExpressionContext ctx) {
        pushScope(false);

        LSReadArgumentNode readArg = new LSReadArgumentNode(0);
        LSExpressionNode stringLiteral = createStringLiteral(LexicalScope.THIS, false);
        LSExpressionNode assignment = createAssignment(stringLiteral, readArg, 0);
        lexicalScope.statementNodes.add(assignment);
        int parameterCount = 1;
        if (ctx.functionParameters() != null) {
            for (TerminalNode nameNode : ctx.functionParameters().IDENTIFIER()) {
                readArg = new LSReadArgumentNode(parameterCount);
                stringLiteral = createStringLiteral(nameNode.getText(), false);
                assignment = createAssignment(stringLiteral, readArg, parameterCount);
                lexicalScope.statementNodes.add(assignment);
                parameterCount++;
            }
        }

        final LSStatementNode methodBlock = (LSStatementNode) visit(ctx.block());
        if (methodBlock == null) {
            throw new NotImplementedException();
        }

        FrameDescriptor frameDescriptor = lexicalScope.frameDescriptor;
        popScope();

        final LSFunctionBodyNode functionBodyNode = new LSFunctionBodyNode(methodBlock);
        final int functionStartPos = methodBlock.getSourceCharIndex();
        final int bodyEndPos = methodBlock.getSourceEndIndex();
        SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
        functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());
        RootNode rootNode = new LSRootNode(language, frameDescriptor, functionBodyNode, functionSrc);
        LSExpressionNode result = new LSFunctionLiteralNode(Truffle.getRuntime().createCallTarget(rootNode));
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitBlock(LazyScriptParser.BlockContext ctx) {
        List<LSStatementNode> bodyNodes = lexicalScope.statementNodes;

        for (LazyScriptParser.StatementContext statement : ctx.statement()) {
            bodyNodes.add((LSStatementNode) visit(statement));
        }

        if (containsNull(bodyNodes)) {
            return null;
        }

        List<LSStatementNode> flattenedNodes = new ArrayList<>(bodyNodes.size());
        flattenBlocks(bodyNodes, flattenedNodes);
        for (LSStatementNode statement : flattenedNodes) {
            if (statement.hasSource() && !isHaltInCondition(statement)) {
                statement.addStatementTag();
            }
        }
        LSBlockNode blockNode = new LSBlockNode(flattenedNodes.toArray(new LSStatementNode[flattenedNodes.size()]));
        setSourceFromContext(blockNode, ctx);
        return blockNode;
    }

    private static boolean isHaltInCondition(LSStatementNode statement) {
        return (statement instanceof LSIfNode) || (statement instanceof LSWhileNode);
    }

    private void flattenBlocks(Iterable<? extends LSStatementNode> bodyNodes, List<LSStatementNode> flattenedNodes) {
        for (LSStatementNode n : bodyNodes) {
            if (n instanceof LSBlockNode) {
                flattenBlocks(((LSBlockNode) n).getStatements(), flattenedNodes);
            } else {
                flattenedNodes.add(n);
            }
        }
    }

    @Override
    public Node visitStatement(LazyScriptParser.StatementContext ctx) {
        // Tricky: avoid calling visit on ctx.SEMI()
        if (ctx.getChild(0) != null && ctx.getChild(0) != ctx.SEMI()) {
            return visit(ctx.getChild(0));
        }
        throw new LSParseError(source, ctx, "Malformed statement");
    }

    @Override
    public Node visitExpression(LazyScriptParser.ExpressionContext ctx) {
        LSExpressionNode leftNode = null;
        for (final LazyScriptParser.LogicTermContext context : ctx.logicTerm()) {
            final LSExpressionNode rightNode = (LSExpressionNode) visit(context);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create(leftNode);
                final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create(rightNode);
                leftNode = new LSLogicalOrNode(leftUnboxed, rightUnboxed);
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitLogicTerm(LazyScriptParser.LogicTermContext ctx) {
        LSExpressionNode leftNode = null;
        for (final LazyScriptParser.LogicFactorContext context : ctx.logicFactor()) {
            final LSExpressionNode rightNode = (LSExpressionNode) visit(context);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create(leftNode);
                final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create(rightNode);
                leftNode = new LSLogicalAndNode(leftUnboxed, rightUnboxed);
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitLogicFactor(LazyScriptParser.LogicFactorContext ctx) {
        LSExpressionNode leftNode = (LSExpressionNode) visit(ctx.left);
        if (ctx.op != null) {
            final LSExpressionNode rightNode = (LSExpressionNode) visit(ctx.right);
            final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create(leftNode);
            final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create(rightNode);
            final String operator = ctx.op.getText();
            switch (operator) {
                case "<":
                    leftNode = LSLessThanNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case "<=":
                    leftNode = LSLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case ">":
                    leftNode = LSLogicalNotNodeGen.create(LSLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                case ">=":
                    leftNode = LSLogicalNotNodeGen.create(LSLessThanNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                case "==":
                    leftNode = LSEqualNodeGen.create(leftUnboxed, rightUnboxed);
                    break;
                case "!=":
                    leftNode = LSLogicalNotNodeGen.create(LSEqualNodeGen.create(leftUnboxed, rightUnboxed));
                    break;
                default:
                    throw new LSParseError(source, ctx, "Invalid logic operator: " + operator);
            }
            setSourceFromContext(leftNode, ctx);
            leftNode.addExpressionTag();
        }
        return leftNode;
    }

    @Override
    public Node visitArithmetic(LazyScriptParser.ArithmeticContext ctx) {
        LSExpressionNode leftNode = null;
        int index = 0;
        for (final LazyScriptParser.TermContext termCtx : ctx.term()) {
            final LSExpressionNode rightNode = (LSExpressionNode) visit(termCtx);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LazyScriptParser.TermOperatorContext operatorCtx = ctx.termOperator(index++);
                final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create(leftNode);
                final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create(rightNode);
                if (operatorCtx.ADD() != null) {
                    leftNode = LSAddNodeGen.create(leftUnboxed, rightUnboxed);
                } else if (operatorCtx.SUB() != null) {
                    leftNode = LSSubNodeGen.create(leftUnboxed, rightUnboxed);
                } else {
                    throw new LSParseError(source, ctx, "Invalid term operator: " + operatorCtx.getText());
                }
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitTerm(LazyScriptParser.TermContext ctx) {
        LSExpressionNode leftNode = null;
        int index = 0;
        for (final LazyScriptParser.FactorContext factorCtx : ctx.factor()) {
            final LSExpressionNode rightNode = (LSExpressionNode) visit(factorCtx);
            if (leftNode == null) {
                leftNode = rightNode;
            } else {
                final LazyScriptParser.FactorOperatorContext operatorCtx = ctx.factorOperator(index++);
                final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create(leftNode);
                final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create(rightNode);
                if (operatorCtx.MUL() != null) {
                    leftNode = LSMulNodeGen.create(leftUnboxed, rightUnboxed);
                } else if (operatorCtx.DIV() != null) {
                    leftNode = LSDivNodeGen.create(leftUnboxed, rightUnboxed);
                } else {
                    throw new LSParseError(source, ctx, "Invalid factor operator: " + operatorCtx.getText());
                }
                setSourceFromContext(leftNode, ctx);
                leftNode.addExpressionTag();
            }
        }
        return leftNode;
    }

    @Override
    public Node visitSingleExpression(LazyScriptParser.SingleExpressionContext ctx) {
        LSExpressionNode receiver = null;
        LSExpressionNode assignmentName = null;
        if (ctx.IDENTIFIER() != null) {
            assignmentName = createStringLiteral(ctx.IDENTIFIER().getText(), false);
            if (ctx.member() == null) {
                return createRead(ctx.member(), assignmentName);
            }
        } else if (ctx.stringLiteral() != null) {
            receiver = (LSExpressionNode) visit(ctx.stringLiteral());
        } else if (ctx.numericLiteral() != null) {
            receiver = (LSExpressionNode) visit(ctx.numericLiteral());
        } else if (ctx.arrayLiteral() != null) {
            receiver = (LSExpressionNode) visit(ctx.arrayLiteral());
        } else if (ctx.functionExpression() != null) {
            receiver = (LSExpressionNode) visit(ctx.functionExpression());
        } else {// esle ctx.parenExpression()
            receiver = createParenExpression(ctx.parenExpression());
        }
        if (ctx.member() == null) {
            return receiver;
        }
        return createMember(ctx.member(), receiver, null, assignmentName);
    }

    public LSExpressionNode createMember(LazyScriptParser.MemberContext ctx, LSExpressionNode receiver,
            LSExpressionNode assignmentReceiver, LSExpressionNode assignmentName) {
        if (ctx.LPAREN() != null) {
            return createCallMember(ctx, receiver, assignmentReceiver, assignmentName);
        }
        if (ctx.DOT() != null) {
            return createDotMember(ctx, receiver, assignmentName);
        }
        if (ctx.LBRACK() != null) {
            return createArrayMember(ctx, receiver, assignmentName);
        }
        throw new LSParseError(source, ctx, "Invalid member expression");
    }

    public LSExpressionNode createParenExpression(LazyScriptParser.ParenExpressionContext ctx) {
        LSExpressionNode expressionNode = (LSExpressionNode) visit(ctx.expression());
        if (expressionNode == null) {
            return null;
        }

        int start = ctx.start.getStartIndex();
        int length = ctx.stop.getStopIndex() - start + 1;
        final LSParenExpressionNode result = new LSParenExpressionNode(expressionNode);
        result.setSourceSection(start, length);
        return result;
    }

    public LSExpressionNode createCallMember(LazyScriptParser.MemberContext ctx, LSExpressionNode r,
            LSExpressionNode functionReceiver, LSExpressionNode functionName) {
        LSExpressionNode receiver;
        if (functionReceiver == null) {
            FrameSlot frameSlot = lexicalScope.locals.get(LexicalScope.THIS);
            receiver = LSReadLocalVariableNodeGen.create(frameSlot);
        } else {
            receiver = functionReceiver;
        }
        List<LSExpressionNode> parameters = new ArrayList<>();
        parameters.add(receiver);
        if (ctx.parameterList() != null) {
            for (LazyScriptParser.ExpressionContext expression : ctx.parameterList().expression()) {
                parameters.add((LSExpressionNode) visit(expression));
            }
        }
        LSExpressionNode result;
        if(r == null) {
            LSExpressionNode functionNode = createRead(ctx, functionName);
            result = createCallFunction(ctx, functionNode, parameters);
        } else {
            //LSExpressionNode functionNode = createReadProperty(ctx, r, functionName);
            //result = createCallFunction(ctx, r, parameters);
            result = createCallMethod(ctx, functionName, parameters);
        }
        if (ctx.member() != null) {
            return createMember(ctx.member(), result, receiver, null);
        }
        return result;
    }

    /**
     * Returns an {@link LSInvokeMethodNode} for the given parameters.
     *
     * @param functionName   The function being called
     * @param parameterNodes The parameters of the function call
     * @return An LSInvokeMethodNode for the given parameters. null if functionNode or any
     *         of the parameterNodes are null.
     */
    public LSExpressionNode createCallMethod(LazyScriptParser.MemberContext ctx, LSExpressionNode functionNameNode,
            List<LSExpressionNode> parameterNodes) {
        if (functionNameNode == null || containsNull(parameterNodes)) {
            return null;
        }

        final LSExpressionNode result = new LSInvokeMethodNode(functionNameNode,
                parameterNodes.toArray(new LSExpressionNode[parameterNodes.size()]));

        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    public LSExpressionNode createCallFunction(LazyScriptParser.MemberContext ctx, LSExpressionNode functionNode,
            List<LSExpressionNode> parameterNodes) {
        if (functionNode == null || containsNull(parameterNodes)) {
            return null;
        }

        final LSExpressionNode result = new LSInvokeFunctionNode(functionNode,
                parameterNodes.toArray(new LSExpressionNode[parameterNodes.size()]));

        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    public LSExpressionNode createDotMember(LazyScriptParser.MemberContext ctx, LSExpressionNode r,
            LSExpressionNode assignmentName) {
        LSExpressionNode receiver = r == null ? createRead(ctx, assignmentName) : r;
        LSExpressionNode nestedAssignmentName = createStringLiteral(ctx.IDENTIFIER().getText(), false);
        LSExpressionNode result = createReadProperty(ctx, receiver, nestedAssignmentName);
        if (ctx.member() != null) {
            return createMember(ctx.member(), result, receiver, nestedAssignmentName);
        }
        return result;
    }

    public LSExpressionNode createArrayMember(LazyScriptParser.MemberContext ctx, LSExpressionNode r,
            LSExpressionNode assignmentName) {
        LSExpressionNode receiver = r == null ? createRead(ctx, assignmentName) : r;
        LSExpressionNode nestedAssignmentName = (LSExpressionNode) visit(ctx.expression());
        LSExpressionNode result = createReadProperty(ctx, receiver, nestedAssignmentName);
        if (ctx.member() != null) {
            return createMember(ctx.member(), result, receiver, nestedAssignmentName);
        }
        return result;
    }

    /**
     * Returns an {@link LSReadPropertyNode} for the given parameters.
     *
     * @param receiverNode The receiver of the property access
     * @param nameNode     The name of the property being accessed
     * @return An LSExpressionNode for the given parameters. null if receiverNode or
     *         nameNode is null.
     */
    public LSExpressionNode createReadProperty(LazyScriptParser.MemberContext ctx, LSExpressionNode receiverNode,
            LSExpressionNode nameNode) {
        if (receiverNode == null || nameNode == null) {
            return null;
        }

        final LSExpressionNode result = LSReadPropertyNodeGen.create(receiverNode, nameNode);
        setSourceFromContext(result, ctx);
        result.addExpressionTag();

        return result;
    }

    @Override
    public Node visitAssignment(LazyScriptParser.AssignmentContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            LSExpressionNode assignmentName = createStringLiteral(ctx.IDENTIFIER().getText(), false);
            return createAssignmentMember(ctx, null, assignmentName);
        } // else ctx.singleExpression() != null
        LSExpressionNode receiver = (LSExpressionNode) visit(ctx.singleExpression());
        if (ctx.assignableMember().IDENTIFIER() != null) {
            LSExpressionNode assignmentName = createStringLiteral(ctx.assignableMember().IDENTIFIER().getText(),
                    false);
            return createAssignmentMember(ctx, receiver, assignmentName);
        } // ctx.assignableMember().expression() != null
        LSExpressionNode assignmentName = (LSExpressionNode) visit(ctx.assignableMember().expression());
        return createAssignmentMember(ctx, receiver, assignmentName);
    }

    public LSExpressionNode createAssignmentMember(LazyScriptParser.AssignmentContext ctx,
            LSExpressionNode assignmentReceiver, LSExpressionNode assignmentName) {
        if (assignmentName == null) {
            throw new LSParseError(source, ctx.expression(), "invalid assignment target");
        }
        LSExpressionNode result = (LSExpressionNode) visit(ctx.expression());
        if (assignmentReceiver == null) {
            result = createAssignment(assignmentName, result);
        } else {
            result = createWriteProperty(ctx, assignmentReceiver, assignmentName, result);
        }
        return result;
    }

    /**
     * Returns an {@link LSWritePropertyNode} for the given parameters.
     *
     * @param receiverNode The receiver object of the property assignment
     * @param nameNode     The name of the property being assigned
     * @param valueNode    The value to be assigned
     * @return An LSExpressionNode for the given parameters. null if receiverNode,
     *         nameNode or valueNode is null.
     */
    public LSExpressionNode createWriteProperty(LazyScriptParser.AssignmentContext ctx, LSExpressionNode receiverNode,
            LSExpressionNode nameNode, LSExpressionNode valueNode) {
        if (receiverNode == null || nameNode == null || valueNode == null) {
            return null;
        }

        final LSExpressionNode result = LSWritePropertyNodeGen.create(receiverNode, nameNode, valueNode);

        setSourceFromContext(result, ctx);
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns an {@link LSWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode  The name of the variable being assigned
     * @param valueNode The value to be assigned
     * @return An LSExpressionNode for the given parameters. null if nameNode or
     *         valueNode is null.
     */
    public LSExpressionNode createAssignment(LSExpressionNode nameNode, LSExpressionNode valueNode) {
        return createAssignment(nameNode, valueNode, null);
    }

    /**
     * Returns an {@link LSWriteLocalVariableNode} for the given parameters.
     *
     * @param nameNode      The name of the variable being assigned
     * @param valueNode     The value to be assigned
     * @param argumentIndex null or index of the argument the assignment is
     *                      assigning
     * @return An LSExpressionNode for the given parameters. null if nameNode or
     *         valueNode is null.
     */
    public LSExpressionNode createAssignment(LSExpressionNode nameNode, LSExpressionNode valueNode,
            Integer argumentIndex) {
        if (nameNode == null || valueNode == null) {
            return null;
        }

        String name = ((LSStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = lexicalScope.frameDescriptor.findOrAddFrameSlot(name, argumentIndex,
                FrameSlotKind.Illegal);
        lexicalScope.locals.put(name, frameSlot);
        final LSExpressionNode result = LSWriteLocalVariableNodeGen.create(valueNode, frameSlot, nameNode);

        if (nameNode.hasSource() && valueNode.hasSource()) {
            final int start = nameNode.getSourceCharIndex();
            final int length = valueNode.getSourceEndIndex() - start;
            result.setSourceSection(start, length);
        }
        result.addExpressionTag();

        return result;
    }

    /**
     * Returns a {@link LSReadLocalVariableNode} if this read is a local variable or
     * a {@link LSReadPropertyNode} if this read is from the object. In Lazy, there
     * are no global names.
     *
     * @param nameNode The name of the variable/function being read
     * @return either:
     *         <ul>
     *         <li>A LSReadLocalVariableNode representing the local variable being
     *         read.</li>
     *         <li>A LSReadPropertyNode representing the property being read.</li>
     *         <li>null if nameNode is null.</li>
     *         </ul>
     */
    public LSExpressionNode createRead(LazyScriptParser.MemberContext ctx, LSExpressionNode nameNode) {
        if (nameNode == null) {
            return null;
        }

        String name = ((LSStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = lexicalScope.locals.get(name);
        final LSExpressionNode result;
        if (frameSlot != null) {
            result = LSReadLocalVariableNodeGen.create(frameSlot);
            setSourceFromContext(result, ctx);
        } else {
            frameSlot = lexicalScope.locals.get(LexicalScope.THIS);
            final LSExpressionNode thisNode = LSReadLocalVariableNodeGen.create(frameSlot);
            result = LSReadPropertyNodeGen.create(thisNode, nameNode);
        }
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitBreakStatement(LazyScriptParser.BreakStatementContext ctx) {
        if (lexicalScope.inLoop) {
            final LSBreakNode breakNode = new LSBreakNode();
            setSourceFromContext(breakNode, ctx);
            return breakNode;
        }
        throw new LSParseError(source, ctx, "break used outside of loop");
    }

    @Override
    public Node visitContinueStatement(LazyScriptParser.ContinueStatementContext ctx) {
        if (lexicalScope.inLoop) {
            final LSContinueNode continueNode = new LSContinueNode();
            setSourceFromContext(continueNode, ctx);
            return continueNode;
        }
        throw new LSParseError(source, ctx, "continue used outside of loop");
    }

    @Override
    public Node visitWhileStatement(LazyScriptParser.WhileStatementContext ctx) {
        LSExpressionNode conditionNode = (LSExpressionNode) visit(ctx.condition);

        pushScope(true);
        LSStatementNode blockNode = (LSStatementNode) visit(ctx.block());

        if (conditionNode == null || blockNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final LSWhileNode whileNode = new LSWhileNode(conditionNode, blockNode);
        setSourceFromContext(whileNode, ctx);
        return whileNode;
    }

    @Override
    public Node visitIfStatement(LazyScriptParser.IfStatementContext ctx) {
        LSExpressionNode conditionNode = (LSExpressionNode) visit(ctx.condition);

        pushScope(lexicalScope.inLoop);
        LSStatementNode thenPartNode = (LSStatementNode) visit(ctx.then);

        LSStatementNode elsePartNode = null;
        if (ctx.ELSE() != null) {
            pushScope(lexicalScope.inLoop);
            elsePartNode = (LSStatementNode) visit(ctx.block(1));
        }

        if (conditionNode == null || thenPartNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final LSIfNode ifNode = new LSIfNode(conditionNode, thenPartNode, elsePartNode);
        setSourceFromContext(ifNode, ctx);
        return ifNode;
    }

    @Override
    public Node visitReturnStatement(LazyScriptParser.ReturnStatementContext ctx) {
        LSExpressionNode valueNode = null;
        if (ctx.expression() != null) {
            valueNode = (LSExpressionNode) visit(ctx.expression());
        }
        final LSReturnNode returnNode = new LSReturnNode(valueNode);
        setSourceFromContext(returnNode, ctx);
        return returnNode;
    }

    @Override
    public Node visitStringLiteral(LazyScriptParser.StringLiteralContext ctx) {
        final LSExpressionNode result = createStringLiteral(ctx.STRING_LITERAL().getText(), true);
        setSourceFromContext(result, ctx);
        return result;
    }

    public LSExpressionNode createStringLiteral(String literal, boolean removeQuotes) {
        /* Remove the trailing and ending " */
        if (removeQuotes) {
            assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
            literal = literal.substring(1, literal.length() - 1);
        }

        final LSStringLiteralNode result = new LSStringLiteralNode(literal.intern());
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitNumericLiteral(LazyScriptParser.NumericLiteralContext ctx) {
        LSExpressionNode result;
        try {
            /* Try if the literal is small enough to fit into a long value. */
            result = new LSLongLiteralNode(Long.parseLong(ctx.NUMERIC_LITERAL().getText()));
        } catch (NumberFormatException ex) {
            /* Overflow of long value, so fall back to BigInteger. */
            result = new LSBigIntegerLiteralNode(new BigInteger(ctx.NUMERIC_LITERAL().getText()));
        }
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitArrayLiteral(LazyScriptParser.ArrayLiteralContext ctx) {
        List<LSExpressionNode> elementNodes = new ArrayList<>();
        if (ctx.elementList() != null) {
            for (LazyScriptParser.ExpressionContext expression : ctx.elementList().expression()) {
                elementNodes.add((LSExpressionNode) visit(expression));
            }
        }

        final LSArrayLiteralNode result = new LSArrayLiteralNode(elementNodes.toArray(new LSExpressionNode[elementNodes.size()]));
        result.addExpressionTag();
        return result;
    }
}