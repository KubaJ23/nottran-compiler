package uk.ac.nott.cs.comp3012.coursework.ast;

import java.util.List;

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

    public T visitCompilationUnit(CompilationUnit cu) {return defaultResult();}

    public T visitProgramDecl(ProgramDecl pd) {return defaultResult();}

    public T visitFunctionDecl(FunctionDecl fd) {return defaultResult();}

    public T visitSubroutineDecl(SubroutineDecl sd) {return defaultResult();}

    public T visitDerivedTypeDecl(DerivedTypeDecl dtd) {return defaultResult();}

    public T visitVariableDecl(VariableDecl vd) {return defaultResult();}

    public T visitAssignmentStmt(AssignmentStmt s) {return defaultResult();}

    public T visitCallStmt(CallStmt s) {return defaultResult();}

    public T visitIfSimpleStmt(IfSimpleStmt s) {return defaultResult();}

    public T visitIfThenElseStmt(IfThenElseStmt s) {return defaultResult();}

    public T visitDoLoopStmt(DoLoopStmt s) {return defaultResult();}

    public T visitDoWhileStmt(DoWhileStmt s) {return defaultResult();}

    public T visitReadStmt(ReadStmt s) {return defaultResult();}

    public T visitWriteStmt(WriteStmt s) {return defaultResult();}

    public T visitAllocateStmt(AllocateStmt s) {return defaultResult();}

    public T visitBinaryOpExpr(BinaryOpExpr e) {return defaultResult();}

    public T visitFieldAccessExpr(FieldAccessExpr e) {return defaultResult();}

    public T visitCallOrIndexExpr(CallOrIndexExpr e) {return defaultResult();}

    public T visitIntrinsicType(IntrinsicType it) {return defaultResult();}

    public T visitDerivedType(DerivedType dt) {return defaultResult();}

    public T visitDeallocateStmt(DeallocateStmt s) {return defaultResult();}

    public T visitNameExpr(NameExpr e) {return defaultResult();}

    public T visitIntLiteral(Literal.IntLiteral l) {return defaultResult();}

    public T visitLogicalLiteral(Literal.LogicalLiteral l) {return defaultResult();}

    public T visitCharLiteral(Literal.CharLiteral l) {return defaultResult();}
}