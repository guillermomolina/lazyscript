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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.guillermomolina.lazyscript.LSLanguage;
import com.guillermomolina.lazyscript.NotImplementedException;
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
import com.guillermomolina.lazyscript.nodes.expression.LSExpressionNode;
import com.guillermomolina.lazyscript.nodes.expression.LSInvokeFunctionNode;
import com.guillermomolina.lazyscript.nodes.expression.LSStatementNode;
import com.guillermomolina.lazyscript.nodes.literals.LSArrayLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSBigIntegerLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSBlockLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSBooleanLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSDecimalLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSFunctionLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSIntegerLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSNullLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSObjectLiteralNode;
import com.guillermomolina.lazyscript.nodes.literals.LSStringLiteralNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadArgumentNode;
import com.guillermomolina.lazyscript.nodes.local.LSReadLocalVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.local.LSReadRemoteVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.local.LSWriteLocalVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.local.LSWriteRemoteVariableNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSEqualNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLessOrEqualNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLessThanNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalAndNode;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalNotNodeGen;
import com.guillermomolina.lazyscript.nodes.logic.LSLogicalOrNode;
import com.guillermomolina.lazyscript.nodes.property.LSInvokePropertyNode;
import com.guillermomolina.lazyscript.nodes.property.LSReadPropertyNodeGen;
import com.guillermomolina.lazyscript.nodes.property.LSWritePropertyNodeGen;
import com.guillermomolina.lazyscript.nodes.root.LSRootNode;
import com.guillermomolina.lazyscript.nodes.util.LSUnboxNodeGen;
import com.guillermomolina.lazyscript.parser.LazyScriptParser.ExpressionContext;
import com.guillermomolina.lazyscript.parser.LazyScriptParser.IdentifierContext;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
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
import org.antlr.v4.runtime.misc.Pair;

/**
 * Helper class used by the LazyScript {@link Parser} to create nodes. The code
 * is factored out of the automatically generated parser to keep the attributed
 * grammar of LazyScript small.
 */
public class LSParserVisitor extends LazyScriptParserBaseVisitor<Node> {
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

    private LSLexicalScope lexicalScope;
    private final LSLanguage language;
    private final Source source;

    public LSParserVisitor(LSLanguage language, Source source) {
        this.language = language;
        this.source = source;
    }

    public LSExpressionNode parse() {
        LazyScriptLexer lexer = new LazyScriptLexer(CharStreams.fromString(source.getCharacters().toString()));
        LazyScriptParser parser = new LazyScriptParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        BailoutErrorListener listener = new BailoutErrorListener(source);
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
        return (LSExpressionNode) visit(parser.module());
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
        if (ctx == null) {
            throw new LSParseError(source, ctx, "Context is null");
        }
        if (node == null) {
            throw new LSParseError(source, ctx, "Node is null");
        }
        int start = ctx.start.getStartIndex();
        int stop = ctx.stop.getStopIndex();
        node.setSourceSection(start, stop - start + 1);
    }

    public void pushScope(boolean inLoop) {
        lexicalScope = new LSLexicalScope(lexicalScope, inLoop);
    }

    public void popScope() {
        lexicalScope = lexicalScope.getOuter();
    }

    @Override
    public Node visitModule(LazyScriptParser.ModuleContext ctx) {
        assert lexicalScope == null;
        pushScope(false);

        final List<LSStatementNode> argumentInitializationNodes = new ArrayList<>();
        argumentInitializationNodes.add(createArgumentInitialization(LSLexicalScope.THIS));

        final LSStatementNode blockNode = createBlock(argumentInitializationNodes, ctx.statement());
        setSourceFromContext(blockNode, ctx);

        FrameDescriptor frameDescriptor = lexicalScope.getFrameDescriptor();
        popScope();
        assert lexicalScope == null : "Wrong scoping of blocks in parser";

        final LSFunctionBodyNode functionBodyNode = new LSFunctionBodyNode(blockNode);
        final int functionStartPos = blockNode.getSourceCharIndex();
        final int bodyEndPos = blockNode.getSourceEndIndex();
        SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
        functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());

