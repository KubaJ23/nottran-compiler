package uk.ac.nott.cs.comp3012.coursework.tam;

import uk.ac.nott.cs.comp3012.coursework.CompilerException;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.Symbol;
import uk.ac.nott.cs.comp3012.coursework.ast.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.types.Type;

import java.util.*;

/**
 * Generates an Intermediate TAM representation by visiting the AST.
 */
public class TamGenerator extends AstBaseVisitor<Void> {

    private final MemoryLayoutManager layoutManager;
    private final Map<Ast, SymbolTable> scopeMap;
    private final Map<Ast, Type> typeMap;
    private final Stack<SymbolTable> scopeStack = new Stack<>();

    private final List<IntermediateTam> ir = new ArrayList<>();
    private final Map<String, String> routineLabels = new HashMap<>();
    private int labelCounter = 0;

    /**
     * TAM Primitive routine offsets
     */
    private enum Primitives {
        ID(1), NOT(2), AND(3), OR(4), SUCC(5), PRED(6), NEG(7), ADD(8), SUB(9), MULT(10), DIV(11), MOD(12), LT(13), LE(14), GE(15), GT(16), EQ(17), NE(18), EOL(19), EOF(20), GET(21), PUT(22), GETEOL(23), PUTEOL(24), GETINT(25), PUTINT(26), NEW(27), DISPOSE(28);

        final int offset;

        Primitives(int offset) {this.offset = offset;}
    }

    public TamGenerator(Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap, MemoryLayoutManager layoutManager) {
        this.scopeMap = scopeMap;
        this.typeMap = typeMap;
        this.layoutManager = layoutManager;
    }

    public List<IntermediateTam> getIntermediateCode() {
        return ir;
    }

    // --- Helper Methods ---

    private void add(TamOpcode op, TamRegister r, int n, int d) {
        ir.add(new IntermediateTam.Instr(op, r, n, d));
    }

    private void callPrimitive(Primitives prim, int n) {
        add(TamOpcode.CALL, TamRegister.PB, n, prim.offset);
    }

    private void addJump(String label) {
        ir.add(new IntermediateTam.Jump(TamOpcode.JUMP, TamRegister.CB, 0, label));
    }

    /**
     * Add instruction to jump to the label if a word popped from the stack equals n
     *
     * @param n     Used to check whether the jump should be taken or not based on if n equals a 1-word value that is popped from the stack
     * @param label Label to jump to
     */
    private void addJumpIf(int n, String label) {
        ir.add(new IntermediateTam.Jump(TamOpcode.JUMPIF, TamRegister.CB, n, label));
    }

    private void addCall(String label) {
        // Static link for register LB is used (8). Functions are not nested so static link is always LB.
        ir.add(new IntermediateTam.Jump(TamOpcode.CALL, TamRegister.LB, 8, label));
    }

    private String newLabel(String hint) {
        return hint + "_" + labelCounter++;
    }

    private void placeLabel(String label) {
        ir.add(new IntermediateTam.Label(label));
    }

    private void visitStatementList(List<Statement> statements) {
        for (Statement s : statements) {
            visit(s);
        }
    }

    private Type getBaseType(Ast node) {
        Type type = typeMap.get(node);
        if (type instanceof Type.PointerType(Type baseType)) {
            return baseType;
        }
        return type;
    }


    @Override
    public Void visitCompilationUnit(CompilationUnit cu) {
        scopeStack.push(scopeMap.get(cu));

        // Register all routine labels
        routineLabels.put(cu.program().name(), "main");
        for (ProgramUnit unit : cu.programUnits()) {
            if (unit instanceof FunctionDecl fd) {
                routineLabels.put(fd.name(), fd.name());
            } else if (unit instanceof SubroutineDecl sd) {
                routineLabels.put(sd.name(), sd.name());
            }
        }

        addCall("main");

        add(TamOpcode.HALT, null, 0, 0);

        for (ProgramUnit unit : cu.programUnits()) {
            if (!(unit instanceof DerivedTypeDecl)) {
                visit(unit);
            }
        }

        placeLabel("main");
        visit(cu.program());

        add(TamOpcode.RETURN, null, 0, 0);

        scopeStack.pop();
        return null;
    }

    @Override
    public Void visitProgramDecl(ProgramDecl pd) {
        scopeStack.push(scopeMap.get(pd));

        int localDataSize = layoutManager.getLocalDataSize(scopeStack.peek());
        if (localDataSize > 0) {
            add(TamOpcode.PUSH, null, 0, localDataSize);
        }

        visitStatementList(pd.statements());

        scopeStack.pop();
        return null;
    }

