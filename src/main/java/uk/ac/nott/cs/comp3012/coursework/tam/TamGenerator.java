package uk.ac.nott.cs.comp3012.coursework.tam;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.CompilerException;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.Symbol;
import uk.ac.nott.cs.comp3012.coursework.ast.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.types.Dimension;
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
     * TAM Primitive routine offsets as defined in the TAM specification.
     * each primitive performs an elementary operation on the stack.
     * <p>
     * a denotes a data address
     * c denotes a character
     * i denotes an integer
     * n denotes a non-negative integer
     * t denotes a truth value (0 for false or 1 for true)
     * v denotes a value of any type
     * w denotes any 1-word value
     */
    private enum Primitives {
        ID(1),      // Identity: w -> w' (Set w' = w)
        NOT(2),     // Logical NOT: t -> t' (Set t' = ¬t)
        AND(3),     // Logical AND: t1, t2 -> t' (Set t' = t1 ∧ t2)
        OR(4),      // Logical OR: t1, t2 -> t' (Set t' = t1 ∨ t2)
        SUCC(5),    // Successor: i -> i' (Set i' = i + 1)
        PRED(6),    // Predecessor: i -> i' (Set i' = i - 1)
        NEG(7),     // Negation: i -> i' (Set i' = -i)
        ADD(8),     // Addition: i1, i2 -> i' (Set i' = i1 + i2)
        SUB(9),     // Subtraction: i1, i2 -> i' (Set i' = i1 - i2)
        MULT(10),   // Multiplication: i1, i2 -> i' (Set i' = i1 × i2)
        DIV(11),    // Division: i1, i2 -> i' (Set i' = i1 / i2, truncated)
        MOD(12),    // Modulo: i1, i2 -> i' (Set i' = i1 mod i2)
        LT(13),     // Less than: i1, i2 -> t' (Set t' = true iff i1 < i2)
        LE(14),     // Less than or equal: i1, i2 -> t' (Set t' = true iff i1 ≤ i2)
        GE(15),     // Greater than or equal: i1, i2 -> t' (Set t' = true iff i1 ≥ i2)
        GT(16),     // Greater than: i1, i2 -> t' (Set t' = true iff i1 > i2)
        EQ(17),     // Equal: v1, v2, n -> t' (Set t' = true iff v1 = v2, where v1 and v2 are n-word values)
        NE(18),     // Not equal: v1, v2, n -> t' (Set t' = true iff v1 ≠ v2, where v1 and v2 are n-word values)
        EOL(19),    // End of line: -> t' (Set t' = true iff next character is end-of-line)
        EOF(20),    // End of file: -> t' (Set t' = true iff no more characters to read)
        GET(21),    // Get character: a -> (Read character and store at address a)
        PUT(22),    // Put character: c -> (Write character c)
        GETEOL(23), // Get end of line: -> (Read characters up to and including next end-of-line)
        PUTEOL(24), // Put end of line: -> (Write end-of-line)
        GETINT(25), // Get integer: a -> (Read integer literal and store at address a)
        PUTINT(26), // Put integer: i -> (Write integer literal with value i)
        NEW(27),    // Allocate heap: n -> a' (Set a' = address of newly allocated n-word object in heap)
        DISPOSE(28);// Deallocate heap: n, a -> (Deallocate n-word object at address a in heap)

        final int offset;

        Primitives(int offset) {
            this.offset = offset;
        }
    }

    public TamGenerator(Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap, MemoryLayoutManager layoutManager) {
        this.scopeMap = scopeMap;
        this.typeMap = typeMap;
        this.layoutManager = layoutManager;
    }

    public List<IntermediateTam> getIntermediateCode() {
        return ir;
    }

    // Helper Methods

    private void add(TamOpcode op, TamRegister r, int n, int d) {
        ir.add(new IntermediateTam.Instr(op, r, n, d));
    }

    private void callPrimitive(Primitives prim) {
        add(TamOpcode.CALL, TamRegister.PB, TamRegister.LB.value, prim.offset);
    }

    private void addJump(String label) {
        ir.add(new IntermediateTam.Jump(TamOpcode.JUMP, TamRegister.CB, 0, label));
    }

    /**
     * Pop a 1-word value from the stack, then jump to code with the label if and only if that popped value equals n
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

    /**
     * Emits code to push the stack address of an l-value onto the stack.
     * This method ignores implicit dereferencing for pointers.
     * It is used for ALLOCATE and READ(char).
     * <p>
     * Throws an errors if the expression is not an assignable value (address is not at a constant offset)
     */
    private void generateLValueStackAddress(Expression e) {
        switch (e) {
            case NameExpr ne -> {
                Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
                MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
                add(TamOpcode.LOADA, TamRegister.LB, 0, addr.offset());
            }
            case FieldAccessExpr(org.antlr.v4.runtime.Token startToken, Expression base, String fieldName) -> {
                generateLValueStackAddress(base);

                Type baseType = getBaseType(base);
                if (!(baseType instanceof Type.DerivedType dt)) {
                    throw new CompilerException(startToken, "Base of field access is not a derived type.");
                }
                int fieldOffset = layoutManager.getFieldOffset(dt, fieldName);

                add(TamOpcode.LOADL, null, 0, fieldOffset);
                callPrimitive(Primitives.ADD);
            }
            case CallOrIndexExpr cie -> {
                // This is an array index
                generateLValueStackAddress(cie.base());

                // Calculate offset
                Type baseType = getBaseType(cie.base());
                if (baseType instanceof Type.ArrayType arrType && arrType.dimensions().stream().allMatch(d -> d instanceof Dimension.Known)) {
                    generateStaticArrayOffset(cie, arrType);
                } else {
                    assert baseType instanceof Type.ArrayType;
                    generatePointerArrayOffset(cie, (Type.ArrayType) baseType);
                }

                callPrimitive(Primitives.ADD);
            }
            default -> throw new CompilerException(e.startToken(), "This expression is not an assignable lvalue.");
        }
    }

    /**
     * Pushes the address of the expression onto the stack. For pointers, it pushes their actual value.
     * Throws an errors if the expression is not an assignable value (address is not at a constant offset)
     *
     * @param e the expression whose address will be pushed onto the stack
     * @throws CompilerException if the expression is not an assignable value.
     */
    private void visitForAddress(Expression e) {
        if (e instanceof NameExpr ne) {
            Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
            MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

            if (sym.type() instanceof Type.PointerType) {
                // Implicitly dereference.
                add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
                return;
            }
        }
        generateLValueStackAddress(e);
    }

    private void generateStaticArrayOffset(CallOrIndexExpr cie, Type.ArrayType arrType) {

        List<Dimension> dims = arrType.dimensions();
        List<Expression> args = cie.arguments();

        if (dims.size() != args.size()) {
            throw new CompilerException(cie.startToken(), "Incorrect number of array dimensions. Expected " + dims.size() + " but got " + args.size());
        }

        visit(args.getFirst());

        for (int i = 1; i < args.size(); i++) {
            Dimension.Known dim = (Dimension.Known) dims.get(i);
            add(TamOpcode.LOADL, null, 0, dim.size());
            callPrimitive(Primitives.MULT);
            visit(args.get(i));
            callPrimitive(Primitives.ADD);
        }
    }

    /**
     * Generates code to calculate an offset for a POINTER array.
     * It leaves the final calculated offset (1 word) on top of the stack.
     * Formula: offset = i * sizeof(baseType)
     */
    private void generatePointerArrayOffset(CallOrIndexExpr cie, Type.ArrayType arrType) {
        if (cie.arguments().size() != 1) {
            throw new CompilerException(cie.startToken(), "Pointer array indexing only supports one dimension.");
        }

        int elementSize = layoutManager.getSize(arrType.baseType());

        visit(cie.arguments().getFirst());

        if (elementSize > 1) {
            add(TamOpcode.LOADL, null, 0, elementSize);
            callPrimitive(Primitives.MULT);
        }
    }

    // AST nodes below

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

        add(TamOpcode.PUSH, null, 0, 3); // Add padding for first scope to account for missing static link, dynamic link, and return address.

        visit(cu.program());

        add(TamOpcode.HALT, null, 0, 0);

        for (ProgramUnit unit : cu.programUnits()) {
            if (!(unit instanceof DerivedTypeDecl)) {
                visit(unit);
            }
        }

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
            visit(arg);
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

        Symbol loopVarSymbol = scopeStack.peek().getSymbol(s.loopVariable()).orElseThrow();
        int loopVarOffset = layoutManager.getAddress(loopVarSymbol).offset();

        // Set initial value
        visit(s.start());
        add(TamOpcode.LOADA, TamRegister.LB, 0, loopVarOffset);
        add(TamOpcode.STOREI, null, 1, 0);

        placeLabel(startLabel);

        // Check condition
        add(TamOpcode.LOAD, TamRegister.LB, 1, loopVarOffset);
        visit(s.end());
        callPrimitive(Primitives.GE);
        addJumpIf(1, endLabel); // Jump if 1 (true)

        visitStatementList(s.body());

        add(TamOpcode.LOAD, TamRegister.LB, 1, loopVarOffset);
        visit(s.step());
        callPrimitive(Primitives.ADD);
        add(TamOpcode.LOADA, TamRegister.LB, 0, loopVarOffset);
        add(TamOpcode.STOREI, null, 1, 0); // Store variable back

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

            if (baseType == Type.Intrinsic.INTEGER || baseType == Type.Intrinsic.LOGICAL) {
                // Get address to store at (handles implicit deref)
                visitForAddress(lval);
                callPrimitive(Primitives.GETINT);
            } else if (baseType == Type.Intrinsic.CHARACTER) {
                add(TamOpcode.LOADL, null, 0, 2); // [ 2 ]
                callPrimitive(Primitives.NEW); // [ addr ]

                // store newly allocated memory address in the character seq. variable
                visitForAddress(lval); // [ addr, str_var_addr ]
                add(TamOpcode.STOREI, null, 1, 0); // [  ]

                // save string length as 1
                visitForAddress(lval); // [str_var_addr]
                add(TamOpcode.LOADI, null, 1, 0); // [ alloc_adr]
                add(TamOpcode.LOADL, null, 0, 1); // [ alloc_adr, 1 ]
                add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // [ alloc_adr, 1, alloc_adr ]
                // store the string length at the string address
                add(TamOpcode.STOREI, null, 1, 0); // [alloc_adr]

                // Make pointer at the stack top be the address of the string + 1 to store the character
                callPrimitive(Primitives.SUCC); // [alloc_adr + 1]

                // read the single char input
                callPrimitive(Primitives.GET);
            } else {
                throw new CompilerException(lval.startToken(), "READ only supports INTEGER, LOGICAL, or CHARACTER variables.");
            }
        }
        return null;
    }

    @Override
    public Void visitWriteStmt(WriteStmt s) {
        for (Expression e : s.expressions()) {
            Type baseType = getBaseType(e);
            if (baseType == Type.Intrinsic.INTEGER || baseType == Type.Intrinsic.LOGICAL) {
                visit(e);
                callPrimitive(Primitives.PUTINT);
            } else if (baseType == Type.Intrinsic.CHARACTER) {
                generateWriteString(e);
            } else if (baseType instanceof Type.ArrayType) {
                throw new CompilerException(e.startToken(), "Cannot write expression of type Array, write a function for that!");
            } else if (baseType instanceof Type.DerivedType) {
                throw new CompilerException(e.startToken(), "Cannot write expression of a derived type, write a function for that!");
            } else {
                throw new CompilerException(e.startToken(), "Cannot write expression of type '" + baseType + "'");
            }
            callPrimitive(Primitives.PUTEOL);
        }
        return null;
    }

    private void generateWriteString(Expression e) {
        if (getBaseType(e) != Type.Intrinsic.CHARACTER) throw new CompilerException(e.startToken(), "Cannot generate code to write a string for the expression '" + getBaseType(e) + "'");

        String loopStart = newLabel("write_str_start");
        String loopEnd = newLabel("write_str_end");

        visit(e); // Stack: [heap_addr]

        // Get length
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // [heap_addr, heap_addr]
        add(TamOpcode.LOADI, null, 1, 0); // [heap_addr, len]

        // Push counter i = 1
        add(TamOpcode.LOADL, null, 0, 1); // [heap_addr, len, i]

        placeLabel(loopStart);
        // Check condition: i > len
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // [heap_addr, len, i, i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // [heap_addr, len, i, i, len]
        callPrimitive(Primitives.GT); // [heap_addr, len, i, result]
        addJumpIf(1, loopEnd); // [heap_addr, len, i]

        // print char at heap_addr[i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // [heap_addr, len, i, heap_addr]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // [heap_addr, len, i, heap_addr, i]
        callPrimitive(Primitives.ADD); //  [heap_addr, len, i, &char]
        add(TamOpcode.LOADI, null, 1, 0); //  [heap_addr, len, i, char_val]
        callPrimitive(Primitives.PUT); // [heap_addr, len, i]

        // Increment: i = i + 1
        callPrimitive(Primitives.SUCC); //  [heap_addr, len, new_i]

        addJump(loopStart);

        placeLabel(loopEnd);
        // Clean up stack
        // Stack is [heap_addr, len, i]
        add(TamOpcode.POP, null, 0, 3); // Pops `i`, `len`, `heap_addr`.
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
                callPrimitive(Primitives.MULT);
            }
        }
        // stack: [ size_to_allocate ]
        callPrimitive(Primitives.NEW); // stack: [ allocated_addr ]

        MemoryLayoutManager.TamAddress ptrAddr = layoutManager.getAddress(ptrSymbol);
        add(TamOpcode.LOADA, TamRegister.LB, 0, ptrAddr.offset()); // stack: [ allocated_addr, ptr_addr ]

        add(TamOpcode.STOREI, null, 1, 0); // stack: []
        return null;
    }

    @Override
    public Void visitDeallocateStmt(DeallocateStmt s) {
        add(TamOpcode.LOADL, null, 0, 1);

        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);
        add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());

        callPrimitive(Primitives.DISPOSE);
        return null;
    }

    // Expressions (push value to stack)

    @Override
    public Void visitBinaryOpExpr(BinaryOpExpr e) {
        // Handle POWER and CONCAT, which are not simple primitives

        visit(e.left());
        visit(e.right());

        switch (e.operator()) {
            case POWER -> {
                generatePower(); // Assumes [base, exponent] on stack
                return null;
            }
            case CONCAT -> {
                generateStringConcat(); // Assumes [s1_ptr, s2_ptr] on stack
                return null;
            }
            default -> {
                // Handle all other simple primitives
            }
        }

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
            // Handled above
            case CONCAT, POWER -> throw new IllegalStateException("Should be handled above");
        };

        if (prim == Primitives.EQ || prim == Primitives.NE) {
            add(TamOpcode.LOADL, null, 0, size);
            callPrimitive(prim);
        } else {
            callPrimitive(prim);
        }
        return null;
    }

    /**
     * Helper for POWER (**).
     * <p>
     * Assumes [base, exponent] is on the stack.
     * Leaves [result] on the stack.
     */
    private void generatePower() {
        String loopStart = newLabel("pow_loop_start");
        String loopEnd = newLabel("pow_loop_end");

        // Initial Stack: [base, exponent]

        // Push accumulator
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [base, exponent, 1<acc>]

        placeLabel(loopStart);
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [base, exponent, acc, exponent]
        add(TamOpcode.LOADL, null, 0, 0);
        // Stack: [base, exponent, acc, exponent, 0]
        callPrimitive(Primitives.GT);
        // Stack: [base, exponent, acc, (exp > 0)]
        addJumpIf(0, loopEnd); // pops
        // Stack: [base, exponent, acc]

        // Loop Body: acc = acc * base
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [base, exponent, acc, acc]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -4);
        // Stack: [base, exponent, acc, acc, base]
        callPrimitive(Primitives.MULT);
        // Stack: [base, exponent, acc, new_acc]
        add(TamOpcode.STORE, TamRegister.ST, 1, -2);
        // Stack: [base, exponent, new_acc]

        // Loop Body: exponent = exponent - 1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [base, exponent, new_acc, exponent]

        callPrimitive(Primitives.PRED);
        // Stack: [base, exponent, new_acc, new_exp]
        add(TamOpcode.STORE, TamRegister.ST, 1, -3);
        // Stack: [base, new_exp, new_acc]

        addJump(loopStart);

        placeLabel(loopEnd);
        // Stack: [base, exponent, acc] (This is the state when loop test fails)

        // clean up stack to leave only the result
        add(TamOpcode.POP, null, 1, 2);
        // Final Stack: [acc<result>]
    }

    /**
     * Helper for CONCAT (//).
     * Refactored to use a string copy helper method.
     * Assumes [s1_ptr, s2_ptr] is on the stack.
     * Leaves [new_ptr] on the stack.
     */
    private void generateStringConcat() {
        // Initial Stack: [s1_ptr, s2_ptr]

        // get len1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [s1_ptr, s2_ptr, s1_ptr]
        add(TamOpcode.LOADI, null, 1, 0);
        // Stack: [s1_ptr, s2_ptr, len1]

        // get len2
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [s1_ptr, s2_ptr, len1, s2_ptr]
        add(TamOpcode.LOADI, null, 1, 0);
        // Stack: [s1_ptr, s2_ptr, len1, len2]

        // new_len = len1 + len2
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, len2]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3);
        // Stack: [s1_ptr, s2_ptr, len1, len2, len2, len1]
        callPrimitive(Primitives.ADD);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len]

        // total_size = new_len + 1 (for length word)
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_len]

        callPrimitive(Primitives.SUCC);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, total_size]

        // new_ptr = allocate(total_size)
        callPrimitive(Primitives.NEW);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr]

        // new_ptr[0] = new_len
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_len]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_len, new_ptr]
        add(TamOpcode.STOREI, null, 1, 0); // stores new_len at the new_ptr
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr]

        // prep for Loop 1 (copy s1)
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -7); // DUP s1_ptr
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr<src>]
        add(TamOpcode.LOADL, null, 0, 1); // dest_offset = 1
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr, 1<dest_off>]
        add(TamOpcode.LOADL, null, 0, 1); // src_offset = 1
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr, 1, 1<src_off>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -9); // DUP len1
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr, 1, 1, len1<count>]

        generateStringCopyLoop("concat1");
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr]

        // prep for Loop 2 (copy s2)
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -6);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>, s2_ptr<src>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -6);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>, s2_ptr<src>, len1]

        callPrimitive(Primitives.SUCC); // dest_offset = len1 + 1
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>, s2_ptr<src>, <dest_off>]
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>, s2_ptr<src>, <dest_off>, 1<src_off>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -7);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr<dest>, s2_ptr<src>, <dest_off>, 1<src_off>, len2<count>]

        // call helper. Stack args consumed.
        generateStringCopyLoop("concat2");
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr]

        // clean up stack to leave only [new_ptr]
        add(TamOpcode.POP, null, 1, 5);
        // Final Stack: [new_ptr]
    }

    /**
     * Helper method to copy a string.
     * Assumes stack is: [dest_ptr, src_ptr, dest_offset, src_offset, count]
     * Consumes these 5 arguments.
     * Copies "count" chars from "src_ptr[src_offset + i]" to "dest_ptr[dest_offset + i]".
     *
     * @param hint A string hint for label generation.
     */
    private void generateStringCopyLoop(String hint) {
        String loopStart = newLabel(hint + "_start");
        String loopEnd = newLabel(hint + "_end");

        // Stack on entry: [dest, src, d_off, s_off, count]

        add(TamOpcode.LOADL, null, 0, 0);
        // Stack: [dest, src, d_off, s_off, count, i=0]

        placeLabel(loopStart);
        // Stack: [dest, src, d_off, s_off, count, i]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [dest, src, d_off, s_off, count, i, i]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -3);
        // Stack: [dest, src, d_off, s_off, count, i, i, count]

        callPrimitive(Primitives.GE);
        // Stack: [dest, src, d_off, s_off, count, i, (i>=count)]

        addJumpIf(1, loopEnd);
        // Stack: [dest, src, d_off, s_off, count, i]

        // body: char = src[s_off + i]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -5);
        // Stack: [dest, src, d_off, s_off, count, i, src]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -4);
        // Stack: [dest, src, d_off, s_off, count, i, src, s_off]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -3);
        // Stack: [dest, src, d_off, s_off, count, i, src, s_off, i]

        callPrimitive(Primitives.ADD);
        // Stack: [dest, src, d_off, s_off, count, i, src, (s_off+i)]

        callPrimitive(Primitives.ADD);
        // Stack: [dest, src, d_off, s_off, count, i, (src + s_off + i)]

        add(TamOpcode.LOADI, null, 1, 0); // get the char from source string
        // Stack: [dest, src, d_off, s_off, count, i, char]

        // part: dest[d_off + i] = char
        add(TamOpcode.LOAD, TamRegister.ST, 1, -7);
        // Stack: [dest, src, d_off, s_off, count, i, char, dest]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -6);
        // Stack: [dest, src, d_off, s_off, count, i, char, dest, d_off]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -4);
        // Stack: [dest, src, d_off, s_off, count, i, char, dest, d_off, i]

        callPrimitive(Primitives.ADD);
        // Stack: [dest, src, d_off, s_off, count, i, char, dest, (d_off+i)]

        callPrimitive(Primitives.ADD);
        // Stack: [dest, src, d_off, s_off, count, i, char, (dest + d_off + i)]

        add(TamOpcode.STOREI, null, 1, 0); // store the char at that address
        // Stack: [dest, src, d_off, s_off, count, i]

        // increment: i = i + 1
        callPrimitive(Primitives.SUCC);
        // Stack: [dest, src, d_off, s_off, count, i + 1]

        addJump(loopStart);
        // Loop back with Stack: [dest, src, d_off, s_off, count, i] where i is now incremented

        placeLabel(loopEnd);
        // Stack when we reach here: [dest, src, d_off, s_off, count, i]

        // 6. Clean up stack
        add(TamOpcode.POP, null, 0, 6); // Pop 0-word result, pop 6 words, push nothing back
        // Final Stack: []
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
                if (arg instanceof NameExpr ne) {
                    // handle the name expression case manually because in this case we don't want to dereference the pointer.
                    Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
                    MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

                    add(TamOpcode.LOAD, TamRegister.LB, addr.size(), addr.offset());
                } else {
                    visit(arg);
                }
            }
            String funcName = ((NameExpr) e.base()).name();
            addCall(routineLabels.get(funcName));
        } else {
            // for Multi-dimensional Arrays
            visitForAddress(e); // This now calculates the full address
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

        if (type instanceof Type.PointerType(Type baseType)) {
            // implicitly dereference.
            add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr
            add(TamOpcode.LOADI, null, layoutManager.getSize(baseType), 0); // Pushes value at heap_addr of the specified size
        } else if (type == Type.Intrinsic.CHARACTER) {
            // Strings are stored as heap pointers, just load the address
            add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset()); // Pushes heap_addr (string pointer)
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
        // Stack: [totalSize]

        callPrimitive(Primitives.NEW);
        // Stack: [heap_addr]

        add(TamOpcode.LOADL, null, 0, len);
        // Stack: [heap_addr, len]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [heap_addr, len, heap_addr]

        add(TamOpcode.STOREI, null, 1, 0);
        // Stack: [heap_addr]

        // Store each character
        for (int i = 0; i < len; i++) {
            int charVal = s.charAt(i);
            int charIndex = i + 1;

            add(TamOpcode.LOADL, null, 0, charVal);
            // Stack: [heap_addr, charVal]

            add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
            // Stack: [heap_addr, charVal, heap_addr]

            add(TamOpcode.LOADL, null, 0, charIndex);
            // Stack: [heap_addr, charVal, heap_addr, charIndex]

            callPrimitive(Primitives.ADD);
            // Stack: [heap_addr, charVal, (heap_addr+charIndex)]

            add(TamOpcode.STOREI, null, 1, 0); // Stores the charVal at the right address
            // Stack: [heap_addr]
        }

        // Final Stack: [heap_addr]
        return null;
    }
}