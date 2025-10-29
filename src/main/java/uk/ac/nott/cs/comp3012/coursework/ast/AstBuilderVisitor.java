package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import uk.ac.nott.cs.comp3012.coursework.NottscriptBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.NottscriptParser;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.topLevel.*;
import uk.ac.nott.cs.comp3012.coursework.ast.type.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AstBuilderVisitor extends NottscriptBaseVisitor<Ast> {
    @Override
    public Ast visitCompilationUnit(NottscriptParser.CompilationUnitContext ctx) {
        return super.visitCompilationUnit(ctx);
    }

    @Override
    public Ast visitFunctionDecl(NottscriptParser.FunctionDeclContext ctx) {
        return super.visitFunctionDecl(ctx);
    }

    @Override
    public Ast visitSubroutineDecl(NottscriptParser.SubroutineDeclContext ctx) {
        return super.visitSubroutineDecl(ctx);
    }

    @Override
    public Ast visitDerivedTypeDecl(NottscriptParser.DerivedTypeDeclContext ctx) {
        return super.visitDerivedTypeDecl(ctx);
    }

    @Override
    public Ast visitProgramDeclaration(NottscriptParser.ProgramDeclarationContext ctx) {
        return super.visitProgramDeclaration(ctx);
    }

    @Override
    public Ast visitFunctionDeclaration(NottscriptParser.FunctionDeclarationContext ctx) {
        return super.visitFunctionDeclaration(ctx);
    }

    @Override
    public Ast visitSubroutineDeclaration(NottscriptParser.SubroutineDeclarationContext ctx) {
        return super.visitSubroutineDeclaration(ctx);
    }

    @Override
    public Ast visitDerivedTypeDeclaration(NottscriptParser.DerivedTypeDeclarationContext ctx) {
        return super.visitDerivedTypeDeclaration(ctx);
    }

    @Override
    public Ast visitDeclarationBlock(NottscriptParser.DeclarationBlockContext ctx) {
        return super.visitDeclarationBlock(ctx);
    }

    @Override
    public Ast visitVariableDecl(NottscriptParser.VariableDeclContext ctx) {
        return super.visitVariableDecl(ctx);
    }

    @Override
    public Ast visitDerivedTypeDeclBlock(NottscriptParser.DerivedTypeDeclBlockContext ctx) {
        return super.visitDerivedTypeDeclBlock(ctx);
    }

    @Override
    public Ast visitDerivedTypeVarDecl(NottscriptParser.DerivedTypeVarDeclContext ctx) {
        return super.visitDerivedTypeVarDecl(ctx);
    }

    @Override
    public Ast visitIntegerType(NottscriptParser.IntegerTypeContext ctx) {
        return super.visitIntegerType(ctx);
    }

    @Override
    public Ast visitRealType(NottscriptParser.RealTypeContext ctx) {
        return super.visitRealType(ctx);
    }

    @Override
    public Ast visitCharType(NottscriptParser.CharTypeContext ctx) {
        return super.visitCharType(ctx);
    }

    @Override
    public Ast visitLogicalType(NottscriptParser.LogicalTypeContext ctx) {
        return super.visitLogicalType(ctx);
    }

    @Override
    public Ast visitDerivedType(NottscriptParser.DerivedTypeContext ctx) {
        return super.visitDerivedType(ctx);
    }

    @Override
    public Ast visitArraySpec(NottscriptParser.ArraySpecContext ctx) {
        return super.visitArraySpec(ctx);
    }

    @Override
    public Ast visitDimList(NottscriptParser.DimListContext ctx) {
        return super.visitDimList(ctx);
    }

    @Override
    public Ast visitDim(NottscriptParser.DimContext ctx) {
        return super.visitDim(ctx);
    }

    @Override
    public Ast visitNameList(NottscriptParser.NameListContext ctx) {
        return super.visitNameList(ctx);
    }

    @Override
    public Ast visitStatementBlock(NottscriptParser.StatementBlockContext ctx) {
        return super.visitStatementBlock(ctx);
    }

    @Override
    public Ast visitAssignmentStatement(NottscriptParser.AssignmentStatementContext ctx) {
        return super.visitAssignmentStatement(ctx);
    }

    @Override
    public Ast visitCallStatement(NottscriptParser.CallStatementContext ctx) {
        return super.visitCallStatement(ctx);
    }

    @Override
    public Ast visitIfSimpleStatement(NottscriptParser.IfSimpleStatementContext ctx) {
        return super.visitIfSimpleStatement(ctx);
    }

    @Override
    public Ast visitIfThenElseStatement(NottscriptParser.IfThenElseStatementContext ctx) {
        return super.visitIfThenElseStatement(ctx);
    }

    @Override
    public Ast visitDoLoopStatement(NottscriptParser.DoLoopStatementContext ctx) {
        return super.visitDoLoopStatement(ctx);
    }

    @Override
    public Ast visitDoWhileStatement(NottscriptParser.DoWhileStatementContext ctx) {
        return super.visitDoWhileStatement(ctx);
    }

    @Override
    public Ast visitReadStatement(NottscriptParser.ReadStatementContext ctx) {
        return super.visitReadStatement(ctx);
    }

    @Override
    public Ast visitWriteStatement(NottscriptParser.WriteStatementContext ctx) {
        return super.visitWriteStatement(ctx);
    }

    @Override
    public Ast visitAllocateStatement(NottscriptParser.AllocateStatementContext ctx) {
        return super.visitAllocateStatement(ctx);
    }

    @Override
    public Ast visitDeallocateStatement(NottscriptParser.DeallocateStatementContext ctx) {
        return super.visitDeallocateStatement(ctx);
    }

    @Override
    public Ast visitAssignmentStmt(NottscriptParser.AssignmentStmtContext ctx) {
        return super.visitAssignmentStmt(ctx);
    }

    @Override
    public Ast visitCallStmt(NottscriptParser.CallStmtContext ctx) {
        return super.visitCallStmt(ctx);
    }

    @Override
    public Ast visitIfSimpleStmt(NottscriptParser.IfSimpleStmtContext ctx) {
        return super.visitIfSimpleStmt(ctx);
    }

    @Override
    public Ast visitIfThenElseStmt(NottscriptParser.IfThenElseStmtContext ctx) {
        return super.visitIfThenElseStmt(ctx);
    }

    @Override
    public Ast visitDoLoopStmt(NottscriptParser.DoLoopStmtContext ctx) {
        return super.visitDoLoopStmt(ctx);
    }

    @Override
    public Ast visitDoWhileLoopStmt(NottscriptParser.DoWhileLoopStmtContext ctx) {
        return super.visitDoWhileLoopStmt(ctx);
    }

    @Override
    public Ast visitReadStmt(NottscriptParser.ReadStmtContext ctx) {
        return super.visitReadStmt(ctx);
    }

    @Override
    public Ast visitWriteStmt(NottscriptParser.WriteStmtContext ctx) {
        return super.visitWriteStmt(ctx);
    }

    @Override
    public Ast visitAllocateStmt(NottscriptParser.AllocateStmtContext ctx) {
        return super.visitAllocateStmt(ctx);
    }

    @Override
    public Ast visitDeallocateStmt(NottscriptParser.DeallocateStmtContext ctx) {
        return super.visitDeallocateStmt(ctx);
    }

    @Override
    public Ast visitExprList(NottscriptParser.ExprListContext ctx) {
        return super.visitExprList(ctx);
    }

    @Override
    public Ast visitExpr(NottscriptParser.ExprContext ctx) {
        return super.visitExpr(ctx);
    }

    @Override
    public Ast visitOrExpr(NottscriptParser.OrExprContext ctx) {
        return super.visitOrExpr(ctx);
    }

    @Override
    public Ast visitAndExpr(NottscriptParser.AndExprContext ctx) {
        return super.visitAndExpr(ctx);
    }

    @Override
    public Ast visitRelExpr(NottscriptParser.RelExprContext ctx) {
        return super.visitRelExpr(ctx);
    }

    @Override
    public Ast visitConcatExpr(NottscriptParser.ConcatExprContext ctx) {
        return super.visitConcatExpr(ctx);
    }

    @Override
    public Ast visitAddExpr(NottscriptParser.AddExprContext ctx) {
        return super.visitAddExpr(ctx);
    }

    @Override
    public Ast visitMulExpr(NottscriptParser.MulExprContext ctx) {
        return super.visitMulExpr(ctx);
    }

    @Override
    public Ast visitPowExpr(NottscriptParser.PowExprContext ctx) {
        return super.visitPowExpr(ctx);
    }

    @Override
    public Ast visitPostfixExpr(NottscriptParser.PostfixExprContext ctx) {
        return super.visitPostfixExpr(ctx);
    }

    @Override
    public Ast visitFieldAccess(NottscriptParser.FieldAccessContext ctx) {
        return super.visitFieldAccess(ctx);
    }

    @Override
    public Ast visitCallOrIndex(NottscriptParser.CallOrIndexContext ctx) {
        return super.visitCallOrIndex(ctx);
    }

    @Override
    public Ast visitParenExpr(NottscriptParser.ParenExprContext ctx) {
        return super.visitParenExpr(ctx);
    }

    @Override
    public Ast visitLiteralExpr(NottscriptParser.LiteralExprContext ctx) {
        return super.visitLiteralExpr(ctx);
    }

    @Override
    public Ast visitNameExpr(NottscriptParser.NameExprContext ctx) {
        return super.visitNameExpr(ctx);
    }

    @Override
    public Ast visitRelOp(NottscriptParser.RelOpContext ctx) {
        return super.visitRelOp(ctx);
    }

    @Override
    public Ast visitLiteral(NottscriptParser.LiteralContext ctx) {
        return super.visitLiteral(ctx);
    }
}

