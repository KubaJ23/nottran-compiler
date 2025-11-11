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
        ir.add(new IntermediateTam.Instr(TamOpcode.CALL, TamRegister.PB, n, prim.offset));
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

    private void visitForParameter(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
            Type type = sym.type();
            int size = addr.size();

            if (type instanceof Type.PointerType(Type baseType) && !(baseType instanceof Type.ArrayType)) {
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // size=1
                return;
            }
        }
        visit(e);
    }

    private Type getBaseType(Ast node) {
        Type type = typeMap.get(node);
        if (type instanceof Type.PointerType pt) {
            return pt.baseType();
        }
        return type;
    }

    // --- Program Units ---

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

        // 1. JUMP to main. TAM execution starts at CB (addr 0).
        addJump("main");

        // 2. Visit all functions/subroutines
        for (ProgramUnit unit : cu.programUnits()) {
            if (!(unit instanceof DerivedTypeDecl)) {
                visit(unit);
            }
        }

        // 3. Place main label
        placeLabel("main");
        visit(cu.program());

        // 4. Halt
        add(TamOpcode.HALT, TamRegister.CB, 0, 0);

        scopeStack.pop();
        return null;
    }

    @Override
    public Void visitProgramDecl(ProgramDecl pd) {
        scopeStack.push(scopeMap.get(pd));

        int localDataSize = layoutManager.getLocalDataSize(scopeStack.peek());
        if (localDataSize > 0) {
            add(TamOpcode.PUSH, TamRegister.CB, 0, localDataSize);
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
            add(TamOpcode.PUSH, TamRegister.CB, 0, localDataSize);
        }

        visitStatementList(fd.statements());

        // --- Implicit Return ---
        SymbolTable scope = scopeStack.peek();
        String resultName = (fd.resultName() != null) ? fd.resultName() : fd.name();
        Symbol resultSym = scope.getSymbolLocal(resultName).orElseThrow();
        MemoryLayoutManager.TamAddress resultAddr = layoutManager.getAddress(resultSym);
        int resultSize = resultAddr.size();

        // Load the result value onto the stack
        add(TamOpcode.LOAD, TamRegister.LB, resultSize, resultAddr.offset());

        // Add RETURN instruction
        int paramDataSize = layoutManager.getParameterDataSize(scope);
        add(TamOpcode.RETURN, TamRegister.CB, resultSize, paramDataSize);

        scopeStack.pop();
        return null;
    }

    @Override
    public Void visitSubroutineDecl(SubroutineDecl sd) {
        scopeStack.push(scopeMap.get(sd));
        placeLabel(sd.name());

        int localDataSize = layoutManager.getLocalDataSize(scopeStack.peek());
        if (localDataSize > 0) {
            add(TamOpcode.PUSH, TamRegister.CB, 0, localDataSize);
        }

        visitStatementList(sd.statements());

        // --- Implicit Return ---
        int paramDataSize = layoutManager.getParameterDataSize(scopeStack.peek());
        add(TamOpcode.RETURN, TamRegister.CB, 0, paramDataSize); // n=0 (no result)

        scopeStack.pop();
        return null;
    }

    // --- Statements ---

    @Override
    public Void visitAssignmentStmt(AssignmentStmt s) {
        // 1. Evaluate r-value, leave its value(s) on the stack
        visit(s.rvalue());

        // 2. Get the *address* of the l-value, leave it on the stack
        visitForAddress(s.lvalue());

        // 3. Store
        int size = layoutManager.getSize(typeMap.get(s.lvalue()));
        add(TamOpcode.STOREI, TamRegister.CB, size, 0);
        return null;
    }

    // In TamGenerator.java, REPLACE the old visitCallStmt

    @Override
    public Void visitCallStmt(CallStmt s) {
        // 1. Push arguments using the new parameter helper
        for (Expression arg : s.arguments()) {
            visitForParameter(arg); // <-- THE FIX
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
            visitForAddress(lval); // Stack: [&lval]
            callPrimitive(Primitives.GETINT, 1); // getint(a)
        }
        return null;
    }

    @Override
    public Void visitWriteStmt(WriteStmt s) {
        for (Expression e : s.expressions()) {
            visit(e); // Stack: [value]
            Type baseType = getBaseType(e);
            if (baseType == Type.Intrinsic.INTEGER || baseType == Type.Intrinsic.LOGICAL) {
                callPrimitive(Primitives.PUTINT, 1);
            } else if (baseType == Type.Intrinsic.CHARACTER) {
                // This requires a new helper function
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
     * Implements:
     * len = s_ptr[0]
     * i = 1
     * while (i <= len)
     * char = s_ptr[i]
     * put(char)
     * i = i + 1
     */
    private void generateWriteString() {
        String loopStart = newLabel("write_str_start");
        String loopEnd = newLabel("write_str_end");

        // Stack: [s_ptr]
        add(TamOpcode.LOADL, TamRegister.CB, 0, 0); // Stack: [s_ptr, 0]
        callPrimitive(Primitives.ADD, 2); // Stack: [s_ptr] (for safety, ensure it's an address)
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
        Symbol ptrSymbol = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        Type.PointerType pt = (Type.PointerType) ptrSymbol.type();
        int baseSize = layoutManager.getSize(pt.baseType());

        if (s.dimensions().isEmpty()) {
            add(TamOpcode.LOADL, TamRegister.CB, 0, baseSize); // n=1, d=size
        } else {
            // Calculate size: dim1 * dim2 * ... * baseSize
            add(TamOpcode.LOADL, TamRegister.CB, 0, baseSize);
            for (Expression dimExpr : s.dimensions()) {
                visit(dimExpr); // Pushes dim value
                callPrimitive(Primitives.MULT, 2);
            }
        }
        // Stack: [total_size]
        callPrimitive(Primitives.NEW, 1); // Stack: [new_heap_addr]

        // Store result in pointer variable (which is always 1 word)
        visitForAddress(new NameExpr(s.startToken(), s.pointerName()));
        // Stack: [new_heap_addr, &ptrVar]
        add(TamOpcode.STOREI, TamRegister.CB, 1, 0);
        return null;
    }

    @Override
    public Void visitDeallocateStmt(DeallocateStmt s) {
        // TAM's `dispose` primitive is `dispose n, a`
        // We need to know the size 'n' that was allocated.
        // This language spec doesn't store it, which is a problem.
        // We will *assume* the size is stored at address `a-1`
        // or just guess '1'. Let's just guess 1 for simplicity.

        add(TamOpcode.LOADL, TamRegister.CB, 0, 1); // n=1 (size, A GUESS)

        // Push pointer value
        visit(new NameExpr(s.startToken(), s.pointerName())); // Pushes heap_addr

        // Call dispose
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
        visitForAddress(e); // Pushes &s%field
        int size = layoutManager.getSize(typeMap.get(e));
        add(TamOpcode.LOADI, TamRegister.CB, size, 0);
        return null;
    }


// In TamGenerator.java, REPLACE the old visitCallOrIndexExpr

    @Override
    public Void visitCallOrIndexExpr(CallOrIndexExpr e) {
        Type baseAstType = typeMap.get(e.base());

        if (baseAstType instanceof Type.FunctionType) {
            // --- Function Call ---
            for (Expression arg : e.arguments()) {
                visitForParameter(arg); // <-- THE FIX
            }
            String funcName = ((NameExpr) e.base()).name();
            addCall(routineLabels.get(funcName));
            // Result is left on the stack.
        } else {
            // --- Array/Pointer Index Read ---
            visitForAddress(e); // Pushes &arr(i) or &p(i)
            int size = layoutManager.getSize(typeMap.get(e));
            add(TamOpcode.LOADI, TamRegister.CB, size, 0);
        }
        return null;
    }

    @Override
    public Void visitNameExpr(NameExpr e) {
        Symbol sym = scopeStack.peek().getSymbol(e.name()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        Type type = sym.type();
        int size = addr.size();

        if (type instanceof Type.PointerType pt) {
            if (pt.baseType() instanceof Type.ArrayType) {
                // Array pointer 'p_arr'. Push its value (the heap address).
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // size=1
            } else {
                // Simple pointer 'p'. Push the value it points to (implicit deref).
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr
                add(TamOpcode.LOADI, TamRegister.CB, 1, 0); // Pushes value at heap_addr
            }
        } else if (type instanceof Type.ArrayType || type instanceof Type.DerivedType) {
            // Static array 'arr' or struct 's'. Push its *value* (all n words).
            add(TamOpcode.LOAD, TamRegister.LB, size, addr.offset());
        } else {
            // Simple variable 'x'
            add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // size=1
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
     */
    private void visitForAddress(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

            if (sym.type() instanceof Type.PointerType pt && !(pt.baseType() instanceof Type.ArrayType)) {
                // LValue is 'p'. We want the *heap address* it stores (implicit deref).
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
            } else {
                // LValue is 'x' or 'arr' or 'p_arr'. We want its stack address.
                add(TamOpcode.LOADA, TamRegister.LB, 0, addr.offset());
            }

        } else if (e instanceof FieldAccessExpr fe) {
            // LValue is s%field
            visitForAddress(fe.base()); // Pushes address of 's'

            Type baseType = getBaseType(fe.base());
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
}