package com.guillermomolina.lazyscript.parser;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

/**
 * All parser methods that used in grammar (p, prev, notEOL, etc.) should start
 * with lower case char similar to parser rules.
 */
public abstract class LSParserBase extends Parser {
    LSParserBase(TokenStream input) {
        super(input);
    }

    protected boolean notEOL() {
        return !here(LazyScriptParser.EOL);
    }

    protected boolean notLCURLYAndNotFUNCTION() {
        int nextTokenType = _input.LT(1).getType();
        return nextTokenType != LazyScriptParser.LCURLY && nextTokenType != LazyScriptParser.FUNCTION;
    }

    protected boolean rcurly() {
        return _input.LT(1).getType() == LazyScriptParser.RCURLY;
    }
    
    /**
     * Returns {@code true} iff on the current index of the parser's
     * token stream a token of the given {@code type} exists on the
     * {@code HIDDEN} channel.
     *
     * @param type
     *         the type of the token on the {@code HIDDEN} channel
     *         to check.
     *
     * @return {@code true} iff on the current index of the parser's
     * token stream a token of the given {@code type} exists on the
     * {@code HIDDEN} channel.
     */
    private boolean here(final int type) {

        // Get the token ahead of the current index.
        int possibleIndexEosToken = this.getCurrentToken().getTokenIndex() - 1;
        Token ahead = _input.get(possibleIndexEosToken);

        // Check if the token resides on the HIDDEN channel and if it's of the
        // provided type.
        return (ahead.getChannel() == Lexer.HIDDEN) && (ahead.getType() == type);
    }

    /**
     * Returns {@code true} iff on the current index of the parser's token stream a
     * token exists on the {@code HIDDEN} channel which either is a line terminator,
     * or is a multi line comment that contains a line terminator.
     *
     * @return {@code true} iff on the current index of the parser's token stream a
     *         token exists on the {@code HIDDEN} channel which either is a line
     *         terminator, or is a multi line comment that contains a line
     *         terminator.
     */
    protected boolean eolAhead() {

        // Get the token ahead of the current index.
        int possibleIndexEosToken = this.getCurrentToken().getTokenIndex() - 1;
        Token ahead = _input.get(possibleIndexEosToken);

        if (ahead.getChannel() != Lexer.HIDDEN) {
            // We're only interested in tokens on the HIDDEN channel.
            return false;
        }

        if (ahead.getType() == LazyScriptParser.EOL) {
            // There is definitely a line terminator ahead.
            return true;
        }

        if (ahead.getType() == LazyScriptParser.WS) {
            // Get the token ahead of the current whitespaces.
            possibleIndexEosToken = this.getCurrentToken().getTokenIndex() - 2;
            ahead = _input.get(possibleIndexEosToken);
        }

        // Get the token's text and type.
        String text = ahead.getText();
        int type = ahead.getType();

        // Check if the token is, or contains a line terminator.
        return (type == LazyScriptParser.COMMENT && (text.contains("\r") || text.contains("\n")))
                || (type == LazyScriptParser.EOL);
    }
}