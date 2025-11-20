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
        ir.add(new IntermediateTam.Jump(TamOpcode.CALL, TamRegister.CB, 8, label));
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
            case FieldAccessExpr(Token startToken, Expression base, String fieldName) -> {
                visitForAddress(base);

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

                // Put address of array start to top of stack
                visitForAddress(cie.base()); // could be field access, or name expr

                Type baseType = getBaseType(cie.base());
                if (!(baseType instanceof Type.ArrayType arrType)) throw new CompilerException(e.startToken(), "This expression is not an assignable lvalue.");

                // Calculate offset
                // Stack: [ arr_base_addr ]
                if (arrType.dimensions().stream().allMatch(d -> d instanceof Dimension.Known)) {
                    generateStaticArrayOffset(cie, arrType);
                } else {
                    generatePointerArrayOffset(cie, arrType);
                }
                // Stack: [ arr_base_addr, final_index ]
                callPrimitive(Primitives.ADD);
                // Stack: [ final_element_addr ]
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
        int elementSize = layoutManager.getSize(arrType.baseType());

        if (dims.size() != args.size()) {
            throw new CompilerException(cie.startToken(), "Incorrect number of array dimensions. Expected " + dims.size() + " but got " + args.size());
        }

        // Push the first index
        visit(args.getFirst());
        // Stack: [ index_accumulator ]

        // Apply Horner's Method: acc = (acc * Dim_size) + next_index
        for (int i = 1; i < args.size(); i++) {
            Dimension.Known dim = (Dimension.Known) dims.get(i);

            add(TamOpcode.LOADL, null, 0, dim.size());
            // Stack: [ acc, dim_size ]

            callPrimitive(Primitives.MULT);
            // Stack: [ acc * dim_size ]

            visit(args.get(i));
            // Stack: [ partial_acc, next_index ]

            callPrimitive(Primitives.ADD);
            // Stack: [ new_acc ]
        }
        // Stack: [ index_accumulator ]

        // Multiply by the element size
        if (elementSize > 1) {
            add(TamOpcode.LOADL, null, 0, elementSize);
            callPrimitive(Primitives.MULT);
        }
        // Stack: [ final_array_index ]
    }

    /**
     * Generates code to calculate an offset for a POINTER array.
     * Assumes the base address of the array is already on top of the stack.
     * It leaves the final calculated offset (1 word) on top of the stack.
     */
    private void generatePointerArrayOffset(CallOrIndexExpr cie, Type.ArrayType arrType) {
        // Stack: [ arr_base_addr ]

        if (cie.arguments().isEmpty()) {
            throw new CompilerException(cie.startToken(), "Pointer array indexing requires specification of indexes.");
        }

        int elementSize = layoutManager.getSize(arrType.baseType());
        int numDimensions = arrType.dimensions().size();

        if (cie.arguments().size() != numDimensions) {
            throw new CompilerException(cie.startToken(), "Incorrect number of array dimensions. Expected " + numDimensions + " but got " + cie.arguments().size());
        }

        add(TamOpcode.LOADL, null, 0, 0); // initialize offset to 0
        // Stack: [ arr_base_addr, 0 ]
        for (int i = 0; i < numDimensions; i++) {
            // Stack: [ arr_base_addr, offset_accumulator ]
            visit(cie.arguments().get(i));
            // Stack: [ arr_base_addr, offset_accumulator, index_expr_i ]
            for (int j = i + 1; j < numDimensions; j++) {
                add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // duplicate arr_base_addr
                // Stack: [ arr_base_addr, offset_accumulator, index_expr_i, arr_base_addr ]
                add(TamOpcode.LOADL, null, 0, j); // load index j
                // Stack: [ arr_base_addr, offset_accumulator, index_expr_i, arr_base_addr, index_j ]
                callPrimitive(Primitives.ADD);
                // Stack: [ arr_base_addr, offset_accumulator, index_expr_i, addr_of_dim_j ]
                add(TamOpcode.LOADI, null, 1, 0); // load size of dimension j
                // Stack: [ arr_base_addr, offset_accumulator, index_expr_i, dim_j_size ]
                callPrimitive(Primitives.MULT);
                // Stack: [ arr_base_addr, offset_accumulator, dim_offset ]
            }
            // Stack: [ arr_base_addr, offset_accumulator, index_i_offset ]
            callPrimitive(Primitives.ADD);
            // Stack: [ arr_base_addr, offset_accumulator ]
        }

        // Stack: [ arr_base_addr, total_index ]
        add(TamOpcode.LOADL, null, 0, elementSize);
        // Stack: [ arr_base_addr, total_index, element_size ]
        callPrimitive(Primitives.MULT);
        // Stack: [ arr_base_addr, total_offset ]
        add(TamOpcode.LOADL, null, 0, numDimensions);
        // Stack: [ arr_base_addr, total_offset, num_dimensions ]
        callPrimitive(Primitives.ADD);
        // Stack: [ arr_base_addr, final_index ]
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
        // Similar to function call:
        for (Expression arg : s.arguments()) {
            if (arg instanceof NameExpr ne) {
                // handle the name expression case manually because in this case we don't want to dereference the pointer.
                Symbol sym = scopeStack.peek().getSymbol(ne.name()).orElseThrow();
                MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

                add(TamOpcode.LOAD, TamRegister.LB, addr.size(), addr.offset());
            } else {
                visit(arg);
            }
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
                handleReadString(lval);
            } else {
                throw new CompilerException(lval.startToken(), "READ only supports INTEGER, LOGICAL, or CHARACTER variables.");
            }
        }
        return null;
    }

    private void handleReadString(Expression lval) {

        add(TamOpcode.LOADL, null, 0, 1);            // allocate 1 word for empty string
        // [1]

        callPrimitive(Primitives.NEW);               // empty_ptr
        // [empty_ptr]

        add(TamOpcode.LOADL, null, 0, 0);            // prepare length 0
        // [empty_ptr, 0]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);  // dup empty_ptr
        // [empty_ptr, 0, empty_ptr]

        add(TamOpcode.STOREI, null, 1, 0);           // empty_ptr[0] = 0
        // [empty_ptr]

        visitForAddress(lval);                       // lval_addr
        // [empty_ptr, lval_addr]

        add(TamOpcode.STOREI, null, 1, 0);           // lval = empty_ptr
        // []

        String loopStart = newLabel("read_loop");
        String loopEnd = newLabel("read_end");

        placeLabel(loopStart);
        // []

        visit(lval);                                 // load accumulator pointer
        // [accum_ptr]

        add(TamOpcode.LOADL, null, 0, 2);            // size for new 1 char string
        // [accum_ptr, 2]

        callPrimitive(Primitives.NEW);               // new_ptr
        // [accum_ptr, new_ptr]

        add(TamOpcode.LOADL, null, 0, 1);            // set length = 1
        // [accum_ptr, new_ptr, 1]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);  // dup new_ptr
        // [accum_ptr, new_ptr, 1, new_ptr]

        add(TamOpcode.STOREI, null, 1, 0);           // new_ptr[0] = 1
        // [accum_ptr, new_ptr]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);  // dup new_ptr for char address
        // [accum_ptr, new_ptr, new_ptr]

        callPrimitive(Primitives.SUCC);              // address of char slot
        // [accum_ptr, new_ptr, char_addr]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);  // dup char_addr for GET
        // [accum_ptr, new_ptr, char_addr, char_addr]

        callPrimitive(Primitives.GET);               // read char into heap
        // [accum_ptr, new_ptr, char_addr]

        add(TamOpcode.LOADI, null, 1, 0);            // load char value
        // [accum_ptr, new_ptr, char_val]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);  // dup char_val for LF check
        // [accum_ptr, new_ptr, char_val, char_val]

        add(TamOpcode.LOADL, null, 0, 10);           // LF
        // [accum_ptr, new_ptr, char_val, char_val, 10]

        add(TamOpcode.LOADL, null, 0, 1);
        // [accum_ptr, new_ptr, char_val, char_val, 10, 1]

        callPrimitive(Primitives.EQ);                // is LF
        // [accum_ptr, new_ptr, char_val, is_LF]

        addJumpIf(1, loopEnd);                       // pop is_LF if jump
        // if jump: [accum_ptr, new_ptr, char_val]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);  // dup char_val for CR check
        // [accum_ptr, new_ptr, char_val, char_val]

        add(TamOpcode.LOADL, null, 0, 13);           // CR
        // [accum_ptr, new_ptr, char_val, char_val, 13]

        add(TamOpcode.LOADL, null, 0, 1);
        // [accum_ptr, new_ptr, char_val, char_val, 13, 1]

        callPrimitive(Primitives.EQ);                // is CR
        // [accum_ptr, new_ptr, char_val, is_CR]

        addJumpIf(1, loopEnd);                       // pop is_CR if jump
        // if jump: [accum_ptr, new_ptr, char_val]

        add(TamOpcode.POP, null, 0, 1);              // discard char_val
        // [accum_ptr, new_ptr]

        generateStringConcat();                      // concat strings
        // [res_ptr]

        visitForAddress(lval);                       // prepare store back to lval
        // [res_ptr, lval_addr]

        add(TamOpcode.STOREI, null, 1, 0);           // lval = res_ptr
        // []

        addJump(loopStart);

        placeLabel(loopEnd);
        // [accum_ptr, new_ptr, char_val]

        add(TamOpcode.POP, null, 0, 3);              // clean up
        // []
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
            } else if (baseType instanceof Type.ArrayType || baseType instanceof Type.DerivedType) {
                // Print the address of an array or derived type
                visitForAddress(e);
                callPrimitive(Primitives.PUTINT);
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

        int baseSize;
        if (pt.baseType() instanceof Type.ArrayType(Type baseType, List<Dimension> dimensions) && dimensions.stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
            baseSize = layoutManager.getSize(baseType);
        } else {
            baseSize = layoutManager.getSize(pt.baseType());
        }
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
        callPrimitive(Primitives.SUCC);
        // stack: [ tot_size ]

        // If there are unknown dimensions, we need to add space for storing their sizes
        if (pt.baseType() instanceof Type.ArrayType(Type baseType, List<Dimension> dimensions) && dimensions.stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
            int dimCount = s.dimensions().size();
            add(TamOpcode.LOADL, null, 0, dimCount);
            // stack: [ tot_size, num_dimensions ]
            callPrimitive(Primitives.ADD);
            // stack: [ tot_size + num_dimensions ]
        }
        // stack: [ tot_size ]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // stack: [ tot_size, tot_size ]

        callPrimitive(Primitives.NEW);
        // stack: [ tot_size, allocated_addr ]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // stack: [ tot_size, allocated_addr, tot_size ]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // stack: [ tot_size, allocated_addr, tot_size, allocated_addr ]
        add(TamOpcode.POP, null, 3, 1);
        // stack: [ allocated_addr, tot_size, allocated_addr ]
        add(TamOpcode.STOREI, null, 1, 0); // store total allocated size at allocated_addr
        // stack: [ allocated_addr ]
        callPrimitive(Primitives.SUCC);
        // stack: [ allocated_addr + 1 ]

        if (pt.baseType() instanceof Type.ArrayType(Type baseType, List<Dimension> dimensions) && dimensions.stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
            // for dynamic arrays, the dimension sizes are stored from the start of the array in the order of allocation.
            for (Expression dimExpr : s.dimensions()) {
                visit(dimExpr);
            }
            // stack: [ arr_start, dim_size0, dim_size1, ... ]
            // Now store each dimension size in the allocated space
            int dimCount = s.dimensions().size();
            add(TamOpcode.LOAD, TamRegister.ST, 1, -dimCount - 1); // duplicate arr_start
            // stack: [ arr_start, dim_size0, dim_size1, ..., arr_start ]
            add(TamOpcode.STOREI, null, dimCount, 0); // store all dimension sizes at the start of the dynamic array
            // stack: [ arr_start ]
        }

        MemoryLayoutManager.TamAddress ptrAddr = layoutManager.getAddress(ptrSymbol);
        add(TamOpcode.LOADA, TamRegister.LB, 0, ptrAddr.offset());
        // stack: [ arr_start, ptr_addr ]
        add(TamOpcode.STOREI, null, 1, 0);
        // stack: []
        return null;
    }

    @Override
    public Void visitDeallocateStmt(DeallocateStmt s) {
        Symbol sym = scopeStack.peek().getSymbol(s.pointerName()).orElseThrow();
        MemoryLayoutManager.TamAddress addr = layoutManager.getAddress(sym);

        add(TamOpcode.LOAD, TamRegister.LB, 1, addr.offset());
        // stack: [ allocated_adr + 1 ] (because we are storing size of allocated memory below the addr)
        callPrimitive(Primitives.PRED);
        // stack: [ allocated_adr ]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // stack: [ allocated_adr, allocated_adr ]
        add(TamOpcode.LOADI, null, 1, 0);
        // stack: [ allocated_adr, size ]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // stack: [ allocated_adr, size, allocated_adr ]
        add(TamOpcode.POP, null, 2, 1);
        // stack: [ size, allocated_adr ]


        callPrimitive(Primitives.DISPOSE);
        return null;
    }

