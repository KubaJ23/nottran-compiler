package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.nott.cs.comp3012.coursework.NottscriptBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.NottscriptParser;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Dimension.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.Literal.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.IntrinsicType.VarType;

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
        checkNameMismatchError(ctx.start, name, ctx.endName, endName);

        List<VariableDecl> decls = (ctx.declarationBlock() != null) ? visitAndMap(ctx.declarationBlock().decls, VariableDecl.class) : Collections.emptyList();

        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);
        return new ProgramDecl(ctx.start, name, decls, stmts);
    }

    @Override
    public Ast visitFunctionDeclaration(NottscriptParser.FunctionDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, ctx.endName, endName);
        List<String> params = visitANameList(ctx.nameList());

        String resultName = (ctx.result != null) ? ctx.result.getText() : null;

        List<VariableDecl> decls = (ctx.declarationBlock() != null) ? visitAndMap(ctx.declarationBlock().decls, VariableDecl.class) : Collections.emptyList();

        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);

        return new FunctionDecl(ctx.start, name, params, resultName, decls, stmts);
    }

    @Override
    public Ast visitSubroutineDeclaration(NottscriptParser.SubroutineDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, ctx.endName, endName);
        List<String> params = visitANameList(ctx.nameList());

        List<VariableDecl> decls = (ctx.declarationBlock() != null) ? visitAndMap(ctx.declarationBlock().decls, VariableDecl.class) : Collections.emptyList();

        List<Statement> stmts = visitAndMap(ctx.statementBlock().stmts, Statement.class);
        return new SubroutineDecl(ctx.start, name, params, decls, stmts);
    }

    @Override
    public Ast visitDerivedTypeDeclaration(NottscriptParser.DerivedTypeDeclarationContext ctx) {
        String name = ctx.name.getText();
        String endName = ctx.endName.getText();
        checkNameMismatchError(ctx.start, name, ctx.endName, endName);

        List<VariableDecl> fields = (ctx.derivedTypeDeclBlock() != null) ? visitAndMap(ctx.derivedTypeDeclBlock().fields, VariableDecl.class) : Collections.emptyList();

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
        return new IntrinsicType(ctx.start, VarType.INTEGER, dims);
    }

    @Override
    public Ast visitRealType(NottscriptParser.RealTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, VarType.REAL, dims);
    }

    @Override
    public Ast visitCharType(NottscriptParser.CharTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, VarType.CHARACTER, dims);
    }

    @Override
    public Ast visitLogicalType(NottscriptParser.LogicalTypeContext ctx) {
        List<Dimension> dims = visitAnArraySpec(ctx.arraySpec());
        return new IntrinsicType(ctx.start, VarType.LOGICAL, dims);
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
        Expression step = (ctx.step != null) ? (Expression) visit(ctx.step) : null;
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
        List<Expression> lvalues = visitAndMap(ctx.lval, Expression.class);
        return new ReadStmt(ctx.start, lvalues);
    }

    @Override
    public Ast visitWriteStmt(NottscriptParser.WriteStmtContext ctx) {
        List<Expression> exprs = (ctx.exprList() != null) ? visitAndMap(ctx.exprList().exprs, Expression.class) : Collections.emptyList();

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
        String text = ctx.REAL_LITERAL().getText();
        return new RealLiteral(ctx.start, Double.parseDouble(text));
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
        return new BozLiteral(ctx.start, ctx.B_LITERAL().getText(), 'b');
    }

    @Override
    public Ast visitBozLitO(NottscriptParser.BozLitOContext ctx) {
        return new BozLiteral(ctx.start, ctx.O_LITERAL().getText(), 'o');
    }

    @Override
    public Ast visitBozLitZ(NottscriptParser.BozLitZContext ctx) {
        return new BozLiteral(ctx.start, ctx.Z_LITERAL().getText(), 'z');
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void checkNameMismatchError(Token startToken, String startName, Token endToken, String endName) {
        if (startName.equals(endName)) return;

        String location = "line " + endToken.getLine() + ":" + endToken.getCharPositionInLine();
        String message = "error: Mismatched end name.";

        String context = "Found '" + endName + "', but expected '" + startName + "' to match declaration on line " + startToken.getLine() + ".";

        throw new IllegalArgumentException(location + ": " + message + "\n" + context);
    }

    private void checkIllegalDimensionsError(Token startToken, List<Dimension> dimensions) {
        if (dimensions.isEmpty() || dimensions.stream().allMatch(dimension -> dimension instanceof LiteralDim) || dimensions.stream().allMatch(dimension -> dimension instanceof PointerDim)) return;
        String location = "line " + startToken.getLine() + ":" + startToken.getCharPositionInLine();
        String message = "error: Invalid Array Dimension.";

        String context = "All array dimensions must be either an integer or undefined.";

        throw new IllegalArgumentException(location + ": " + message + "\n" + context);
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
                return new LiteralDim(Integer.parseInt(dimCtx.INT_LITERAL().getText()));
            } else {
                return new PointerDim();
            }
        }).collect(Collectors.toList());

        checkIllegalDimensionsError(ctx.dimList().start, result);
        return result;
    }
}