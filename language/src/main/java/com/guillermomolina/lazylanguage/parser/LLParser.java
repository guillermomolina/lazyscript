package com.guillermomolina.lazylanguage.parser;

import com.guillermomolina.lazylanguage.LLLanguage;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class LLParser {
    private final LLNodeFactory factory;

    public LLParser(final LLLanguage language, final Source source) {
        LazyLanguageLexer lexer = new LazyLanguageLexer(CharStreams.fromString(source.getCharacters().toString()));
        LazyLanguageParser parser = new LazyLanguageParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        BailoutErrorListener listener = new BailoutErrorListener(source);
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
        factory = new LLNodeFactory(language, source);
        factory.visit(parser.module());
    }

    public RootCallTarget getFunction() {
        return factory.getFunction();
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
}