    @Override
    public Void visitFunctionDecl(FunctionDecl fd) {
        scopeStack.push(scopeMap.get(fd));
        placeLabel(fd.name());

        int localDataSize = layoutManager.getLocalDataSize(scopeStack.peek());
        if (localDataSize > 0) {
            add(TamOpcode.PUSH, null, 0, localDataSize);
        }

        visitStatementList(fd.statements());

        SymbolTable scope = scopeStack.peek();
        String resultName = (fd.resultName() != null) ? fd.resultName() : fd.name();
        Symbol resultSym = scope.getSymbolLocal(resultName).orElseThrow();
        MemoryLayoutManager.TamAddress resultAddr = layoutManager.getAddress(resultSym);
        int resultSize = resultAddr.size();

        // Load the result value onto the stack
        add(TamOpcode.LOAD, TamRegister.LB, resultSize, resultAddr.offset());

        int paramDataSize = layoutManager.getParameterDataSize(scope);
        add(TamOpcode.RETURN, null, resultSize, paramDataSize);

        scopeStack.pop();
        return null;
    }

    @Override
    public Void visitSubroutineDecl(SubroutineDecl sd) {
        scopeStack.push(scopeMap.get(sd));
        placeLabel(sd.name());

        int localDataSize = layoutManager.getLocalDataSize(scopeStack.peek());
        if (localDataSize > 0) {
            add(TamOpcode.PUSH, null, 0, localDataSize);
        }

        visitStatementList(sd.statements());

        int paramDataSize = layoutManager.getParameterDataSize(scopeStack.peek());
        add(TamOpcode.RETURN, null, 0, paramDataSize); // n=0 (no result)

        scopeStack.pop();
        return null;
    }

    // Statements

    @Override
    public Void visitAssignmentStmt(AssignmentStmt s) {
        visit(s.rvalue());
        visitForAddress(s.lvalue());
        int size = layoutManager.getSize(typeMap.get(s.lvalue()));
        add(TamOpcode.STOREI, null, size, 0);
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt s) {
        for (Expression arg : s.arguments()) {
            visitForParameter(arg);
        }
        addCall(routineLabels.get(s.subroutineName()));
        return null;
    }

