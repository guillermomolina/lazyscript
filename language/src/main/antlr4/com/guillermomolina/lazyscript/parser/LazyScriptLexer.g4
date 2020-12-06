lexer grammar LazyScriptLexer;

BREAK: 'break';
CONTINUE: 'continue';
ELSE: 'else';
FUNCTION: 'function';
IF: 'if';
RETURN: 'return';
WHILE: 'while';

NULL: 'null';
TRUE: 'true';
FALSE: 'false';

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
LINE_COMMENT: '//' ~[\r\n\u2028\u2029]* -> skip;

IDENTIFIER: LETTER (LETTER | DIGIT)*;
STRING_LITERAL: '"' STRING_CHAR* '"';

/// Numeric _LITERALs

DECIMAL_INTEGER_LITERAL: '0' | [1-9] [0-9_]*;
HEX_INTEGER_LITERAL: '0' [xX] [0-9a-fA-F] [_0-9a-fA-F]*;
OCTAL_INTEGER_LITERAL: '0' [oO] [0-7] [_0-7]*;
BINARY_INTEGER_LITERAL: '0' [bB] [01] [_01]*;
DECIMAL_LITERAL:
	('0' | [1-9] [0-9_]*) '.' [0-9] [0-9_]* ([eE] [+-]? [0-9_]+)?;

fragment DIGIT: [0-9];
fragment LETTER: [A-Z] | [a-z] | '_' | '$';
fragment TAB: '\t';
fragment STRING_CHAR: ~('"' | '\\' | '\r' | '\n');