        final String name = "main";
        LSRootNode rootNode = new LSRootNode(language, frameDescriptor, functionBodyNode, functionSrc, name);
        RootCallTarget mainCallTarget = Truffle.getRuntime().createCallTarget(rootNode);
        LSExpressionNode result = new LSFunctionLiteralNode(name, mainCallTarget);
        result.addExpressionTag();
        return result;

    }

    private LSExpressionNode createArgumentInitialization(String name) {
        final FrameSlot frameSlot = lexicalScope.addParameter(name);
        int index = (int) frameSlot.getInfo();
        LSReadArgumentNode readArgNode = new LSReadArgumentNode(index);
        LSExpressionNode nameNode = new LSStringLiteralNode(name);
        return LSWriteLocalVariableNodeGen.create(readArgNode, frameSlot, nameNode, true);
    }

    @Override
    public Node visitFunctionLiteral(LazyScriptParser.FunctionLiteralContext ctx) {
        final String functionName = "function";
        LSRootNode rootNode = createRootNode(functionName, LSLexicalScope.THIS, ctx.parameterList(), ctx.block());
        LSExpressionNode functionNode = new LSFunctionLiteralNode(functionName,
                Truffle.getRuntime().createCallTarget(rootNode));
        setSourceFromContext(functionNode, ctx);
        functionNode.addExpressionTag();
        return functionNode;
    }

    @Override
    public Node visitBlockLiteral(LazyScriptParser.BlockLiteralContext ctx) {
        final String name = "anonymous";
        LSRootNode rootNode = createRootNode(name, LSLexicalScope.PARENT_SCOPE, ctx.parameterList(), ctx.block());
        LSFunctionLiteralNode functionNode = new LSFunctionLiteralNode(name,
                Truffle.getRuntime().createCallTarget(rootNode));
        LSExpressionNode result = new LSBlockLiteralNode(functionNode);
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    private LSRootNode createRootNode(final String functionName, final String parameter0Name,
            LazyScriptParser.ParameterListContext parameterListCtx, LazyScriptParser.BlockContext blockCtx) {
        pushScope(false);

        final List<LSStatementNode> argumentInitializationNodes = new ArrayList<>();
        argumentInitializationNodes.add(createArgumentInitialization(parameter0Name));

        if (parameterListCtx != null) {
            for (IdentifierContext identifierCtx : parameterListCtx.identifier()) {
                argumentInitializationNodes.add(createArgumentInitialization(identifierCtx.getText()));
            }
        }

        final LSStatementNode blockNode = createBlock(argumentInitializationNodes, blockCtx.statement());
        setSourceFromContext(blockNode, blockCtx);

        FrameDescriptor frameDescriptor = lexicalScope.getFrameDescriptor();
        popScope();

        final LSFunctionBodyNode functionBodyNode = new LSFunctionBodyNode(blockNode);
        final int functionStartPos = blockNode.getSourceCharIndex();
        final int bodyEndPos = blockNode.getSourceEndIndex();
        SourceSection functionSrc = source.createSection(functionStartPos, bodyEndPos - functionStartPos);
        functionBodyNode.setSourceSection(functionSrc.getCharIndex(), functionSrc.getCharLength());
        return new LSRootNode(language, frameDescriptor, functionBodyNode, functionSrc, functionName);
    }

    @Override
    public Node visitBlock(LazyScriptParser.BlockContext ctx) {
        LSBlockNode result = createBlock(null, ctx.statement());
        setSourceFromContext(result, ctx);
        return result;
    }

    private LSBlockNode createBlock(List<LSStatementNode> initializationNodeList,
            List<LazyScriptParser.StatementContext> statementCtxList) {
        List<LSStatementNode> bodyNodeList = new ArrayList<>();

        if (initializationNodeList != null) {
            bodyNodeList.addAll(initializationNodeList);
        }

        if (statementCtxList.isEmpty()) {
            bodyNodeList.add(createReadThis());
        } else {
            for (LazyScriptParser.StatementContext statementCtx : statementCtxList) {
                bodyNodeList.add((LSStatementNode) visit(statementCtx));
            }
        }

        if (containsNull(bodyNodeList)) {
            throw new UnsupportedOperationException("reached an unimplemented visit method");
        }

        List<LSStatementNode> flattenedNodeList = new ArrayList<>(bodyNodeList.size());
        flattenBlocks(bodyNodeList, flattenedNodeList);
        for (LSStatementNode statement : flattenedNodeList) {
            if (statement.hasSource() && !isHaltInCondition(statement)) {
                statement.addStatementTag();
            }
        }
        return new LSBlockNode(flattenedNodeList.toArray(new LSStatementNode[flattenedNodeList.size()]));
    }

    private static boolean isHaltInCondition(LSStatementNode statement) {
        return (statement instanceof LSIfNode) || (statement instanceof LSWhileNode);
    }

    private void flattenBlocks(Iterable<? extends LSStatementNode> bodyNodeList,
            List<LSStatementNode> flattenedNodeList) {
        for (LSStatementNode n : bodyNodeList) {
            if (n instanceof LSBlockNode) {
                flattenBlocks(((LSBlockNode) n).getStatements(), flattenedNodeList);
            } else {
                flattenedNodeList.add(n);
            }
        }
    }

    @Override
    public Node visitExpressionStatement(LazyScriptParser.ExpressionStatementContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Node visitExpression(LazyScriptParser.ExpressionContext ctx) {
        if (ctx.parenExpression() != null) {
            return visit(ctx.parenExpression().expression());
        }
        if (ctx.identifier() != null) {
            return createIdentifierExpression(ctx);
        }
        if (ctx.index() != null) {
            return createIndexExpression(ctx);
        }
        if (ctx.member() != null) {
            return createMemberExpression(ctx);
        }
        if (ctx.arguments() != null) {
            return createCallExpression(ctx);
        }
        if (ctx.ASSIGN() != null) {
            return createAssignExpression(ctx);
        }
        if (ctx.expression().size() == 1) {
            return createUnaryExpression(ctx);
        }
        if (ctx.expression().size() == 2) {
            return createBinaryExpression(ctx);
        }
        // literals
        return visitChildren(ctx);
    }

    LSExpressionNode createUnaryExpression(LazyScriptParser.ExpressionContext ctx) {
        throw new NotImplementedException();
    }

    LSExpressionNode createBinaryExpression(LazyScriptParser.ExpressionContext ctx) {
        final LSExpressionNode leftUnboxed = LSUnboxNodeGen.create((LSExpressionNode) visit(ctx.expression(0)));
        final LSExpressionNode rightUnboxed = LSUnboxNodeGen.create((LSExpressionNode) visit(ctx.expression(1)));
        final LSExpressionNode result;
        switch (ctx.operator.getType()) {
            case LazyScriptParser.ADD:
                result = LSAddNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.SUB:
                result = LSSubNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.MUL:
                result = LSMulNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.DIV:
                result = LSDivNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.AND:
                result = new LSLogicalAndNode(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.OR:
                result = new LSLogicalOrNode(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.LT:
                result = LSLessThanNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.LE:
                result = LSLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.GT:
                result = LSLogicalNotNodeGen.create(LSLessOrEqualNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            case LazyScriptParser.GE:
                result = LSLogicalNotNodeGen.create(LSLessThanNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            case LazyScriptParser.EQUAL:
                result = LSEqualNodeGen.create(leftUnboxed, rightUnboxed);
                break;
            case LazyScriptParser.NOT_EQUAL:
                result = LSLogicalNotNodeGen.create(LSEqualNodeGen.create(leftUnboxed, rightUnboxed));
                break;
            default:
                throw new LSParseError(source, ctx, "Invalid binary operator: " + ctx.operator.getText());
        }
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    LSExpressionNode createMemberExpression(LazyScriptParser.ExpressionContext ctx) {
        final LSExpressionNode receiverNode = (LSExpressionNode) visit(ctx.expression(0));
        final LSExpressionNode nameNode = (LSExpressionNode) visit(ctx.member().identifier());
        return createReadProperty(ctx, receiverNode, nameNode);
    }

    LSExpressionNode createIndexExpression(LazyScriptParser.ExpressionContext ctx) {
        final LSExpressionNode receiverNode = (LSExpressionNode) visit(ctx.expression(0));
        final LSExpressionNode indexNode = (LSExpressionNode) visit(ctx.index().expression());
        return createReadProperty(ctx, receiverNode, indexNode);
    }

    LSExpressionNode createIdentifierExpression(LazyScriptParser.ExpressionContext ctx) {
        final LSExpressionNode nameNode = (LSExpressionNode) visit(ctx.identifier());
        return createReadVariable(ctx, nameNode);
    }

    public LSExpressionNode createReadProperty(LazyScriptParser.ExpressionContext ctx, LSExpressionNode receiverNode,
            LSExpressionNode nameNode) {
        if (receiverNode == null || nameNode == null) {
            throw new LSParseError(source, ctx, "One of receiverNode or nameNode is null");
        }

        final LSExpressionNode result = LSReadPropertyNodeGen.create(receiverNode, nameNode);
        setSourceFromContext(result, ctx);
        result.addExpressionTag();

        return result;
    }

    public LSExpressionNode createReadVariable(LazyScriptParser.ExpressionContext ctx, LSExpressionNode nameNode) {
        if (nameNode == null) {
           throw new LSParseError(source, ctx, "Name node is null");
        }

        String name = ((LSStringLiteralNode) nameNode).executeGeneric(null);
        Pair<Integer, FrameSlot> variable = lexicalScope.getVariable(name);
        int scopeDepth = variable.a;
        FrameSlot frameSlot = variable.b;
        final LSExpressionNode result;
        if (frameSlot != null) {
            if (scopeDepth == 0) {
                result = LSReadLocalVariableNodeGen.create(frameSlot);
            } else {
                result = LSReadRemoteVariableNodeGen.create(frameSlot, scopeDepth);
            }
        } else {
            if (name.equals(LSLexicalScope.THIS)) {
                throw new UnsupportedOperationException("There is no this variable");
            }
            // There is no variable with that name, try the property "this.name"
            result = LSReadPropertyNodeGen.create(createReadThis(), nameNode);
        }
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    LSExpressionNode createCallExpression(LazyScriptParser.ExpressionContext ctx) {
        final ExpressionContext receivcCtx = ctx.expression(0);
        if (receivcCtx.member() != null) {
            final LSExpressionNode receiverNode = (LSExpressionNode) visit(receivcCtx.expression(0));
            final LSExpressionNode nameNode = (LSExpressionNode) visit(receivcCtx.member().identifier());
            return createCall(ctx, receiverNode, nameNode);
        }
        if (receivcCtx.index() != null) {
            final LSExpressionNode receiverNode = (LSExpressionNode) visit(receivcCtx.expression(0));
            final LSExpressionNode indexNode = (LSExpressionNode) visit(receivcCtx.index().expression());
            return createCall(ctx, receiverNode, indexNode);
        }
        if (receivcCtx.identifier() != null) {
            final LSExpressionNode nameNode = (LSExpressionNode) visit(receivcCtx.identifier());
            return createCall(ctx, null, nameNode);
        }
        throw new LSParseError(source, ctx, "Unsuported call expression");
    }

    public LSExpressionNode createCall(LazyScriptParser.ExpressionContext ctx, LSExpressionNode r,
            LSExpressionNode functionNameNode) {
        if (functionNameNode == null) {
            throw new LSParseError(source, ctx, "invalid function target");
        }
        List<LSExpressionNode> argumentNodeList = new ArrayList<>();
        if (ctx.arguments().argumentList() != null) {
            for (LazyScriptParser.ExpressionContext expression : ctx.arguments().argumentList().expression()) {
                argumentNodeList.add((LSExpressionNode) visit(expression));
            }
        }
        LSExpressionNode[] argumentNodes = argumentNodeList.toArray(new LSExpressionNode[argumentNodeList.size()]);
        LSExpressionNode receiverNode = r;
        if (receiverNode == null) {
            String name = ((LSStringLiteralNode) functionNameNode).executeGeneric(null);
            Pair<Integer, FrameSlot> variable = lexicalScope.getVariable(name);
            int scopeDepth = variable.a;
            FrameSlot frameSlot = variable.b;
            if (frameSlot != null) {
                final LSExpressionNode functionNode;
                if (scopeDepth == 0) {
                    functionNode = LSReadLocalVariableNodeGen.create(frameSlot);
                } else {
                    functionNode = LSReadRemoteVariableNodeGen.create(frameSlot, scopeDepth);
                }
                receiverNode = new LSNullLiteralNode();
                LSExpressionNode result = new LSInvokeFunctionNode(receiverNode, functionNode, argumentNodes);
                result.addExpressionTag();
                setSourceFromContext(result, ctx);
                return result;
            }
            receiverNode = createReadThis();
        }
        LSExpressionNode result = new LSInvokePropertyNode(receiverNode, functionNameNode, argumentNodes);
        result.addExpressionTag();
        setSourceFromContext(result, ctx);
        return result;
    }

    LSExpressionNode createAssignExpression(LazyScriptParser.ExpressionContext ctx) {
        final ExpressionContext receivcCtx = ctx.expression(0);
        if (receivcCtx.member() != null) {
            final LSExpressionNode receiverNode = (LSExpressionNode) visit(receivcCtx.expression(0));
            final LSExpressionNode nameNode = (LSExpressionNode) visit(receivcCtx.member().identifier());
            LSExpressionNode valueNode = (LSExpressionNode) visit(ctx.expression(1));
            return createWriteProperty(ctx, receiverNode, nameNode, valueNode);
        }
        if (receivcCtx.index() != null) {
            final LSExpressionNode receiverNode = (LSExpressionNode) visit(receivcCtx.expression(0));
            final LSExpressionNode indexNode = (LSExpressionNode) visit(receivcCtx.index().expression());
            LSExpressionNode valueNode = (LSExpressionNode) visit(ctx.expression(1));
            return createWriteProperty(ctx, receiverNode, indexNode, valueNode);
        }
        if (receivcCtx.identifier() != null) {
            final LSExpressionNode nameNode = (LSExpressionNode) visit(receivcCtx.identifier());
            LSExpressionNode valueNode = (LSExpressionNode) visit(ctx.expression(1));
            return createWriteVariable(nameNode, valueNode);
        }
        throw new LSParseError(source, ctx, "Unsupported assignment expression");
    }

    public LSExpressionNode createWriteVariable(LSExpressionNode nameNode, LSExpressionNode valueNode) {
        if (nameNode == null || valueNode == null) {
            throw new UnsupportedOperationException("nameNode and valueNode must not be null");
        }

        String name = ((LSStringLiteralNode) nameNode).executeGeneric(null);
        Pair<Integer, FrameSlot> variable = lexicalScope.getVariable(name);
        int scopeDepth = variable.a;
        FrameSlot frameSlot = variable.b;
        final LSExpressionNode result;
        boolean newVariable = false;
        if (frameSlot == null) {
            frameSlot = lexicalScope.addVariable(name);
            newVariable = true;
            scopeDepth = 0;
        }
        if (scopeDepth == 0) {
            result = LSWriteLocalVariableNodeGen.create(valueNode, frameSlot, nameNode, newVariable);
        } else {
            result = LSWriteRemoteVariableNodeGen.create(valueNode, frameSlot, nameNode, scopeDepth);
        }
        if (!nameNode.hasSource() || !valueNode.hasSource()) {
            throw new UnsupportedOperationException("nameNode and valueNode must have source defined");
        }
        final int start = nameNode.getSourceCharIndex();
        final int length = valueNode.getSourceEndIndex() - start;
        result.setSourceSection(start, length);
        result.addExpressionTag();

        return result;
    }

    public LSExpressionNode createWriteProperty(LazyScriptParser.ExpressionContext ctx, LSExpressionNode receiverNode,
            LSExpressionNode nameNode, LSExpressionNode valueNode) {
        if (receiverNode == null || nameNode == null || valueNode == null) {
            throw new LSParseError(source, ctx, "One of receiverNode, nameNode or valueNode is null");
        }

        final LSExpressionNode result = LSWritePropertyNodeGen.create(receiverNode, nameNode, valueNode);

        setSourceFromContext(result, ctx);
        result.addExpressionTag();

        return result;
    }

    @Override
    public Node visitThisLiteral(LazyScriptParser.ThisLiteralContext ctx) {
        final LSExpressionNode result = createReadThis();
        setSourceFromContext(result, ctx);
        return result;
    }

    public LSExpressionNode createReadThis() {
        String name = LSLexicalScope.THIS;
        Pair<Integer, FrameSlot> variable = lexicalScope.getVariable(name);
        int scopeDepth = variable.a;
        FrameSlot frameSlot = variable.b;
        final LSExpressionNode result;
        if (frameSlot == null) {
            throw new UnsupportedOperationException("There is no this variable");
        }
        if (scopeDepth == 0) {
            result = LSReadLocalVariableNodeGen.create(frameSlot);
        } else {
            result = LSReadRemoteVariableNodeGen.create(frameSlot, scopeDepth);
        }
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitBreakStatement(LazyScriptParser.BreakStatementContext ctx) {
        if (lexicalScope.isInLoop()) {
            final LSBreakNode breakNode = new LSBreakNode();
            setSourceFromContext(breakNode, ctx);
            return breakNode;
        }
        throw new LSParseError(source, ctx, "break used outside of loop");
    }

    @Override
    public Node visitContinueStatement(LazyScriptParser.ContinueStatementContext ctx) {
        if (lexicalScope.isInLoop()) {
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
        popScope();

        if (conditionNode == null || blockNode == null) {
            throw new LSParseError(source, ctx, "One of conditionNode or blockNode is null");
        }

        conditionNode.addStatementTag();
        final LSWhileNode whileNode = new LSWhileNode(conditionNode, blockNode);
        setSourceFromContext(whileNode, ctx);
        return whileNode;
    }

    @Override
    public Node visitIfStatement(LazyScriptParser.IfStatementContext ctx) {
        LSExpressionNode conditionNode = (LSExpressionNode) visit(ctx.condition);

        pushScope(lexicalScope.isInLoop());
        LSStatementNode thenPartNode = (LSStatementNode) visit(ctx.block(0));
        popScope();

        LSStatementNode elsePartNode = null;
        if (ctx.ELSE() != null) {
            pushScope(lexicalScope.isInLoop());
            elsePartNode = (LSStatementNode) visit(ctx.block(1));
            popScope();
        }

        if (conditionNode == null || thenPartNode == null) {
            throw new LSParseError(source, ctx, "One of conditionNode or thenNode is null");
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
    public Node visitIdentifier(LazyScriptParser.IdentifierContext ctx) {
        final String identifier = ctx.IDENTIFIER().getText();

        final LSStringLiteralNode result = new LSStringLiteralNode(identifier.intern());
        result.addExpressionTag();
        setSourceFromContext(result, ctx);
        return result;
    }

    @Override
    public Node visitNullLiteral(LazyScriptParser.NullLiteralContext ctx) {
        final LSNullLiteralNode result = new LSNullLiteralNode();
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitBooleanLiteral(LazyScriptParser.BooleanLiteralContext ctx) {
        Boolean value;
        if (ctx.TRUE() != null) {
            value = true;
        } else if (ctx.FALSE() != null) {
            value = false;
        } else {
            throw new LSParseError(source, ctx, "Invalid constant literal: " + ctx.getText());
        }
        final LSBooleanLiteralNode result = new LSBooleanLiteralNode(value);
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitStringLiteral(LazyScriptParser.StringLiteralContext ctx) {
        String literal = ctx.STRING_LITERAL().getText();
        assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
        literal = literal.substring(1, literal.length() - 1);

        final LSStringLiteralNode result = new LSStringLiteralNode(literal.intern());
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitNumericLiteral(LazyScriptParser.NumericLiteralContext ctx) {
        LSExpressionNode result;
        if (ctx.DECIMAL_INTEGER_LITERAL() != null) {
            try {
                /* Try if the literal is small enough to fit into a long value. */
                result = new LSIntegerLiteralNode(Long.parseLong(ctx.DECIMAL_INTEGER_LITERAL().getText()));
            } catch (NumberFormatException ex) {
                /* Overflow of long value, so fall back to BigInteger. */
                result = new LSBigIntegerLiteralNode(new BigInteger(ctx.DECIMAL_INTEGER_LITERAL().getText()));
            }
        } else if (ctx.HEX_INTEGER_LITERAL() != null) {
            throw new NotImplementedException();
        } else if (ctx.OCTAL_INTEGER_LITERAL() != null) {
            throw new NotImplementedException();
        } else if (ctx.BINARY_INTEGER_LITERAL() != null) {
            throw new NotImplementedException();
        } else if (ctx.DECIMAL_LITERAL() != null) {
            result = new LSDecimalLiteralNode(Double.parseDouble(ctx.DECIMAL_LITERAL().getText()));
        } else {
            throw new LSParseError(source, ctx, "Invalid numeric literal: " + ctx.getText());
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

        final LSArrayLiteralNode result = new LSArrayLiteralNode(
                elementNodes.toArray(new LSExpressionNode[elementNodes.size()]));
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }

    @Override
    public Node visitObjectLiteral(LazyScriptParser.ObjectLiteralContext ctx) {
        List<LSExpressionNode> nameNodes = new ArrayList<>();
        List<LSExpressionNode> valueNodes = new ArrayList<>();
        if (ctx.propertyAssignment() != null) {
            for (LazyScriptParser.PropertyAssignmentContext propertyAssignmentCtx : ctx.propertyAssignment()) {
                nameNodes.add((LSExpressionNode) visit(propertyAssignmentCtx.propertyName()));
                valueNodes.add((LSExpressionNode) visit(propertyAssignmentCtx.expression()));
            }
        }

        final LSObjectLiteralNode result = new LSObjectLiteralNode(
                nameNodes.toArray(new LSExpressionNode[nameNodes.size()]),
                valueNodes.toArray(new LSExpressionNode[valueNodes.size()]));
        setSourceFromContext(result, ctx);
        result.addExpressionTag();
        return result;
    }
}
