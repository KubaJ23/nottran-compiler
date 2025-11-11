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
        if (type instanceof Type.PointerType pt) {
            return pt.baseType();
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

        // Padding here are the first code is not called from anywhere
        add(TamOpcode.PUSH, null, 0, 3);
        visit(cu.program());

        for (ProgramUnit unit : cu.programUnits()) {
            if (!(unit instanceof DerivedTypeDecl)) {
                visit(unit);
            }
        }

        add(TamOpcode.HALT, null, 0, 0);

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
        String resultName = fd.resultName();
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
        // 1. Evaluate r-value, leave its value(s) on the stack
        // This will use visitNameExpr, which handles the default
        // implicit dereference for any pointers on the right-hand side.
        visit(s.rvalue());

        // 2. Get the *address* of the l-value, leave it on the stack
        // This will use visitForAddress, which handles the implicit
        // dereference for any pointer on the left-hand side.
        visitForAddress(s.lvalue());

        // 3. Store
        int size = layoutManager.getSize(typeMap.get(s.lvalue()));
        add(TamOpcode.STOREI, TamRegister.CB, size, 0);
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt s) {
        // 1. Push arguments
        for (Expression arg : s.arguments()) {
            // EXCEPTION 1: Use special helper for parameters
            visitForParameter(arg);
        }
        // 2. Call (static call, use LB as static link [n=8])
        addCall(routineLabels.get(s.subroutineName()));
        return null;
    }

    @Override
    public Void visitIfSimpleStmt(IfSimpleStmt s) {
        String endLabel = newLabel("if_end");

        visit(s.condition()); // Leaves 0 (false) or 1 (true) on stack
        addJumpIf(0, endLabel); // Jump if 0 (false)

        visit(s.thenStatement());
        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitIfThenElseStmt(IfThenElseStmt s) {
        String elseLabel = newLabel("if_else");
        String endLabel = newLabel("if_end");

        visit(s.condition()); // Leaves 0 or 1 on stack
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

        // 1. Assign loopVar = startVal
        visit(s.start());
        visitForAddress(new NameExpr(s.startToken(), s.loopVariable()));
        add(TamOpcode.STOREI, TamRegister.CB, 1, 0); // size=1

        placeLabel(startLabel);

        // 2. Condition check: if (loopVar >= endVal) goto end
        visit(new NameExpr(s.startToken(), s.loopVariable())); // Push loopVar
        visit(s.end()); // Push endVal
        callPrimitive(Primitives.GE, 2); // Stack: [loopVar >= endVal]
        addJumpIf(1, endLabel); // Jump if 1 (true)

        // 3. Body
        visitStatementList(s.body());

        // 4. Increment: loopVar = loopVar + stepVal
        visit(new NameExpr(s.startToken(), s.loopVariable())); // Push loopVar
        visit(s.step()); // Push stepVal
        callPrimitive(Primitives.ADD, 2); // Stack: [new_val]
        visitForAddress(new NameExpr(s.startToken(), s.loopVariable())); // Push &loopVar
        add(TamOpcode.STOREI, TamRegister.CB, 1, 0); // Store

        // 5. Loop
        addJump(startLabel);
        placeLabel(endLabel);
        return null;
    }

    @Override
    public Void visitDoWhileStmt(DoWhileStmt s) {
        String startLabel = newLabel("while_start");
        String endLabel = newLabel("while_end");

        placeLabel(startLabel);
        visit(s.condition()); // Stack: [condition]
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
            // visitForAddress handles implicit deref for pointer l-values
            visitForAddress(lval); // Stack: [&lval]
            callPrimitive(Primitives.GETINT, 1); // getint(a)
        }
        return null;
    }

    @Override
    public Void visitWriteStmt(WriteStmt s) {
        for (Expression e : s.expressions()) {
            // visit(e) handles implicit deref for pointer r-values
            visit(e); // Stack: [value]
            Type baseType = getBaseType(e);
            if (baseType == Type.Intrinsic.INTEGER || baseType == Type.Intrinsic.LOGICAL) {
                callPrimitive(Primitives.PUTINT, 1);
            } else if (baseType == Type.Intrinsic.CHARACTER) {
                generateWriteString();
            } else {
                throw new CompilerException(e.startToken(), "Cannot WRITE complex types (arrays/structs) directly.");
            }
            callPrimitive(Primitives.PUTEOL, 0); // Add a newline
        }
        return null;
    }

    /**
     * Helper to write a string. Assumes string heap address is on the stack.
     */
    private void generateWriteString() {
        String loopStart = newLabel("write_str_start");
        String loopEnd = newLabel("write_str_end");

        // Stack: [s_ptr]
        add(TamOpcode.LOADL, TamRegister.CB, 0, 0); // Stack: [s_ptr, 0]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Stack: [s_ptr, s_ptr] (dup)

        // Get len = s_ptr[0]
        add(TamOpcode.LOADL, TamRegister.CB, 0, 0); // Stack: [s_ptr, s_ptr, 0]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr, &s_ptr[0]]
        add(TamOpcode.LOADI, TamRegister.CB, 1, 0); // Stack: [s_ptr, len]

        // i = 1
        add(TamOpcode.LOADL, TamRegister.CB, 0, 1); // Stack: [s_ptr, len, i=1]

        placeLabel(loopStart);
        // Stack: [s_ptr, len, i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Stack: [s_ptr, len, i, i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Stack: [s_ptr, len, i, i, len]
        callPrimitive(Primitives.GT, 2); // Stack: [s_ptr, len, i, i > len]

        addJumpIf(1, loopEnd); // Jump if (i > len)

        // Stack: [s_ptr, len, i]
        // char = s_ptr[i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // Stack: [s_ptr, len, i, s_ptr]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Stack: [s_ptr, len, i, s_ptr, i]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr, len, i, &s_ptr[i]]
        add(TamOpcode.LOADI, TamRegister.CB, 1, 0); // Stack: [s_ptr, len, i, char]

        // put(char)
        callPrimitive(Primitives.PUT, 1); // Stack: [s_ptr, len, i]

        // i = i + 1
        add(TamOpcode.LOADL, TamRegister.CB, 0, 1); // Stack: [s_ptr, len, i, 1]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr, len, i+1]

        addJump(loopStart);

        placeLabel(loopEnd);
        // Clean up stack
        add(TamOpcode.POP, TamRegister.CB, 0, 3); // Pop s_ptr, len, i
    }


    @Override
    public Void visitAllocateStmt(AllocateStmt s) {
        // EXCEPTION 2: ALLOCATE p

        // 1. Compute total size
        Symbol ptrSymbol = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        Type.PointerType pt = (Type.PointerType) ptrSymbol.type();
        int baseSize = layoutManager.getSize(pt.baseType());

        if (s.dimensions().isEmpty()) {
            add(TamOpcode.LOADL, TamRegister.CB, 0, baseSize);
        } else {
            add(TamOpcode.LOADL, TamRegister.CB, 0, baseSize);
            for (Expression dimExpr : s.dimensions()) {
                visit(dimExpr);
                callPrimitive(Primitives.MULT, 2);
            }
        }
        // Stack: [total_size]
        callPrimitive(Primitives.NEW, 1); // Stack: [new_heap_addr]

        // 2. Store result in pointer variable 'p'
        // This is an exception, we need the *stack address* of 'p'
        MemoryLayoutManager.TamAddress ptrAddr = layoutManager.getAddress(ptrSymbol);
        add(TamOpcode.LOADA, TamRegister.LB, 0, ptrAddr.offset()); // Pushes &p
        // Stack: [new_heap_addr, &p]

        add(TamOpcode.STOREI, TamRegister.CB, 1, 0); // Store pointer
        return null;
    }

    @Override
    public Void visitDeallocateStmt(DeallocateStmt s) {
        // EXCEPTION 3: DEALLOCATE p

        // We assume 'dispose' primitive is `dispose(n, a)`
        // We will guess n=1 as the spec is ambiguous.
        add(TamOpcode.LOADL, TamRegister.CB, 0, 1); // n=1 (size guess)

        // This is an exception. We must push the pointer's real value
        // (the heap address) without dereferencing it.
        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr

        callPrimitive(Primitives.DISPOSE, 2); // n=2 (size, addr)
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
            // EQ/NE take n (size) as a 3rd argument
            add(TamOpcode.LOADL, TamRegister.CB, 0, size); // Push size 'n'
            callPrimitive(prim, 3); // n=3 (v1, v2, n)
        } else {
            callPrimitive(prim, 2); // n=2 (v1, v2)
        }
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr e) {
        // R-Value: s%field
        // This is a "default use", so it implicitly dereferences.
        visitForAddress(e); // Pushes &s%field
        int size = layoutManager.getSize(typeMap.get(e));
        add(TamOpcode.LOADI, TamRegister.CB, size, 0);
        return null;
    }

    @Override
    public Void visitCallOrIndexExpr(CallOrIndexExpr e) {
        Type baseAstType = typeMap.get(e.base());

        if (baseAstType instanceof Type.FunctionType) {
            // --- Function Call ---
            for (Expression arg : e.arguments()) {
                // EXCEPTION 1: Use special helper for parameters
                visitForParameter(arg);
            }
            String funcName = ((NameExpr) e.base()).name();
            addCall(routineLabels.get(funcName));
            // Result is left on the stack.
        } else {
            // --- Array/Pointer Index Read ---
            // This is a "default use", so it implicitly dereferences.
            visitForAddress(e); // Pushes &arr(i) or &p(i)
            int size = layoutManager.getSize(typeMap.get(e));
            add(TamOpcode.LOADI, TamRegister.CB, size, 0);
        }
        return null;
    }

    /**
     * visitNameExpr: This is the "default r-value" case.
     * It MUST handle implicit dereferencing.
     */
    @Override
    public Void visitNameExpr(NameExpr e) {
        Symbol sym = scopeStack.peek().getSymbol(e.name()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        Type type = sym.type();

        if (type instanceof Type.PointerType) {
            // DEFAULT RULE: Implicitly dereference.
            // This is NOT an exception.
            add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr
            add(TamOpcode.LOADI, TamRegister.CB, 1, 0); // Pushes value at heap_addr
        } else {
            // Not a pointer. Load its value (which could be 1 or N words).
            add(TamOpcode.LOAD, TamRegister.LB, addr.size(), addr.offset());
        }
        return null;
    }

    @Override
    public Void visitIntLiteral(Literal.IntLiteral l) {
        add(TamOpcode.LOADL, TamRegister.CB, 0, l.value());
        return null;
    }

    @Override
    public Void visitLogicalLiteral(Literal.LogicalLiteral l) {
        add(TamOpcode.LOADL, TamRegister.CB, 0, l.value() ? 1 : 0);
        return null;
    }

    @Override
    public Void visitCharLiteral(Literal.CharLiteral l) {
        // Allocates a string on the heap and leaves its pointer on the stack
        String s = l.value();
        int len = s.length();
        int totalSizeInWords = 1 + len; // 1 word for length + N words for chars

        // 1. Allocate heap memory
        add(TamOpcode.LOADL, TamRegister.CB, 0, totalSizeInWords);
        callPrimitive(Primitives.NEW, 1); // Stack: [s_ptr]

        // 2. Store length at s_ptr[0]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Stack: [s_ptr, s_ptr] (dup)
        add(TamOpcode.LOADL, TamRegister.CB, 0, 0); // Stack: [s_ptr, s_ptr, 0]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr, &s_ptr[0]]
        add(TamOpcode.LOADL, TamRegister.CB, 0, len); // Stack: [s_ptr, &s_ptr[0], len]
        add(TamOpcode.STOREI, TamRegister.CB, 1, 0); // Stack: [s_ptr]

        // 3. Store each character
        for (int i = 0; i < len; i++) {
            int charVal = (int) s.charAt(i);
            int charIndex = i + 1;

            add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Stack: [s_ptr, s_ptr] (dup)
            add(TamOpcode.LOADL, TamRegister.CB, 0, charIndex); // Stack: [s_ptr, s_ptr, index]
            callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr, &s_ptr[i]]
            add(TamOpcode.LOADL, TamRegister.CB, 0, charVal); // Stack: [s_ptr, &s_ptr[i], char]
            add(TamOpcode.STOREI, TamRegister.CB, 1, 0); // Stack: [s_ptr]
        }

        // The heap pointer is left on the stack.
        return null;
    }

    // --- L-Value (push address to stack) ---

    /**
     * Emits code to push the *address* of an l-value onto the stack.
     * This is the "default l-value" case, so it MUST handle
     * implicit dereferencing.
     */
    private void visitForAddress(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

            if (sym.type() instanceof Type.PointerType) {
                // DEFAULT RULE: Implicitly dereference.
                // Get the heap address, which is where we will store.
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
            } else {
                // Not a pointer. Get the stack address.
                add(TamOpcode.LOADA, TamRegister.LB, 0, addr.offset());
            }

        } else if (e instanceof FieldAccessExpr fe) {
            // LValue is s%field
            // This is a default use, so implicitly dereference base 's' if it's a pointer.
            visitForAddress(fe.base()); // Pushes address of 's' (or heap addr if 's' is ptr)

            Type baseType = getBaseType(fe.base()); // Gets 's' type, not 's pointer' type
            if (!(baseType instanceof Type.DerivedType dt)) {
                throw new CompilerException(fe.startToken(), "Base of field access is not a derived type.");
            }
            int fieldOffset = layoutManager.getFieldOffset(dt, fe.fieldName());

            add(TamOpcode.LOADL, TamRegister.CB, 0, fieldOffset); // Pushes offset
            callPrimitive(Primitives.ADD, 2); // Pops addr, offset; pushes addr+offset

        } else if (e instanceof CallOrIndexExpr cie) {
            // LValue is arr(i) or p(i)
            Symbol baseSym = scopeStack.peek().getSymbol(((NameExpr) cie.base()).name()).orElseThrow();
            MemoryLayoutManager.TamAddress baseAddr = layoutManager.getAddress(baseSym);

            if (getBaseType(cie.base()) instanceof Type.ArrayType) {
                // Static array: push &arr
                add(TamOpcode.LOADA, TamRegister.LB, 0, baseAddr.offset());
            } else {
                // Pointer: push heap_addr from p
                add(TamOpcode.LOAD, TamRegister.LB, 1, baseAddr.offset());
            }

            // Compute offset (simplified to 1D)
            visit(cie.arguments().get(0)); // Pushes index

            // Add them
            callPrimitive(Primitives.ADD, 2); // Pops base_addr, index; pushes (base_addr + index)

        } else {
            throw new CompilerException(e.startToken(), "This expression is not an assignable lvalue.");
        }
    }

    /**
     * Pushes the value of an expression for use as a parameter.
     * This is the same as visit(), but it does *not* implicitly dereference
     * simple pointers (e.g., "integer pointer :: p").
     */
    private void visitForParameter(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

            if (sym.type() instanceof Type.PointerType) {
                // EXCEPTION 1: Pass-as-parameter.
                // Push the pointer's real value (heap addr). DO NOT DEREFERENCE.
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
            } else {
                // Not a pointer. Just visit normally for its value.
                visit(e);
            }
        } else {
            // Not a simple name (e.g., an expression like "x+1").
            // Visit normally for its value.
            visit(e);
        }
    }
}