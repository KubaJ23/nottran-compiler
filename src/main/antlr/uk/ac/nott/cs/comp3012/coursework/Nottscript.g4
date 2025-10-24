grammar Nottscript;

// ---------------------------
// Parser
// ---------------------------

compilationUnit
    : programUnit* programDeclaration programUnit* EOF
    ;

programUnit
    : functionDeclaration  # FunctionDecl
    | subroutineDeclaration # SubroutineDecl
    | derivedTypeDeclaration # DerivedTypeDecl
    ;

programDeclaration
    : PROGRAM NAME declarationBlock? statementBlock END PROGRAM NAME
    ;

functionDeclaration
    : FUNCTION NAME LPAREN nameList? RPAREN (RESULT LPAREN NAME RPAREN)? declarationBlock? statementBlock END FUNCTION NAME
    ;

subroutineDeclaration
    : SUBROUTINE NAME LPAREN nameList? RPAREN declarationBlock? statementBlock END SUBROUTINE NAME
    ;

derivedTypeDeclaration
    : TYPE NAME derivedTypeDeclBlock? END TYPE NAME
    ;

// ---------------------------
// Declarations
// ---------------------------

declarationBlock
    : variableDecl+
    ;

variableDecl
    : typeSpec POINTER? COLONCOLON nameList
    ;

derivedTypeDeclBlock
    : derivedTypeVarDecl+
    ;

derivedTypeVarDecl
    : typeSpec COLONCOLON nameList
    ;

typeSpec
    : INTEGER arraySpec?                 # IntegerType
    | REAL arraySpec?                    # RealType
    | CHARACTER arraySpec?               # CharType
    | LOGICAL arraySpec?                 # LogicalType
    | TYPE LPAREN NAME RPAREN arraySpec? # DerivedType
    ;

arraySpec
    : LPAREN dimList RPAREN
    ;

dimList
    : dim (COMMA dim)*
    ;

dim
    : INT_LITERAL // For fixed-size array e.g. integer(10)
    | STAR        // For pointer array dimension e.g. integer(*)
    ;

nameList
    : NAME (COMMA NAME)*
    ;

// ---------------------------
// Statements
// ---------------------------

statementBlock
    : statement+ // Can not be empty
    ;

statement
    : assignmentStmt      # AssignmentStatement
    | callStmt            # CallStatement
    | ifSimpleStmt        # IfSimpleStatement
    | ifThenElseStmt      # IfThenElseStatement
    | doLoopStmt          # DoLoopStatement
    | doWhileLoopStmt     # DoWhileStatement
    | readStmt            # ReadStatement
    | writeStmt           # WriteStatement
    | allocateStmt        # AllocateStatement
    | deallocateStmt      # DeallocateStatement
    ;

assignmentStmt
    : postfixExpr ASSIGN expr // validate semantically for lvalue
    ;

callStmt
    : CALL NAME LPAREN exprList? RPAREN
    ;

ifSimpleStmt
    : IF LPAREN expr RPAREN statement
    ;

ifThenElseStmt
    : IF LPAREN expr RPAREN THEN statementBlock (ELSE statementBlock)? END IF
    ;

doLoopStmt
    : DO NAME ASSIGN expr COMMA expr (COMMA expr)? statementBlock END DO
    ;

doWhileLoopStmt
    : DO WHILE LPAREN expr RPAREN statementBlock END DO
    ;

readStmt
    : READ postfixExpr (COMMA postfixExpr)* // sus
    ;

writeStmt
    : WRITE exprList // Write takes expressions
    ;

allocateStmt
    : ALLOCATE NAME (COMMA exprList)?
    ;

deallocateStmt
    : DEALLOCATE NAME
    ;

// ---------------------------
// Expressions
// ---------------------------

exprList
    : expr (COMMA expr)*
    ;

expr
    : orExpr
    ;

orExpr
    : andExpr (OR_OP andExpr)*
    ;

andExpr
    : relExpr (AND_OP relExpr)*
    ;

relExpr
    : concatExpr (relOp concatExpr)*
    ;

concatExpr
    : addExpr (CONCAT addExpr)*
    ;

addExpr
    : mulExpr ((PLUS | MINUS) mulExpr)*
    ;

