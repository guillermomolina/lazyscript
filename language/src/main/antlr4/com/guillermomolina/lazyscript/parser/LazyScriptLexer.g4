
lexer grammar LazyScriptLexer;

BREAK: 'break';
CONTINUE: 'continue';
ELSE: 'else';
FUNCTION: 'function';
IF: 'if';
RETURN: 'return';
WHILE: 'while';

LPAREN: '(';
RPAREN: ')';
LBRACK: '[';
RBRACK: ']';
LCURLY: '{';
RCURLY: '}';

COMMA: ',';
DOT: '.';
COLON: ':';

SEMI: ';';
OR: '||';
AND: '&&';
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';
EQUAL: '==';
NOT_EQUAL: '!=';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
BITAND: '&';
BITOR: '|';
ASSIGN: '=';

ARROW: '=>';

WS: [ \t\r\n\u000C]+ -> skip;
COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

fragment LETTER: [A-Z] | [a-z] | '_' | '$';
fragment NON_ZERO_DIGIT: [1-9];
fragment DIGIT: [0-9];
fragment HEX_DIGIT: [0-9] | [a-f] | [A-F];
fragment OCT_DIGIT: [0-7];
fragment BINARY_DIGIT: '0' | '1';
fragment TAB: '\t';
fragment STRING_CHAR: ~('"' | '\\' | '\r' | '\n');

IDENTIFIER: LETTER (LETTER | DIGIT)*;
STRING_LITERAL: '"' STRING_CHAR* '"';
NUMERIC_LITERAL: '0' | NON_ZERO_DIGIT DIGIT*;

