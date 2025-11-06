package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;
import uk.ac.nott.cs.comp3012.coursework.util.types.Dimension;

import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TypeChecker extends AstBaseVisitor<Type> {

    private final Map<Ast, SymbolTable> scopeMap;
    private final Stack<SymbolTable> scopeStack = new Stack<>();

    public TypeChecker(SymbolTable globalScope, Map<Ast, SymbolTable> scopeMap) {
        this.scopeMap = scopeMap;
        scopeStack.push(globalScope);
    }

    private Type unwrapPointer(Type type) {
        if (type instanceof Type.PointerType(Type baseType)) {
            return baseType;
        }
        return type;
    }

    private boolean isLValue(Expression e) {
        return e instanceof NameExpr || e instanceof FieldAccessExpr || (e instanceof CallOrIndexExpr expr && isArrayIndex(expr));
    }

    private boolean isArrayIndex(CallOrIndexExpr expr) {
        Type t = visit(expr.base());
        Type unwrappedBase = unwrapPointer(t);
        return unwrappedBase instanceof Type.ArrayType;
    }

    private void checkExpectedType(Token token, Type expected, Type actual) {
        Type unwrappedExpected = unwrapPointer(expected);
        Type unwrappedActual = unwrapPointer(actual);

        if (!unwrappedExpected.equals(unwrappedActual)) {
            throw new CompilerException(token, "Type mismatch. Expected '" + unwrappedExpected + "' but found '" + unwrappedActual + "'.");
        }
    }

    private void visitStatementList(List<Statement> statements) {
        for (Statement s : statements) {
            visit(s);
        }
    }

    @Override
    public Type visitCompilationUnit(CompilationUnit cu) {
        for (ProgramUnit unit : cu.programUnits()) {
            if (!(unit instanceof DerivedTypeDecl)) visit(unit);
        }
        visit(cu.program());
        return null;
    }

    @Override
    public Type visitProgramDecl(ProgramDecl pd) {
        scopeStack.push(scopeMap.get(pd));
        visitStatementList(pd.statements());
        scopeStack.pop();
        return null;
    }

    @Override
    public Type visitFunctionDecl(FunctionDecl fd) {
        scopeStack.push(scopeMap.get(fd));
        visitStatementList(fd.statements());
        scopeStack.pop();
        return null;
    }

    @Override
    public Type visitSubroutineDecl(SubroutineDecl sd) {
        scopeStack.push(scopeMap.get(sd));
        visitStatementList(sd.statements());
        scopeStack.pop();
        return null;
    }

    @Override
    public Type visitAssignmentStmt(AssignmentStmt s) {
        if (!isLValue(s.lvalue())) {
            throw new CompilerException(s.lvalue().startToken(), "This expression is not an assignable location (lvalue).");
        }

        Type lType = visit(s.lvalue());
        Type rType = visit(s.rvalue());

        checkExpectedType(s.startToken(), lType, rType);
        return null;
    }

    @Override
    public Type visitCallStmt(CallStmt s) {
        Symbol sym = scopeStack.peek().getSymbol(s.subroutineName()).orElseThrow(() -> new CompilerException(s.startToken(), "Undeclared subroutine: '" + s.subroutineName() + "'"));

        if (sym.kind() != Symbol.Kind.SUBROUTINE) {
            throw new CompilerException(s.startToken(), "'" + s.subroutineName() + "' is not a subroutine.");
        }

        Type.SubroutineType subType = (Type.SubroutineType) sym.type();
        checkFunctionArguments(s.startToken(), subType.paramTypes(), s.arguments());
        return null;
    }

    @Override
    public Type visitIfSimpleStmt(IfSimpleStmt s) {
        Type condType = visit(s.condition());
        checkExpectedType(s.condition().startToken(), Type.Intrinsic.LOGICAL, condType);
        visit(s.thenStatement());
        return null;
    }

    @Override
    public Type visitIfThenElseStmt(IfThenElseStmt s) {
        Type condType = visit(s.condition());
        checkExpectedType(s.condition().startToken(), Type.Intrinsic.LOGICAL, condType);
        visitStatementList(s.thenBlock());
        visitStatementList(s.elseBlock());
        return null;
    }

    @Override
    public Type visitDoLoopStmt(DoLoopStmt s) {
        Symbol varSym = scopeStack.peek().getSymbolLocal(s.loopVariable()).orElseThrow(() -> new CompilerException(s.startToken(), "Loop variable '" + s.loopVariable() + "' is not declared in this scope."));
        checkExpectedType(s.startToken(), Type.Intrinsic.INTEGER, varSym.type());

        Type startType = visit(s.start());
        Type endType = visit(s.end());
        Type stepType = visit(s.step());

        checkExpectedType(s.start().startToken(), Type.Intrinsic.INTEGER, startType);
        checkExpectedType(s.end().startToken(), Type.Intrinsic.INTEGER, endType);
        checkExpectedType(s.step().startToken(), Type.Intrinsic.INTEGER, stepType);

        visitStatementList(s.body());
        return null;
    }

    @Override
    public Type visitDoWhileStmt(DoWhileStmt s) {
        Type condType = visit(s.condition());
        checkExpectedType(s.condition().startToken(), Type.Intrinsic.LOGICAL, condType);
        visitStatementList(s.body());
        return null;
    }

    @Override
    public Type visitReadStmt(ReadStmt s) {
        for (Expression lval : s.lvalues()) {
            if (!isLValue(lval)) throw new CompilerException(lval.startToken(), "Not a valid read lvalue.");

            Type t = visit(lval);

            if (!(unwrapPointer(t) instanceof Type.Intrinsic)) throw new CompilerException(lval.startToken(), "Can only read into intrinsic types (integer, logical, character).");

        }
        return null;
    }

    @Override
    public Type visitWriteStmt(WriteStmt s) {
        for (Expression e : s.expressions()) {
            Type t = visit(e);
            if (!(unwrapPointer(t) instanceof Type.Intrinsic)) throw new CompilerException(e.startToken(), "Can only write intrinsic types (integer, logical, character).");
        }
        return null;
    }

    @Override
    public Type visitAllocateStmt(AllocateStmt s) {
        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow(() -> new CompilerException(s.startToken(), "Undeclared variable: '" + s.pointerName() + "'"));

        if (!(sym.type() instanceof Type.PointerType(Type baseType))) {
            throw new CompilerException(s.startToken(), "'" + s.pointerName() + "' is not a POINTER.");
        }

        List<Expression> allocDimensions = s.dimensions();

        if (baseType instanceof Type.ArrayType declArrayType) {
            int expectedDims = declArrayType.dimensions().size();
            int actualDims = allocDimensions.size();

            if (expectedDims != actualDims) {
                throw new CompilerException(s.startToken(), "Incorrect number of dimensions for '" + s.pointerName() + "'. Expected " + expectedDims + " but got " + actualDims + ".");
            }

            for (Expression dimExpr : allocDimensions) {
                checkExpectedType(dimExpr.startToken(), Type.Intrinsic.INTEGER, visit(dimExpr));
            }

        } else {
            if (!allocDimensions.isEmpty()) {
                throw new CompilerException(s.startToken(), "Cannot provide dimensions when allocating a simple pointer.");
            }
        }

        return null;
    }

    @Override
    public Type visitDeallocateStmt(DeallocateStmt s) {
        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow(() -> new CompilerException(s.startToken(), "Undeclared variable: '" + s.pointerName() + "'"));

        if (!(sym.type() instanceof Type.PointerType)) {
            throw new CompilerException(s.startToken(), "'" + s.pointerName() + "' is not a POINTER.");
        }
        return null;
    }

    @Override
    public Type visitBinaryOpExpr(BinaryOpExpr e) {
        Type left = visit(e.left());
        Type right = visit(e.right());

        Type unwrappedLeft = unwrapPointer(left);
        Type unwrappedRight = unwrapPointer(right);

        switch (e.operator()) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER:
                checkExpectedType(e.left().startToken(), Type.Intrinsic.INTEGER, unwrappedLeft);
                checkExpectedType(e.right().startToken(), Type.Intrinsic.INTEGER, unwrappedRight);
                return Type.Intrinsic.INTEGER;

            case CONCAT:
                checkExpectedType(e.left().startToken(), Type.Intrinsic.CHARACTER, unwrappedLeft);
                checkExpectedType(e.right().startToken(), Type.Intrinsic.CHARACTER, unwrappedRight);
                return Type.Intrinsic.CHARACTER;

            case AND, OR:
                checkExpectedType(e.left().startToken(), Type.Intrinsic.LOGICAL, unwrappedLeft);
                checkExpectedType(e.right().startToken(), Type.Intrinsic.LOGICAL, unwrappedRight);
                return Type.Intrinsic.LOGICAL;

            case EQUAL, NOT_EQUAL:
                checkExpectedType(e.startToken(), unwrappedLeft, unwrappedRight);
                if (!(unwrappedLeft instanceof Type.Intrinsic)) {
                    throw new CompilerException(e.startToken(), "Cannot compare derived types for equality.");
                }
                return Type.Intrinsic.LOGICAL;

            case LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL:
                checkExpectedType(e.startToken(), unwrappedLeft, unwrappedRight);
                if (unwrappedLeft == Type.Intrinsic.LOGICAL) {
                    throw new CompilerException(e.startToken(), "Can only perform ordering comparisons on INTEGER or CHARACTER types.");
                }
                return Type.Intrinsic.LOGICAL;

            default:
                throw new IllegalStateException("Unexpected binary operator: " + e.operator());
        }
    }

    @Override
    public Type visitFieldAccessExpr(FieldAccessExpr e) {
        Type baseType = visit(e.base());
        Type unwrappedBase = unwrapPointer(baseType);

        if (!(unwrappedBase instanceof Type.DerivedType(String typeName, Map<String, Type> fields))) {
            throw new CompilerException(e.base().startToken(), "Base of field access is not a derived type.");
        }

        Type fieldType = fields.get(e.fieldName());
        if (fieldType == null) {
            throw new CompilerException(e.startToken(), "Type '" + typeName + "' has no field named '" + e.fieldName() + "'.");
        }

        return fieldType;
    }

    @Override
    public Type visitCallOrIndexExpr(CallOrIndexExpr e) {
        Type rawBaseType = visit(e.base());

        if (rawBaseType instanceof Type.FunctionType(Type returnType, List<Type> paramTypes)) {
            checkFunctionArguments(e.startToken(), paramTypes, e.arguments());
            return returnType;
        }

        Type unwrappedBase = unwrapPointer(rawBaseType);

        if (unwrappedBase instanceof Type.ArrayType(Type baseType, List<Dimension> dimensions)) {
            for (Expression arg : e.arguments()) {
                Type argType = visit(arg);
                checkExpectedType(arg.startToken(), Type.Intrinsic.INTEGER, argType);
            }
            if (e.arguments().size() != dimensions.size()) throw new CompilerException(e.startToken(), "Incorrect number of array dimensions. Expected " + dimensions.size() + " but got " + e.arguments().size() + ".");

            return baseType;
        }

        throw new CompilerException(e.base().startToken(), "This expression is not a function or an array, so it cannot be called or indexed.");
    }

    @Override
    public Type visitNameExpr(NameExpr e) {
        Symbol sym = scopeStack.peek().getSymbol(e.name()).orElseThrow(() -> new CompilerException(e.startToken(), "Undeclared variable: '" + e.name() + "'"));

        if (sym.kind() == Symbol.Kind.SUBROUTINE || sym.kind() == Symbol.Kind.TYPE_DEFINITION) {
            throw new CompilerException(e.startToken(), "'" + e.name() + "' is a " + sym.kind() + " and cannot be used in an expression.");
        }

        return sym.type();
    }

    private void checkFunctionArguments(Token token, List<Type> expectedTypes, List<Expression> actualArgs) {
        if (expectedTypes.size() != actualArgs.size()) {
            throw new CompilerException(token, "Incorrect number of arguments. Expected " + expectedTypes.size() + " but got " + actualArgs.size() + ".");
        }

        for (int i = 0; i < expectedTypes.size(); i++) {
            Type argType = visit(actualArgs.get(i));
            checkExpectedType(actualArgs.get(i).startToken(), expectedTypes.get(i), argType);
        }
    }

    @Override
    public Type visitIntLiteral(Literal.IntLiteral l) {
        return Type.Intrinsic.INTEGER;
    }

    @Override
    public Type visitLogicalLiteral(Literal.LogicalLiteral l) {
        return Type.Intrinsic.LOGICAL;
    }

    @Override
    public Type visitCharLiteral(Literal.CharLiteral l) {
        return Type.Intrinsic.CHARACTER;
    }
}