// Expressions (push value to stack)

    @Override
    public Void visitBinaryOpExpr(BinaryOpExpr e) {
        visit(e.left());
        visit(e.right());

        Type leftType = getBaseType(e.left());
        if (leftType == Type.Intrinsic.CHARACTER && isRelationalOp(e.operator())) {

            generateStringComparison();
            // Stack now has: [comparison_result]

            add(TamOpcode.LOADL, null, 0, 0);
            // Stack: [result, 0]

            switch (e.operator()) {
                case LESS_THAN -> callPrimitive(Primitives.LT);       // -1 < 0 -> true
                case LESS_EQUAL -> callPrimitive(Primitives.LE);      // -1 <= 0 -> true, 0 <= 0 -> true
                case GREATER_THAN -> callPrimitive(Primitives.GT);    // 1 > 0 -> true
                case GREATER_EQUAL -> callPrimitive(Primitives.GE);   // 1 >= 0 -> true, 0 >= 0 -> true
                case EQUAL -> {
                    add(TamOpcode.LOADL, null, 0, 1);
                    callPrimitive(Primitives.EQ); // 0 == 0 -> true
                }
                case NOT_EQUAL -> {
                    add(TamOpcode.LOADL, null, 0, 1);
                    callPrimitive(Primitives.NE); // -1 != 0 -> true
                }
                default -> throw new IllegalStateException("Unexpected string operator");
            }
            return null;
        }

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

    private boolean isRelationalOp(BinaryOpExpr.BinaryOp op) {
        return switch (op) {
            case LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL, EQUAL, NOT_EQUAL -> true;
            default -> false;
        };
    }

    /**
     * Helper to compare two strings.
     * Stack input: [s1_addr, s2_addr]
     * Stack output: [result] where result is -1 (s1 < s2), 0 (s1 == s2), or 1 (s1 > s2).
     */
    private void generateStringComparison() {
        String loopStart = newLabel("str_cmp_loop");
        String exitLoopEndReached = newLabel("str_cmp_len");
        String exitLoopCharNotMatching = newLabel("str_cmp_minus");
        String end = newLabel("str_cmp_end");

        // Initial Stack: [s1, s2], these are the addresses of the strings in heap memory, first word is length (s1[0])

        // Load lengths
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Dup s1
        add(TamOpcode.LOADI, null, 1, 0);        // Load len1
        // Stack: [s1, s2, len1]

        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Dup s2
        add(TamOpcode.LOADI, null, 1, 0);        // Load len2
        // Stack: [s1, s2, len1, len2]

        // Compute min_len = min(len1, len2) for loop bound
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Dup len1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Dup len2
        // Stack: [s1, s2, len1, len2, len1, len2]

        callPrimitive(Primitives.SUB);               // len1 - len2
        // Stack: [s1, s2, len1, len2, (len1 - len2)]

        // Compute min_len = min(len1, len2) for loop bound
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // Dup len1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // Dup len2
        callPrimitive(Primitives.LE);               // len1 <= len2
        // Stack: [s1, s2, len1, len2, (len1 - len2), (len1 <= len2)]

        String useLen1 = newLabel("use_len2");
        String minReady = newLabel("min_ready");

        addJumpIf(1, useLen1);                      // if len1 <= len2, use len1
        // Stack: [s1, s2, len1, len2, (len1 - len2)]

        // Use len2 as min
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2); // Push len2
        // Stack: [s1, s2, len1, len2, (len1 - len2), len2]
        addJump(minReady);

        placeLabel(useLen1);
        // Stack: [s1, s2, len1, len2, (len1 - len2)]
        // Use len1 as min
        add(TamOpcode.LOAD, TamRegister.ST, 1, -3); // Push len1
        // Stack: [s1, s2, len1, len2, (len1 - len2), len1]

        placeLabel(minReady);
        // Stack: [s1, s2, len1, len2, (len1 - len2), cmp_len]


        add(TamOpcode.LOAD, TamRegister.ST, 1, -6);
        // Stack: [s1, s2, len1, len2, (len1 - len2), cmp_len, s1]
        callPrimitive(Primitives.SUCC); // Skip the length word.
        // Stack: [s1, s2, len1, len2, (len1 - len2), cmp_len, s1 + 1]
        callPrimitive(Primitives.ADD);
        // Stack: [s1, s2, len1, len2, (len1 - len2), cmp_len + s1 + 1]
        // Stack: [s1, s2, len1, len2, (len1 - len2), <max_addr_s1>]
        add(TamOpcode.POP, null, 2, 2);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>]

        // Increment s1 pointer to point to first character
        addToStackTopOffsetAndStore(-4, 1);
        // Stack: [s1 + 1, s2, (len1 - len2), <max_addr_s1>]

        // Increment s2 pointer to point to first character
        addToStackTopOffsetAndStore(-3, 1);
        // Stack: [s1 + 1, s2 + 1, (len1 - len2), <max_addr_s1>]

        // Now s1 and s2 point to first characters of the strings
        // --- At this point will refer to s1 and s2 as the current character addresses ---

        placeLabel(loopStart);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>]

        // Check if (loop termination)
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, <max_addr_s1>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -5);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, <max_addr_s1>, s1]
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, <max_addr_s1>, s1, 1]

        callPrimitive(Primitives.EQ); // Exit loop if reached end of comparison length
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, max_addr_s1 == s1]
        addJumpIf(1, exitLoopEndReached);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>]

        // If the end of one of the strings is not reached, compare characters
        // Load char1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -4);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, s1]
        add(TamOpcode.LOADI, null, 1, 0);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1]

        // Load char2
        add(TamOpcode.LOAD, TamRegister.ST, 1, -4);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1, s2]
        add(TamOpcode.LOADI, null, 1, 0);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1, char2]

        callPrimitive(Primitives.SUB); // char1 - char2
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -1); // Dup char1 - char2
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2, char1 - char2]
        add(TamOpcode.LOADL, null, 0, 0);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2, char1 - char2, 0]
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2, char1 - char2, 0, 1]
        callPrimitive(Primitives.NE); // Check if the 2 characters don't match
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2, char1 != char2]
        addJumpIf(1, exitLoopCharNotMatching); // if not equal, exit loop
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2]

        // If we are here, characters matched, continue loop by incrementing pointers
        add(TamOpcode.POP, null, 0, 1); // Pop char1 - char2
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>]

        // Increment s1 pointer to point to next character
        addToStackTopOffsetAndStore(-4, 1);
        // Stack: [s1 + 1, s2, (len1 - len2), <max_addr_s1>]

        // Increment s2 pointer to point to next character
        addToStackTopOffsetAndStore(-3, 1);
        // Stack: [s1 + 1, s2 + 1, (len1 - len2), <max_addr_s1>]

        addJump(loopStart);

        placeLabel(exitLoopCharNotMatching);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, char1 - char2]

        // Handle exiting due to character mismatch
        add(TamOpcode.POP, null, 1, 4);
        // Stack: [char1 - char2]
        addJump(end);

        placeLabel(exitLoopEndReached); // Exit loop: lengths exhausted
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [s1, s2, (len1 - len2), <max_addr_s1>, (len1 - len2)]
        add(TamOpcode.POP, null, 1, 4);
        // Stack: [(len1 - len2)]

        placeLabel(end);
        // Stack: [char1 - char2] or [(len1 - len2)]
        // RETURN HERE, leaving string comparison result because strings matched and reached end of at least one of the strings
    }

    private void addToStackTopOffsetAndStore(int offset, int amount) {
        // Stack: [...]

        add(TamOpcode.LOAD, TamRegister.ST, 1, offset);
        // Stack: [..., value_to_increment]
        add(TamOpcode.LOADL, null, 0, amount);
        // Stack: [..., value_to_increment, amount_to_add]
        callPrimitive(Primitives.ADD);
        // Stack: [..., value_to_increment + amount_to_add]
        add(TamOpcode.LOADA, TamRegister.ST, 0, offset - 1);
        // Stack: [..., value_to_increment + amount_to_add, stack_value_addr]
        add(TamOpcode.STOREI, null, 1, 0); // stores back the new value
        // Stack: [...]
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
        add(TamOpcode.POP, null, 1, 1);
        // Stack: [base, exponent, new_acc]

        // Loop Body: exponent = exponent - 1
        add(TamOpcode.LOAD, TamRegister.ST, 1, -2);
        // Stack: [base, exponent, new_acc, exponent]

        callPrimitive(Primitives.PRED);
        // Stack: [base, exponent, new_acc, new_exp]
        add(TamOpcode.STORE, TamRegister.ST, 1, -2);
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
        add(TamOpcode.LOAD, TamRegister.ST, 1, -7);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr<src>]
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr, 1<dest_off>]
        add(TamOpcode.LOADL, null, 0, 1);
        // Stack: [s1_ptr, s2_ptr, len1, len2, new_len, new_ptr, new_ptr, s1_ptr, 1, 1<src_off>]
        add(TamOpcode.LOAD, TamRegister.ST, 1, -8); // DUP len1
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