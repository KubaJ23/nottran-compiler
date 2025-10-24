package uk.ac.nott.cs.comp3012.coursework.ast;

import uk.ac.nott.cs.comp3012.coursework.ast.expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.topLevel.*;
import uk.ac.nott.cs.comp3012.coursework.ast.type.*;

public class AstVisitorConcrete implements AstVisitor {
    @Override
    public Object visit(CompilationUnit node) {
        return null;
    }

    @Override
    public Object visit(ProgramDecl node) {
        return null;
    }

    @Override
    public Object visit(FunctionDecl node) {
        return null;
    }

    @Override
    public Object visit(SubroutineDecl node) {
        return null;
    }

    @Override
    public Object visit(DerivedTypeDecl node) {
        return null;
    }

    @Override
    public Object visit(VariableDecl node) {
        return null;
    }

    @Override
    public Object visit(DerivedType node) {
        return null;
    }

    @Override
    public Object visit(Dimension.LiteralDim node) {
        return null;
    }

    @Override
    public Object visit(Dimension.PointerDim node) {
        return null;
    }

    @Override
    public Object visit(AssignmentStmt node) {
        return null;
    }

    @Override
    public Object visit(CallStmt node) {
        return null;
    }

    @Override
    public Object visit(IfSimpleStmt node) {
        return null;
    }

    @Override
    public Object visit(IfThenElseStmt node) {
        return null;
    }

    @Override
    public Object visit(DoLoopStmt node) {
        return null;
    }

    @Override
    public Object visit(DoWhileStmt node) {
        return null;
    }

    @Override
    public Object visit(ReadStmt node) {
        return null;
    }

    @Override
    public Object visit(WriteStmt node) {
        return null;
    }

    @Override
    public Object visit(AllocateStmt node) {
        return null;
    }

    @Override
    public Object visit(DeallocateStmt node) {
        return null;
    }

    @Override
    public Object visit(BinaryOpExpr node) {
        return null;
    }

    @Override
    public Object visit(FieldAccessExpr node) {
        return null;
    }

    @Override
    public Object visit(CallOrIndexExpr node) {
        return null;
    }

    @Override
    public Object visit(NameExpr node) {
        return null;
    }

    @Override
    public Object visit(IntLiteral node) {
        return null;
    }

    @Override
    public Object visit(RealLiteral node) {
        return null;
    }

    @Override
    public Object visit(LogicalLiteral node) {
        return null;
    }

    @Override
    public Object visit(CharLiteral node) {
        return null;
    }

    @Override
    public Object visit(BozLiteral node) {
        return null;
    }

    @Override
    public Object visit(IntrinsicType intrinsicType) {
        return null;
    }
}
