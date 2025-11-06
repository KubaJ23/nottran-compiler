package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.nott.cs.comp3012.coursework.CompilerException;
import uk.ac.nott.cs.comp3012.coursework.NottscriptBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.NottscriptParser;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.Literal.*;
import uk.ac.nott.cs.comp3012.coursework.util.types.Dimension;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

public class AstBuilder extends NottscriptBaseVisitor<Ast> {

    // ==================================================================
    // Top-Level & Program Units
    // ==================================================================

    @Override
    public Ast visitCompilationUnit(NottscriptParser.CompilationUnitContext ctx) {
        ProgramUnit.ProgramDecl program = (ProgramUnit.ProgramDecl) visit(ctx.programDeclaration());
        List<ProgramUnit> units = new ArrayList<>();
        for (NottscriptParser.ProgramUnitContext unitCtx : ctx.programUnit()) {
            units.add((ProgramUnit) visit(unitCtx));
        }
        return new CompilationUnit(ctx.start, program, units);
    }

    @Override
    public Ast visitProgramDeclaration(NottscriptParser.ProgramDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, endName);

        List<VariableDecl> decls = visitAndMap(ctx.declarationBlock().decls, VariableDecl.class);
        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);

        return new ProgramDecl(ctx.start, name, decls, stmts);
    }

    @Override
    public Ast visitFunctionDeclaration(NottscriptParser.FunctionDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, endName);
        List<String> params = visitANameList(ctx.nameList());

        String resultName = (ctx.result != null) ? ctx.result.getText() : null;

        List<VariableDecl> decls = visitAndMap(ctx.declarationBlock().decls, VariableDecl.class);
        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);

        return new FunctionDecl(ctx.start, name, params, resultName, decls, stmts);
    }

    @Override
    public Ast visitSubroutineDeclaration(NottscriptParser.SubroutineDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, endName);
        List<String> params = visitANameList(ctx.nameList());

        List<VariableDecl> decls = visitAndMap(ctx.declarationBlock().decls, VariableDecl.class);
        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);

        return new SubroutineDecl(ctx.start, name, params, decls, stmts);
    }

    @Override
    public Ast visitDerivedTypeDeclaration(NottscriptParser.DerivedTypeDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, endName);

        List<VariableDecl> fields = visitAndMap(ctx.derivedTypeDeclBlock().fields, VariableDecl.class);

        return new DerivedTypeDecl(ctx.start, name, fields);
    }

    // ==================================================================
    // Declarations & Type Specifications
    // ==================================================================

    @Override
    public Ast visitVariableDecl(NottscriptParser.VariableDeclContext ctx) {
        TypeSpec type = (TypeSpec) visit(ctx.typeSpec());
        boolean isPointer = (ctx.POINTER() != null);
        List<String> names = visitANameList(ctx.nameList());
        return new VariableDecl(ctx.start, type, isPointer, names);
    }

    @Override
    public Ast visitDerivedTypeVarDecl(NottscriptParser.DerivedTypeVarDeclContext ctx) {
        TypeSpec type = (TypeSpec) visit(ctx.typeSpec());
        List<String> names = visitANameList(ctx.nameList());
        return new VariableDecl(ctx.start, type, false, names);
    }

    @Override
    public Ast visitIntegerType(NottscriptParser.IntegerTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, Type.Intrinsic.INTEGER, dims);
    }

    @Override
    public Ast visitRealType(NottscriptParser.RealTypeContext ctx) {
        throw new UnsupportedOperationException("Reals are not supported yet.");
//        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
//        return new IntrinsicType(ctx.start, VarType.REAL, dims);
    }

    @Override
    public Ast visitCharType(NottscriptParser.CharTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, Type.Intrinsic.CHARACTER, dims);
    }

    @Override
    public Ast visitLogicalType(NottscriptParser.LogicalTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, Type.Intrinsic.LOGICAL, dims);
    }

    @Override
    public Ast visitDerivedType(NottscriptParser.DerivedTypeContext ctx) {
        String name = ctx.NAME().getText();
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new DerivedType(ctx.start, name, dims);
    }

    // ==================================================================
    // Statements
    // ==================================================================

    @Override
    public Ast visitAssignmentStmt(NottscriptParser.AssignmentStmtContext ctx) {
        Expression lvalue = (Expression) visit(ctx.lvalue);
        Expression rvalue = (Expression) visit(ctx.rvalue);
        return new AssignmentStmt(ctx.start, lvalue, rvalue);
    }

    @Override
    public Ast visitCallStmt(NottscriptParser.CallStmtContext ctx) {
        String name = ctx.NAME().getText();

        List<Expression> args = (ctx.exprList() != null) ? visitAndMap(ctx.exprList().exprs, Expression.class) : Collections.emptyList();

        return new CallStmt(ctx.start, name, args);
    }

    @Override
    public Ast visitIfSimpleStmt(NottscriptParser.IfSimpleStmtContext ctx) {
        Expression condition = (Expression) visit(ctx.expr());
        Statement thenStmt = (Statement) visit(ctx.statement());
        return new IfSimpleStmt(ctx.start, condition, thenStmt);
    }

    @Override
    public Ast visitIfThenElseStmt(NottscriptParser.IfThenElseStmtContext ctx) {
        Expression condition = (Expression) visit(ctx.cond);
        List<Statement> thenBlock = visitAndMap(ctx.thenBlock.stmts, Statement.class);

        List<Statement> elseBlock = (ctx.elseBlock != null) ? visitAndMap(ctx.elseBlock.stmts, Statement.class) : Collections.emptyList();

        return new IfThenElseStmt(ctx.start, condition, thenBlock, elseBlock);
    }

    @Override
    public Ast visitDoLoopStmt(NottscriptParser.DoLoopStmtContext ctx) {
        String var = ctx.loopVar.getText();
        Expression start = (Expression) visit(ctx.start);
        Expression end = (Expression) visit(ctx.end);
        Expression step = (ctx.step != null) ? (Expression) visit(ctx.step) : new IntLiteral(ctx.getStart(), 1);
        List<Statement> body = visitAndMap(ctx.statementBlock().stmts, Statement.class);
        return new DoLoopStmt(ctx.getStart(), var, start, end, step, body);
    }

    @Override
    public Ast visitDoWhileLoopStmt(NottscriptParser.DoWhileLoopStmtContext ctx) {
        Expression condition = (Expression) visit(ctx.expr());
        List<Statement> body = visitAndMap(ctx.statementBlock().stmts, Statement.class);
        return new DoWhileStmt(ctx.start, condition, body);
    }

    @Override
    public Ast visitReadStmt(NottscriptParser.ReadStmtContext ctx) {
        List<Expression> exprs = visitAndMap(ctx.exprList().exprs, Expression.class);
        return new ReadStmt(ctx.start, exprs);
    }

    @Override
    public Ast visitWriteStmt(NottscriptParser.WriteStmtContext ctx) {
        List<Expression> exprs = visitAndMap(ctx.exprList().exprs, Expression.class);

        return new WriteStmt(ctx.start, exprs);
    }

    @Override
    public Ast visitAllocateStmt(NottscriptParser.AllocateStmtContext ctx) {
        String name = ctx.NAME().getText();

        List<Expression> dims = (ctx.exprList() != null) ? visitAndMap(ctx.exprList().exprs, Expression.class) : Collections.emptyList();

        return new AllocateStmt(ctx.start, name, dims);
    }

    @Override
    public Ast visitDeallocateStmt(NottscriptParser.DeallocateStmtContext ctx) {
        String name = ctx.NAME().getText();
        return new DeallocateStmt(ctx.start, name);
    }

    // ==================================================================
    // Expressions
    // ==================================================================

    @Override
    public Ast visitOrExpr(NottscriptParser.OrExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOpExpr(left.startToken(), left, BinaryOpExpr.BinaryOp.OR, right);
    }

    @Override
    public Ast visitAndExpr(NottscriptParser.AndExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOpExpr(left.startToken(), left, BinaryOpExpr.BinaryOp.AND, right);
    }

    @Override
    public Ast visitRelExpr(NottscriptParser.RelExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);

        BinaryOpExpr.BinaryOp op = switch (ctx.op.start.getType()) {
            case NottscriptParser.EQ, NottscriptParser.EQ_TEXT -> BinaryOpExpr.BinaryOp.EQUAL;
            case NottscriptParser.NE, NottscriptParser.NE_TEXT -> BinaryOpExpr.BinaryOp.NOT_EQUAL;
            case NottscriptParser.LT, NottscriptParser.LT_TEXT -> BinaryOpExpr.BinaryOp.LESS_THAN;
            case NottscriptParser.LE, NottscriptParser.LE_TEXT -> BinaryOpExpr.BinaryOp.LESS_EQUAL;
            case NottscriptParser.GT, NottscriptParser.GT_TEXT -> BinaryOpExpr.BinaryOp.GREATER_THAN;
            case NottscriptParser.GE, NottscriptParser.GE_TEXT -> BinaryOpExpr.BinaryOp.GREATER_EQUAL;
            default -> throw new IllegalArgumentException("Unexpected relational operator: " + ctx.op.getText());
        };

        return new BinaryOpExpr(left.startToken(), left, op, right);
    }

    @Override
    public Ast visitConcatExpr(NottscriptParser.ConcatExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOpExpr(left.startToken(), left, BinaryOpExpr.BinaryOp.CONCAT, right);
    }

    @Override
    public Ast visitAddExpr(NottscriptParser.AddExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);

        BinaryOpExpr.BinaryOp op = switch (ctx.op.getType()) {
            case NottscriptParser.PLUS -> BinaryOpExpr.BinaryOp.ADD;
            case NottscriptParser.MINUS -> BinaryOpExpr.BinaryOp.SUBTRACT;
            default -> throw new IllegalStateException("Unexpected operator in AddExpr: " + ctx.op.getText());
        };

        return new BinaryOpExpr(left.startToken(), left, op, right);
    }

    @Override
    public Ast visitMulExpr(NottscriptParser.MulExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);

        BinaryOpExpr.BinaryOp op = switch (ctx.op.getType()) {
            case NottscriptParser.STAR -> BinaryOpExpr.BinaryOp.MULTIPLY;
            case NottscriptParser.SLASH -> BinaryOpExpr.BinaryOp.DIVIDE;
            default -> throw new IllegalStateException("Unexpected operator in MulExpr: " + ctx.op.getText());
        };

        return new BinaryOpExpr(left.startToken(), left, op, right);
    }

    @Override
    public Ast visitPowExpr(NottscriptParser.PowExprContext ctx) {
        Expression left = (Expression) visit(ctx.base);
        Expression right = (Expression) visit(ctx.exponent);
        return new BinaryOpExpr(left.startToken(), left, BinaryOpExpr.BinaryOp.POWER, right);
    }

    @Override
    public Ast visitFieldAccess(NottscriptParser.FieldAccessContext ctx) {
        Expression base = (Expression) visit(ctx.base);
        String fieldName = ctx.field.getText();
        return new FieldAccessExpr(base.startToken(), base, fieldName);
    }

    @Override
    public Ast visitCallOrIndex(NottscriptParser.CallOrIndexContext ctx) {
        Expression base = (Expression) visit(ctx.base);

        List<Expression> args = (ctx.args != null) ? visitAndMap(ctx.args.exprs, Expression.class) : Collections.emptyList();

        return new CallOrIndexExpr(base.startToken(), base, args);
    }

    @Override
    public Ast visitParenExpr(NottscriptParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Ast visitNameExpr(NottscriptParser.NameExprContext ctx) {
        return new NameExpr(ctx.start, ctx.NAME().getText());
    }

    @Override
    public Ast visitOrBase(NottscriptParser.OrBaseContext ctx) {
        return visit(ctx.andExpr_());
    }

    @Override
    public Ast visitAndBase(NottscriptParser.AndBaseContext ctx) {
        return visit(ctx.relExpr_());
    }

    @Override
    public Ast visitRelBase(NottscriptParser.RelBaseContext ctx) {
        return visit(ctx.concatExpr_());
    }

    @Override
    public Ast visitConcatBase(NottscriptParser.ConcatBaseContext ctx) {
        return visit(ctx.addExpr_());
    }

    @Override
    public Ast visitAddBase(NottscriptParser.AddBaseContext ctx) {
        return visit(ctx.mulExpr_());
    }

    @Override
    public Ast visitMulBase(NottscriptParser.MulBaseContext ctx) {
        return visit(ctx.powExpr_());
    }

    @Override
    public Ast visitPowBase(NottscriptParser.PowBaseContext ctx) {
        return visit(ctx.primaryExpr());
    }

    @Override
    public Ast visitPrimaryBase(NottscriptParser.PrimaryBaseContext ctx) {
        return visit(ctx.atom());
    }

    // ==================================================================
    // Literals
    // ==================================================================

    @Override
    public Ast visitIntLit(NottscriptParser.IntLitContext ctx) {
        String text = ctx.INT_LITERAL().getText();
        return new IntLiteral(ctx.start, Integer.parseInt(text));
    }

    @Override
    public Ast visitRealLit(NottscriptParser.RealLitContext ctx) {
        throw new CompilerException(ctx.start, "Reals are not supported.");
//        String text = ctx.REAL_LITERAL().getText();
//        return new RealLiteral(ctx.start, Double.parseDouble(text));
    }

    @Override
    public Ast visitLogicalLit(NottscriptParser.LogicalLitContext ctx) {
        boolean val = ctx.LOGICAL_LITERAL().getText().equals(".true.");
        return new LogicalLiteral(ctx.start, val);
    }

    @Override
    public Ast visitCharLit(NottscriptParser.CharLitContext ctx) {
        String text = ctx.CHAR_LITERAL().getText();
        String unquoted = text.substring(1, text.length() - 1);
        String unescaped = unquoted.replace("\"\"", "\"");
        return new CharLiteral(ctx.start, unescaped);
    }

    @Override
    public Ast visitBozLitB(NottscriptParser.BozLitBContext ctx) {
        String text = ctx.B_LITERAL().getText();
        String val = text.substring(2, text.length() - 1);
        int intVal = Integer.parseInt(val, 2);
        return new IntLiteral(ctx.start, intVal);
    }

    @Override
    public Ast visitBozLitO(NottscriptParser.BozLitOContext ctx) {
        String text = ctx.O_LITERAL().getText();
        String val = text.substring(2, text.length() - 1);
        int intVal = Integer.parseInt(val, 8);
        return new IntLiteral(ctx.start, intVal);
    }

    @Override
    public Ast visitBozLitZ(NottscriptParser.BozLitZContext ctx) {
        String text = ctx.Z_LITERAL().getText();
        String val = text.substring(2, text.length() - 1);
        int intVal = Integer.parseInt(val, 16);
        return new IntLiteral(ctx.start, intVal);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void checkNameMismatchError(Token startToken, String startName, String endName) {
        if (!startName.equals(endName)) throw new CompilerException(startToken, "Declaration name '" + startName + "' does not match end name '" + endName + "'.");
    }

    private <T extends Ast> List<T> visitAndMap(List<? extends ParseTree> nodes, Class<T> clazz) {
        if (nodes == null) {
            return Collections.emptyList();
        }
        return nodes.stream().map(this::visit).map(clazz::cast).collect(Collectors.toList());
    }

    private List<String> visitANameList(NottscriptParser.NameListContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        return ctx.names.stream().map(Token::getText).collect(Collectors.toList());
    }

    private List<Dimension> visitAnArraySpec(NottscriptParser.ArraySpecContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }

        List<Dimension> result = ctx.dimList().dim().stream().map(dimCtx -> {
            if (dimCtx.INT_LITERAL() != null) {
                return new Dimension.Known(Integer.parseInt(dimCtx.INT_LITERAL().getText()));
            } else {
                return new Dimension.Unknown();
            }
        }).collect(Collectors.toList());

        if (result.isEmpty() || result.stream().allMatch(dimension -> dimension instanceof Dimension.Known) || result.stream().allMatch(dimension -> dimension instanceof Dimension.Unknown)) {
            return result;
        }
        throw new CompilerException(ctx.dimList().start, "All array dimensions must be either an integer or unknown.");
    }
}