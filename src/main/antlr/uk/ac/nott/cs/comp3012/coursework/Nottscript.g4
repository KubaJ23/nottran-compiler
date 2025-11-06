grammar Nottscript;

// ---------------------------
// Parser
// ---------------------------

compilationUnit
    : programUnit* programDeclaration programUnit* EOF
    ;

programUnit
    : functionDeclaration   # FunctionDecl
    | subroutineDeclaration # SubroutineDecl
    | derivedTypeDeclaration # DerivedTypeDecl
    ;

programDeclaration
    : PROGRAM name=NAME declarationBlock statementBlock END PROGRAM endName=NAME
    ;

functionDeclaration
    : FUNCTION name=NAME LPAREN nameList? RPAREN (RESULT LPAREN result=NAME RPAREN)? declarationBlock statementBlock END FUNCTION endName=NAME
    ;

subroutineDeclaration
    : SUBROUTINE name=NAME LPAREN nameList? RPAREN declarationBlock statementBlock END SUBROUTINE endName=NAME
    ;

derivedTypeDeclaration
    : TYPE name=NAME derivedTypeDeclBlock END TYPE endName=NAME
    ;

// ---------------------------
// Declarations
// ---------------------------

declarationBlock
    : decls+=variableDecl*
    ;

variableDecl
    : typeSpec POINTER? COLONCOLON nameList
    ;

derivedTypeDeclBlock
    : fields+=derivedTypeVarDecl+
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
    : INT_LITERAL
    | STAR
    ;

nameList
    : names+=NAME (COMMA names+=NAME)*
    ;

// ---------------------------
// Statements
// ---------------------------

statementBlock
    : stmts+=statement+
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
    : lvalue=expr ASSIGN rvalue=expr
    ;

callStmt
    : CALL NAME LPAREN exprList? RPAREN
    ;

ifSimpleStmt
    : IF LPAREN expr RPAREN statement
    ;

ifThenElseStmt
    : IF LPAREN cond=expr RPAREN THEN thenBlock=statementBlock (ELSE elseBlock=statementBlock)? END IF
    ;

doLoopStmt
    : DO loopVar=NAME ASSIGN start=expr COMMA end=expr (COMMA step=expr)? statementBlock END DO
    ;

doWhileLoopStmt
    : DO WHILE LPAREN expr RPAREN statementBlock END DO
    ;

readStmt
    : READ exprList
    ;

writeStmt
    : WRITE exprList
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
    : exprs+=expr (COMMA exprs+=expr)*
    ;

expr
    : orExpr_
    ;

orExpr_
    : left=orExpr_ OR_OP right=andExpr_   # OrExpr
    | andExpr_                            # OrBase
    ;

andExpr_
    : left=andExpr_ AND_OP right=relExpr_ # AndExpr
    | relExpr_                            # AndBase
    ;

relExpr_
    : left=relExpr_ op=relOp right=concatExpr_ # RelExpr
    | concatExpr_                              # RelBase
    ;

concatExpr_
    : left=concatExpr_ CONCAT right=addExpr_ # ConcatExpr
    | addExpr_                               # ConcatBase
    ;

addExpr_
    : left=addExpr_ op=(PLUS | MINUS) right=mulExpr_ # AddExpr
    | mulExpr_                                       # AddBase
    ;

mulExpr_
    : left=mulExpr_ op=(STAR | SLASH) right=powExpr_ # MulExpr
    | powExpr_                                       # MulBase
    ;

powExpr_
    : base=primaryExpr EXP exponent=powExpr_  # PowExpr
    | primaryExpr                             # PowBase
    ;

primaryExpr
    : base=primaryExpr PERCENT field=NAME            # FieldAccess
    | base=primaryExpr LPAREN args=exprList? RPAREN  # CallOrIndex
    | atom                                           # PrimaryBase
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
    : INT_LITERAL     # IntLit
    | REAL_LITERAL    # RealLit
    | B_LITERAL       # BozLitB
    | O_LITERAL       # BozLitO
    | Z_LITERAL       # BozLitZ
    | LOGICAL_LITERAL # LogicalLit
    | CHAR_LITERAL    # CharLit
    ;

// ---------------------------
// Lexer
// ---------------------------

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

PLUS        : '+';
MINUS       : '-';
STAR        : '*';
SLASH       : '/';
EXP         : '**';
CONCAT      : '//';

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

AND_OP      : '.and.';
OR_OP       : '.or.';

ASSIGN      : '=';
COMMA       : ',';
COLONCOLON  : '::';
LPAREN      : '(';
RPAREN      : ')';
PERCENT     : '%';


CHAR_LITERAL: '"' ( '""' | PRINTABLE_CHARS_NO_DOUBLE_QUOTE)* '"';

fragment PRINTABLE_CHARS_NO_DOUBLE_QUOTE: [\u0009\u0020-\u0021\u0023-\u007E];

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

fragment PRINTABLE_COMMENT_CHARS: [\u0009\u0020-\u007E];

WS: [ \t\r\n]+ -> skip;