mulExpr
    : powExpr ((STAR | SLASH) powExpr)*
    ;

powExpr
    : postfixExpr (EXP powExpr)?
    ;

postfixExpr
    : atom postfixSuffix*
    ;

postfixSuffix
    : PERCENT NAME                    # FieldAccess
    | LPAREN exprList? RPAREN         # CallOrIndex
    ;

atom
    : LPAREN expr RPAREN              # ParenExpr
    | literal                         # LiteralExpr
    | NAME                            # NameExpr
    ;

relOp
    : EQ | NE | LT | LE | GT | GE
    | LT_TEXT | LE_TEXT | GT_TEXT | GE_TEXT | EQ_TEXT | NE_TEXT
    ;

literal
    : INT_LITERAL
    | REAL_LITERAL
    | B_LITERAL
    | O_LITERAL
    | Z_LITERAL
    | LOGICAL_LITERAL
    | CHAR_LITERAL
    ;

// ---------------------------
// Lexer
// ---------------------------

// Keywords
ALLOCATE    : 'allocate';
CALL        : 'call';
CHARACTER   : 'character';
DEALLOCATE  : 'deallocate';
DO          : 'do';
ELSE        : 'else';
END         : 'end';
FUNCTION    : 'function';
IF          : 'if';
INTEGER     : 'integer';
LOGICAL     : 'logical';
POINTER     : 'pointer';
PROGRAM     : 'program';
READ        : 'read';
REAL        : 'real';
RESULT      : 'result';
SUBROUTINE  : 'subroutine';
THEN        : 'then';
TYPE        : 'type';
WHILE       : 'while';
WRITE       : 'write';

// ---------------------------
// Operators & Special Symbols
// ---------------------------
PLUS        : '+';
MINUS       : '-';
STAR        : '*';
SLASH       : '/';
EXP         : '**';
CONCAT      : '//'; // sus

// Relational operators (two forms)
LT          : '<';
LE          : '<=';
GT          : '>';
GE          : '>=';
EQ          : '==';
NE          : '/=';

LT_TEXT     : '.lt.';
LE_TEXT     : '.le.';
GT_TEXT     : '.gt.';
GE_TEXT     : '.ge.';
EQ_TEXT     : '.eq.';
NE_TEXT     : '.neq.';

// Logical operators
AND_OP      : '.and.';
OR_OP       : '.or.';

// Assignment & punctuation
ASSIGN      : '=';
COMMA       : ',';
COLONCOLON  : '::';
LPAREN      : '(';
RPAREN      : ')';
PERCENT     : '%';


CHAR_LITERAL: '"' ( '""' | PRINTABLE_CHARS_NO_DOUBLE_QUOTE)* '"';

fragment PRINTABLE_CHARS_NO_DOUBLE_QUOTE: [\u0009\u0020-\u0021\u0023-\u007E]; // ASCII 32–33 and 35–126 and ASCII 9 = horizontal tab

LOGICAL_LITERAL: '.true.' | '.false.';

REAL_LITERAL: ('+'|'-')? ( DECIMAL_DIGIT+ '.' DECIMAL_DIGIT* | '.' DECIMAL_DIGIT+ );

INT_LITERAL: ('+'|'-')? DECIMAL_DIGIT+;

B_LITERAL : 'b' '"' BINARY_DIGIT+ '"';

O_LITERAL : 'o' '"' OCTAL_DIGIT+ '"';

Z_LITERAL : 'z' '"' HEXADECIMAL_DIGIT+ '"';

fragment DECIMAL_DIGIT: [0-9];
fragment BINARY_DIGIT: [0-1];
fragment OCTAL_DIGIT: [0-7];
fragment HEXADECIMAL_DIGIT: [0-9a-f];

NAME: LETTER (ALPHA_NUM | '_')*;

fragment ALPHA_NUM: LETTER | DECIMAL_DIGIT;
fragment LETTER: [a-z];

LINE_COMMENT: '!' PRINTABLE_COMMENT_CHARS* -> skip;

fragment PRINTABLE_COMMENT_CHARS: [\u0009\u0020-\u007E];   // ASCII 9 (tab), 32–126 (printable)

WS: [ \t\r\n]+ -> skip;
