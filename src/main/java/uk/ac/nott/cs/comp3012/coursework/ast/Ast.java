package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;

import java.util.List;

/**
 * Base interface for all AST nodes.
 */
public sealed interface Ast {
    /**
     * @return The starting ANTLR token, used for error reporting (line/column).
     */
    Token startToken();

    /**
     * The root of the AST, representing the entire file.
     * Corresponds to 'compilationUnit'.
     */
    record CompilationUnit(Token startToken, ProgramUnit.ProgramDecl program, List<ProgramUnit> programUnits) implements Ast {}


    sealed interface ProgramUnit extends Ast {
        record DerivedTypeDecl(Token startToken, String name, List<VariableDecl> fields) implements ProgramUnit {}

        record FunctionDecl(Token startToken, String name, List<String> parameterNames, String resultName, List<VariableDecl> declarations, List<Statement> statements) implements ProgramUnit {}

        record ProgramDecl(Token startToken, String name, List<VariableDecl> declarations, List<Statement> statements) implements ProgramUnit {}

        record SubroutineDecl(Token startToken, String name, List<String> parameterNames, List<VariableDecl> declarations, List<Statement> statements) implements ProgramUnit {}
    }


    sealed interface Statement extends Ast {
        record AllocateStmt(Token startToken, String pointerName, List<Expression> dimensions) implements Statement {}

        record AssignmentStmt(Token startToken, Expression lvalue, Expression rvalue) implements Statement {}

        record CallStmt(Token startToken, String subroutineName, List<Expression> arguments) implements Statement {}

        record DeallocateStmt(Token startToken, String pointerName) implements Statement {}

        record DoLoopStmt(Token startToken, String loopVariable, Expression start, Expression end, Expression step, List<Statement> body) implements Statement {}

        record DoWhileStmt(Token startToken, Expression condition, List<Statement> body) implements Statement {}

        record IfSimpleStmt(Token startToken, Expression condition, Statement thenStatement) implements Statement {}

        record IfThenElseStmt(Token startToken, Expression condition, List<Statement> thenBlock, List<Statement> elseBlock) implements Statement {}

        record ReadStmt(Token startToken, List<Expression> lvalues) implements Statement {}

        record WriteStmt(Token startToken, List<Expression> expressions) implements Statement {}
    }


    /**
     * A variable declaration line (e.g., 'integer :: a, b').
     */
    record VariableDecl(Token startToken, TypeSpec type, boolean isPointer, List<String> names) implements Ast {}

    sealed interface Dimension {
        record LiteralDim(int size) implements Dimension {}

        record PointerDim() implements Dimension {}
    }

    sealed interface TypeSpec extends Ast {
        record DerivedType(Token startToken, String name, List<Dimension> d) implements TypeSpec {}

        record IntrinsicType(Token startToken, VarType type, List<Dimension> d) implements TypeSpec {
            enum VarType {
                INTEGER, LOGICAL, CHARACTER
            }
        }
    }


    sealed interface Expression extends Ast {
        sealed interface Literal extends Expression {
            record CharLiteral(Token startToken, String value) implements Literal {}

            record IntLiteral(Token startToken, int value) implements Literal {}

            record LogicalLiteral(Token startToken, boolean value) implements Literal {}

            record BozLiteral(Token startToken, String value, char base) implements Literal {}
        }

        record BinaryOpExpr(Token startToken, Expression left, BinaryOp operator, Expression right) implements Expression {
            public enum BinaryOp {
                ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, CONCAT, EQUAL, NOT_EQUAL, LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL, AND, OR
            }
        }

        record FieldAccessExpr(Token startToken, Expression base, String fieldName) implements Expression {}

        record NameExpr(Token startToken, String name) implements Expression {}

        record CallOrIndexExpr(Token startToken, Expression base, List<Expression> arguments) implements Expression {}
    }
}