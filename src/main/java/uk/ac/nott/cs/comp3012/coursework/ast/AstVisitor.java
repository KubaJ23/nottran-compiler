package uk.ac.nott.cs.comp3012.coursework.ast;

import uk.ac.nott.cs.comp3012.coursework.ast.expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.topLevel.*;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.type.*;

/**
 * Visitor interface for traversing the AST.
 * Uses the visitor pattern to define operations on the AST.
 *
 * @param <T> The return type of the visit methods.
 */
public interface AstVisitor<T> {
    // Top-Level
    T visit(CompilationUnit node);

    T visit(ProgramDecl node);

    T visit(FunctionDecl node);

    T visit(SubroutineDecl node);

    T visit(DerivedTypeDecl node);

    // Declarations
    T visit(VariableDecl node);

    T visit(DerivedType node);

    T visit(Dimension.LiteralDim node);

    T visit(Dimension.PointerDim node);

    // Statements
    T visit(AssignmentStmt node);

    T visit(CallStmt node);

    T visit(IfSimpleStmt node);

    T visit(IfThenElseStmt node);

    T visit(DoLoopStmt node);

    T visit(DoWhileStmt node);

    T visit(ReadStmt node);

    T visit(WriteStmt node);

    T visit(AllocateStmt node);

    T visit(DeallocateStmt node);

    // Expressions
    T visit(BinaryOpExpr node);

    T visit(FieldAccessExpr node);

    T visit(CallOrIndexExpr node);

    T visit(NameExpr node);

    // Literals
    T visit(IntLiteral node);

    T visit(RealLiteral node);

    T visit(LogicalLiteral node);

    T visit(CharLiteral node);

    T visit(BozLiteral node);

    T visit(IntrinsicType intrinsicType);
}
