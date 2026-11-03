grammar Splc;

@parser::members {
    private framework.project2.ErrorReporter errorReporter;

    public void setErrorReporter(framework.project2.ErrorReporter reporter) {
        this.errorReporter = reporter;
    }

    /**
     * Report a grammar-specific missing-token error.
     *
     * line is already the 0-indexed line required by Project 2.
     */
    protected void reportMissing(int tokenType, int line) {
        if (errorReporter != null) {
            errorReporter.reportMissing(tokenType, line);
        }
    }

    /**
     * Return the 0-indexed line of the parser's current lookahead token.
     */
    protected int currentLine() {
        return Math.max(0, getCurrentToken().getLine() - 1);
    }
}


/* ==================== Program ==================== */

program
    : globalDef* EOF
    ;

globalDef
    : structDef
    | functionDef
    | globalVarDef
    ;


/* ==================== Type ==================== */

type
    : INT
    | STRUCT Identifier
    ;


/* ==================== Struct ==================== */

structDef
    : STRUCT Identifier
      LBRACE fieldDef+
      RBRACE SEMI
    ;

fieldDef
    : variableDecl SEMI
    ;


/* ==================== Variable ==================== */

variableDecl
    : type Identifier arrayDimension*
    ;

arrayDimension
    : LBRACK Number RBRACK
    ;

globalVarDef
    : variableDecl SEMI
    ;

localVarDef
    : variableDecl (ASSIGN expression)? SEMI
    ;


/* ==================== Function ==================== */

functionDef
    : type Identifier
      LPAREN parameterList? RPAREN
      block
    ;

// Recover a missing comma before consuming the next parameter.
parameterList
    : parameter (COMMA parameter
                | {reportMissing(COMMA, currentLine());} parameter)*
    ;

parameter
    : variableDecl
    ;


/* ==================== Statement ==================== */

block
    : LBRACE statement* RBRACE
    ;

statement
    : block
    | localVarDef
    | ifStatement
    | whileStatement
    | returnStatement
    | expressionStatement
    ;

ifStatement
    : IF LPAREN expression RPAREN
      block
      (ELSE block)?
    ;

whileStatement
    : WHILE LPAREN expression RPAREN
      block
    ;

returnStatement
    : RETURN expression SEMI
    ;

expressionStatement
    : expression SEMI
    ;


/* ==================== Expression ==================== */

// Recursive alternatives run from highest to lowest precedence.
expression
    : Number
    | Identifier
    | LPAREN expression RPAREN
    | Identifier LPAREN argumentList? RPAREN
    | expression LBRACK expression RBRACK
    | expression DOT Identifier
    | <assoc=right> expression POW expression
    | (PLUS | MINUS | NOT) expression
    | expression (STAR | DIV | MOD) expression
    | expression (PLUS | MINUS) expression
    | expression (LT | LE | GT | GE) expression
    | expression (EQ | NEQ) expression
    | expression AND expression
    | expression OR expression
    | <assoc=right> expression ASSIGN expression
    ;

argumentList
    : expression (COMMA expression)*
    ;


/* ==================== Lexer ==================== */

INT : 'int';
STRUCT : 'struct';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
RETURN : 'return';

LE : '<=';
GE : '>=';
EQ : '==';
NEQ : '!=';
AND : '&&';
OR : '||';
ASSIGN : '=';
PLUS : '+';
MINUS : '-';
STAR : '*';
DIV : '/';
MOD : '%';
POW : '^';
LT : '<';
GT : '>';
NOT : '!';

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
SEMI : ';';
COMMA : ',';
DOT : '.';

Number
    : '0'
    | [1-9] [0-9]*
    ;

Identifier
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
