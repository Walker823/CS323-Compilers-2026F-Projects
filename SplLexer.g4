lexer grammar SplLexer;

// Keywords win ties with Identifier; longer identifiers still match in full.
INT      : 'int';
STRUCT   : 'struct';
RETURN   : 'return';
IF       : 'if';
ELSE     : 'else';
WHILE    : 'while';

// Operators use ANTLR's longest-match rule.
LE       : '<=';
GE       : '>=';
EQ       : '==';
NEQ      : '!=';
AND      : '&&';
OR       : '||';
ASSIGN   : '=';
PLUS     : '+';
MINUS    : '-';
MUL      : '*';
DIV      : '/';
MOD      : '%';
POW      : '^';
LT       : '<';
GT       : '>';
NOT      : '!';
DOT      : '.';

// Separators.
SEMI     : ';';
COMMA    : ',';
LPAREN   : '(';
RPAREN   : ')';
LBRACE   : '{';
RBRACE   : '}';
LBRACK   : '[';
RBRACK   : ']';

Identifier : [a-zA-Z_][a-zA-Z_0-9]*;
Number     : '0' | [1-9][0-9]*;

WS           : [ \t\r\n]+ -> skip;
LINE_COMMENT : '//' ~[\r\n]* -> skip;
// Non-greedy matching ends at the first closing delimiter, without nesting.
BLOCK_COMMENT : '/*' .*? '*/' -> skip;
