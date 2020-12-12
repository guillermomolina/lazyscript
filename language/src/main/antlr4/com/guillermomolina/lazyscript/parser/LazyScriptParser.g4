/*
 * Copyright (c) 2012, 2018, Guillermo Adrián Molina. All rights reserved. DO NOT ALTER OR REMOVE
 * COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * The Universal Permissive License (UPL), Version 1.0
 * 
 * Subject to the condition set forth below, permission is hereby granted to any person obtaining a
 * copy of this software, associated documentation and/or data (collectively the "Software"), free
 * of charge and under any and all copyright rights in the Software, and any and all patent rights
 * owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software
 * as contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to
 * deal in both
 * 
 * (a) the Software, and
 * 
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with
 * the Software each a "Larger Work" to which the Software is contributed by such licensors),
 * without restriction, including without limitation the rights to copy, create derivative works of,
 * display, perform, and distribute the Software and make, use, sell, offer for sale, import,
 * export, have made, and have sold the Software and the Larger Work(s), and to sublicense the
 * foregoing rights on either these or other terms.
 * 
 * This license is subject to the following condition:
 * 
 * The above copyright notice and either this complete permission notice or at a minimum a reference
 * to the UPL must be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

parser grammar LazyScriptParser;

options {
	tokenVocab = LazyScriptLexer;
	superClass = LSParserBase;
}

module: statement* EOF;

statement:
	whileStatement
	| ifStatement
	| breakStatement
	| continueStatement
	| returnStatement
	| expressionStatement;

whileStatement:
	WHILE LPAREN condition = expression RPAREN block;

breakStatement: BREAK eos;

continueStatement: CONTINUE eos;

ifStatement:
	IF LPAREN condition = expression RPAREN then = block (
		ELSE block
	)?;

debuggerStatement: DEBUGGER eos;

returnStatement: RETURN ({this.notEOL()}? expression)? eos;

expressionStatement:
	{this.notLCURLYAndNotFUNCTION()}? expression eos;

eos: SEMI | EOF | {this.eolAhead()}? | {this.rcurly()}?;

expression:
	expression index
	| expression member
	| expression arguments
	| operator = SUB expression
	| expression operator = (MUL | DIV) expression
	| expression operator = (ADD | SUB) expression
	| expression operator = (LT | GT | LE | GE) expression
	| expression operator = (EQUAL | NOT_EQUAL) expression
	| expression operator = BITAND expression
	| expression operator = BITOR expression
	| expression operator = AND expression
	| expression operator = OR expression
	| <assoc = right> expression operator = ASSIGN expression
	| thisLiteral
	| identifier
	| nullLiteral
	| booleanLiteral
	| stringLiteral
	| numericLiteral
	| arrayLiteral
	| objectLiteral
	| functionLiteral
	| blockLiteral
	| parenExpression;

index: LBRACK expression RBRACK;

member: DOT identifier;

thisLiteral: THIS;

identifier: IDENTIFIER;

parenExpression: LPAREN expression RPAREN;

arguments: LPAREN argumentList? RPAREN;

argumentList: expression (COMMA expression)*;

nullLiteral: NULL;

booleanLiteral: TRUE | FALSE;

stringLiteral: STRING_LITERAL;

numericLiteral:
	DECIMAL_LITERAL
	| DECIMAL_INTEGER_LITERAL
	| HEX_INTEGER_LITERAL
	| OCTAL_INTEGER_LITERAL
	| BINARY_INTEGER_LITERAL;

arrayLiteral: LBRACK elementList RBRACK;

elementList: COMMA* expression? (COMMA+ expression)* COMMA*;

objectLiteral:
	LCURLY (propertyAssignment (COMMA propertyAssignment)*)? COMMA? RCURLY;

propertyAssignment: propertyName COLON expression;

propertyName: identifier | stringLiteral | numericLiteral;

functionLiteral:
	FUNCTION identifier LPAREN parameterList? RPAREN block;

blockLiteral: LPAREN parameterList? RPAREN ARROW block;

parameterList: identifier ( COMMA identifier)*;

block: LCURLY statement* RCURLY;