    @Override
    public Void visitIfSimpleStmt(IfSimpleStmt s) {
        String endLabel = newLabel("if_end");

        visit(s.condition());
        addJumpIf(0, endLabel); // Jump if 0 (false)

        visit(s.thenStatement());
        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitIfThenElseStmt(IfThenElseStmt s) {
        String elseLabel = newLabel("if_else");
        String endLabel = newLabel("if_end");

        visit(s.condition());
        addJumpIf(0, elseLabel); // Jump if 0 (false)

        visitStatementList(s.thenBlock());
        addJump(endLabel);

        placeLabel(elseLabel);
        visitStatementList(s.elseBlock());

        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitDoLoopStmt(DoLoopStmt s) {
        String startLabel = newLabel("do_start");
        String endLabel = newLabel("do_end");

        visit(s.start());
        NameExpr nameExpr = new NameExpr(s.startToken(), s.loopVariable());
        visitForAddress(nameExpr);
        add(TamOpcode.STOREI, null, 1, 0); // size=1

        placeLabel(startLabel);

        visit(nameExpr);
        visit(s.end());
        callPrimitive(Primitives.GE, 2);
        addJumpIf(1, endLabel); // Jump if 1 (true)

        visitStatementList(s.body());

        visit(nameExpr);
        visit(s.step());
        callPrimitive(Primitives.ADD, 2);
        visitForAddress(nameExpr);
        add(TamOpcode.STOREI, null, 1, 0); // Store

        addJump(startLabel);
        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitDoWhileStmt(DoWhileStmt s) {
        String startLabel = newLabel("while_start");
        String endLabel = newLabel("while_end");

        placeLabel(startLabel);
        visit(s.condition());
        addJumpIf(0, endLabel); // Jump if 0 (false)

        visitStatementList(s.body());
        addJump(startLabel);
        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitReadStmt(ReadStmt s) {
        for (Expression lval : s.lvalues()) {
            Type baseType = getBaseType(lval);
            if (baseType != Type.Intrinsic.INTEGER) {
                throw new CompilerException(lval.startToken(), "READ only supports INTEGER variables.");
            }
            visitForAddress(lval);
            callPrimitive(Primitives.GETINT, 1);
        }
        return null;
    }

    @Override
    public Void visitWriteStmt(WriteStmt s) {
        for (Expression e : s.expressions()) {
            visit(e);
            Type baseType = getBaseType(e);
            if (baseType == Type.Intrinsic.INTEGER || baseType == Type.Intrinsic.LOGICAL) {
                callPrimitive(Primitives.PUTINT, 1);
            } else if (baseType == Type.Intrinsic.CHARACTER) {
                generateWriteString();
            } else {
                throw new CompilerException(e.startToken(), "Cannot WRITE complex types (arrays/structs) directly.");
            }
            callPrimitive(Primitives.PUTEOL, 0);
        }
        return null;
    }

    private void generateWriteString() {
        String loopStart = newLabel("write_str_start");
        String loopEnd = newLabel("write_str_end");

        add(TamOpcode.LOADL, null, 0, 0);
        callPrimitive(Primitives.ADD, 2);
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);

        add(TamOpcode.LOADL, null, 0, 0);
        callPrimitive(Primitives.ADD, 2);
        add(TamOpcode.LOADI, null, 1, 0);

        add(TamOpcode.LOADL, null, 0, 1);

        placeLabel(loopStart);
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        callPrimitive(Primitives.GT, 2);

        addJumpIf(1, loopEnd);

        add(TamOpcode.LOAD, TamRegister.ST, 1, -3);
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        callPrimitive(Primitives.ADD, 2);
        add(TamOpcode.LOADI, null, 1, 0);

        callPrimitive(Primitives.PUT, 1);

        add(TamOpcode.LOADL, null, 0, 1);
        callPrimitive(Primitives.ADD, 2);

        addJump(loopStart);

        placeLabel(loopEnd);
        add(TamOpcode.POP, null, 0, 3);
    }


    @Override
    public Void visitAllocateStmt(AllocateStmt s) {
        Symbol ptrSymbol = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        Type.PointerType pt = (Type.PointerType) ptrSymbol.type();
        int baseSize = layoutManager.getSize(pt.baseType());

        if (s.dimensions().isEmpty()) {
            add(TamOpcode.LOADL, null, 0, baseSize);
        } else {
            add(TamOpcode.LOADL, null, 0, baseSize);
            for (Expression dimExpr : s.dimensions()) {
                visit(dimExpr);
                callPrimitive(Primitives.MULT, 2);
            }
        }
        callPrimitive(Primitives.NEW, 1);

        MemoryLayoutManager.TamAddress ptrAddr = layoutManager.getAddress(ptrSymbol);
        add(TamOpcode.LOADA, TamRegister.LB, 0, ptrAddr.offset());

        add(TamOpcode.STOREI, null, 1, 0);
        return null;
    }

    @Override
    public Void visitDeallocateStmt(DeallocateStmt s) {
        add(TamOpcode.LOADL, null, 0, 1);

        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());

        callPrimitive(Primitives.DISPOSE, 2);
        return null;
    }

    // --- Expressions (push value to stack) ---

    @Override
    public Void visitBinaryOpExpr(BinaryOpExpr e) {
        visit(e.left());
        visit(e.right());

        Type leftType = getBaseType(e.left());
        int size = layoutManager.getSize(leftType);

        Primitives prim = switch (e.operator()) {
            case ADD -> Primitives.ADD;
            case SUBTRACT -> Primitives.SUB;
            case MULTIPLY -> Primitives.MULT;
            case DIVIDE -> Primitives.DIV;
            case EQUAL -> Primitives.EQ;
            case NOT_EQUAL -> Primitives.NE;
            case LESS_THAN -> Primitives.LT;
            case LESS_EQUAL -> Primitives.LE;
            case GREATER_THAN -> Primitives.GT;
            case GREATER_EQUAL -> Primitives.GE;
            case AND -> Primitives.AND;
            case OR -> Primitives.OR;
            case CONCAT -> throw new UnsupportedOperationException("String CONCAT not implemented");
            case POWER -> throw new UnsupportedOperationException("POWER not implemented");
        };

        if (prim == Primitives.EQ || prim == Primitives.NE) {
            add(TamOpcode.LOADL, null, 0, size);
            callPrimitive(prim, 3);
        } else {
            callPrimitive(prim, 2);
        }
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr e) {
        visitForAddress(e);
        int size = layoutManager.getSize(typeMap.get(e));
        add(TamOpcode.LOADI, null, size, 0);
        return null;
    }

    @Override
    public Void visitCallOrIndexExpr(CallOrIndexExpr e) {
        Type baseAstType = typeMap.get(e.base());

        if (baseAstType instanceof Type.FunctionType) {
            for (Expression arg : e.arguments()) {
                visitForParameter(arg);
            }
            String funcName = ((NameExpr) e.base()).name();
            addCall(routineLabels.get(funcName));
        } else {
            visitForAddress(e);
            int size = layoutManager.getSize(typeMap.get(e));
            add(TamOpcode.LOADI, null, size, 0);
        }
        return null;
    }

    @Override
    public Void visitNameExpr(NameExpr e) {
        Symbol sym = scopeStack.peek().getSymbol(e.name()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        Type type = sym.type();

        if (type instanceof Type.PointerType) {
            // DEFAULT RULE: Implicitly dereference.
            add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr
            add(TamOpcode.LOADI, null, 1, 0); // Pushes value at heap_addr
        } else {
            add(TamOpcode.LOAD, TamRegister.LB, addr.size(), addr.offset());
        }
        return null;
    }

    @Override
    public Void visitIntLiteral(Literal.IntLiteral l) {
        add(TamOpcode.LOADL, null, 0, l.value());
        return null;
    }

    @Override
    public Void visitLogicalLiteral(Literal.LogicalLiteral l) {
        add(TamOpcode.LOADL, null, 0, l.value() ? 1 : 0);
        return null;
    }

    @Override
    public Void visitCharLiteral(Literal.CharLiteral l) {
        String s = l.value();
        int len = s.length();
        int totalSizeInWords = 1 + len;

        add(TamOpcode.LOADL, null, 0, totalSizeInWords);
        callPrimitive(Primitives.NEW, 1);

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        add(TamOpcode.LOADL, null, 0, 0);
        callPrimitive(Primitives.ADD, 2);
        add(TamOpcode.LOADL, null, 0, len);
        add(TamOpcode.STOREI, null, 1, 0);

        for (int i = 0; i < len; i++) {
            int charVal = s.charAt(i);
            int charIndex = i + 1;

            add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
            add(TamOpcode.LOADL, null, 0, charIndex);
            callPrimitive(Primitives.ADD, 2);
            add(TamOpcode.LOADL, null, 0, charVal);
            add(TamOpcode.STOREI, null, 1, 0);
        }

        return null;
    }

    // --- L-Value (push address to stack) ---

    private void visitForAddress(Expression e) {
        switch (e) {
            case NameExpr ne -> {
                Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
                MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

                if (sym.type() instanceof Type.PointerType) {
                    // DEFAULT RULE: Implicitly dereference.
                    add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
                } else {
                    add(TamOpcode.LOADA, TamRegister.LB, 0, addr.offset());
                }
            }
            case FieldAccessExpr(org.antlr.v4.runtime.Token startToken, Expression base, String fieldName) -> {
                visitForAddress(base);

                Type baseType = getBaseType(base);
                if (!(baseType instanceof Type.DerivedType dt)) {
                    throw new CompilerException(startToken, "Base of field access is not a derived type.");
                }
                int fieldOffset = layoutManager.getFieldOffset(dt, fieldName);

                add(TamOpcode.LOADL, null, 0, fieldOffset);
                callPrimitive(Primitives.ADD, 2);
            }
            case CallOrIndexExpr cie -> {
                Symbol baseSym = scopeStack.peek().getSymbol(((NameExpr) cie.base()).name()).orElseThrow();
                MemoryLayoutManager.TamAddress baseAddr = layoutManager.getAddress(baseSym);

                if (getBaseType(cie.base()) instanceof Type.ArrayType) {
                    add(TamOpcode.LOADA, TamRegister.LB, 0, baseAddr.offset());
                } else {
                    add(TamOpcode.LOAD, TamRegister.LB, 1, baseAddr.offset());
                }

                visit(cie.arguments().getFirst());

                callPrimitive(Primitives.ADD, 2);
            }
            default -> throw new CompilerException(e.startToken(), "This expression is not an assignable lvalue.");
        }
    }

    private void visitForParameter(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

            if (sym.type() instanceof Type.PointerType) {
                // EXCEPTION 1: Pass-as-parameter.
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
            } else {
                visit(e);
            }
        } else {
            visit(e);
        }
    }
}