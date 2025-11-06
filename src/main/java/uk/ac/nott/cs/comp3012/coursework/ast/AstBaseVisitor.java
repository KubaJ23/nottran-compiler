package uk.ac.nott.cs.comp3012.coursework.ast;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.*;

public abstract class AstBaseVisitor<T> implements AstVisitor<T> {

    public final T visit(Ast node) {
        return switch (node) {
            case CompilationUnit cu -> visitCompilationUnit(cu);
            case ProgramDecl pd -> visitProgramDecl(pd);
            case FunctionDecl fd -> visitFunctionDecl(fd);
            case SubroutineDecl sd -> visitSubroutineDecl(sd);
            case DerivedTypeDecl dtd -> visitDerivedTypeDecl(dtd);
            case VariableDecl vd -> visitVariableDecl(vd);
            case IntrinsicType it -> visitIntrinsicType(it);
            case DerivedType dt -> visitDerivedType(dt);
            case AssignmentStmt s -> visitAssignmentStmt(s);
            case CallStmt s -> visitCallStmt(s);
            case IfSimpleStmt s -> visitIfSimpleStmt(s);
            case IfThenElseStmt s -> visitIfThenElseStmt(s);
            case DoLoopStmt s -> visitDoLoopStmt(s);
            case DoWhileStmt s -> visitDoWhileStmt(s);
            case ReadStmt s -> visitReadStmt(s);
            case WriteStmt s -> visitWriteStmt(s);
            case AllocateStmt s -> visitAllocateStmt(s);
            case DeallocateStmt s -> visitDeallocateStmt(s);
            case BinaryOpExpr e -> visitBinaryOpExpr(e);
            case FieldAccessExpr e -> visitFieldAccessExpr(e);
            case CallOrIndexExpr e -> visitCallOrIndexExpr(e);
            case NameExpr e -> visitNameExpr(e);
            case Literal.IntLiteral l -> visitIntLiteral(l);
            case Literal.LogicalLiteral l -> visitLogicalLiteral(l);
            case Literal.CharLiteral l -> visitCharLiteral(l);
            case null -> defaultResult();
        };
    }

    protected T defaultResult() {return null;}

    public T visitCompilationUnit(CompilationUnit cu) {throw new UnsupportedOperationException("visitCompilationUnit not implemented");}

    public T visitProgramDecl(ProgramDecl pd) {throw new UnsupportedOperationException("visitProgramDecl not implemented");}

    public T visitFunctionDecl(FunctionDecl fd) {throw new UnsupportedOperationException("visitFunctionDecl not implemented");}

    public T visitSubroutineDecl(SubroutineDecl sd) {throw new UnsupportedOperationException("visitSubroutineDecl not implemented");}

    public T visitDerivedTypeDecl(DerivedTypeDecl dtd) {throw new UnsupportedOperationException("visitDerivedTypeDecl not implemented");}

    public T visitVariableDecl(VariableDecl vd) {throw new UnsupportedOperationException("visitVariableDecl not implemented");}

    public T visitAssignmentStmt(AssignmentStmt s) {throw new UnsupportedOperationException("visitAssignmentStmt not implemented");}

    public T visitCallStmt(CallStmt s) {throw new UnsupportedOperationException("visitCallStmt not implemented");}

    public T visitIfSimpleStmt(IfSimpleStmt s) {throw new UnsupportedOperationException("visitIfSimpleStmt not implemented");}

    public T visitIfThenElseStmt(IfThenElseStmt s) {throw new UnsupportedOperationException("visitIfThenElseStmt not implemented");}

    public T visitDoLoopStmt(DoLoopStmt s) {throw new UnsupportedOperationException("visitDoLoopStmt not implemented");}

    public T visitDoWhileStmt(DoWhileStmt s) {throw new UnsupportedOperationException("visitDoWhileStmt not implemented");}

    public T visitReadStmt(ReadStmt s) {throw new UnsupportedOperationException("visitReadStmt not implemented");}

    public T visitWriteStmt(WriteStmt s) {throw new UnsupportedOperationException("visitWriteStmt not implemented");}

    public T visitAllocateStmt(AllocateStmt s) {throw new UnsupportedOperationException("visitAllocateStmt not implemented");}

    public T visitBinaryOpExpr(BinaryOpExpr e) {throw new UnsupportedOperationException("visitBinaryOpExpr not implemented");}

    public T visitFieldAccessExpr(FieldAccessExpr e) {throw new UnsupportedOperationException("visitFieldAccessExpr not implemented");}

    public T visitCallOrIndexExpr(CallOrIndexExpr e) {throw new UnsupportedOperationException("visitCallOrIndexExpr not implemented");}

    public T visitIntrinsicType(IntrinsicType it) {throw new UnsupportedOperationException("visitIntrinsicType not implemented");}

    public T visitDerivedType(DerivedType dt) {throw new UnsupportedOperationException("visitDerivedType not implemented");}

    public T visitDeallocateStmt(DeallocateStmt s) {throw new UnsupportedOperationException("visitDeallocateStmt not implemented");}

    public T visitNameExpr(NameExpr e) {throw new UnsupportedOperationException("visitNameExpr not implemented");}

    public T visitIntLiteral(Literal.IntLiteral l) {throw new UnsupportedOperationException("visitIntLiteral not implemented");}

    public T visitLogicalLiteral(Literal.LogicalLiteral l) {throw new UnsupportedOperationException("visitLogicalLiteral not implemented");}

    public T visitCharLiteral(Literal.CharLiteral l) {throw new UnsupportedOperationException("visitCharLiteral not implemented");}
}