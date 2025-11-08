package uk.ac.nott.cs.comp3012.coursework.util.tam;

import uk.ac.nott.cs.comp3012.coursework.CompilerException;
import uk.ac.nott.cs.comp3012.coursework.Symbol;
import uk.ac.nott.cs.comp3012.coursework.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacInstr;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacOp;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacParam;
import uk.ac.nott.cs.comp3012.coursework.util.types.Dimension;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates a list of TAC instructions into TAM bytecode.
 * (Two-pass translator)
 */
public class TamTranslator {

    private final List<TacInstr> tac;
    private final AddressManager addressManager;
    private final Map<Ast, SymbolTable> scopeMap;

    // Map to get the type of a TacParam.Variable (used for loadAddress)
    private final Map<TacParam.Variable, Type> paramTypeMap = new HashMap<>();

    private final Map<String, Integer> labelMap = new HashMap<>();
    private final List<TamInstruction.Instruction> tamCode = new ArrayList<>();

    // State for Pass 2
    private SymbolTable currentScope;
    private TamRegister currentRegister; // SB for main/globals, LB for functions
    private int currentTempTop; // Next available stack offset for a temporary
    private final Map<String, AddressManager.TamAddress> tempAddressMap = new HashMap<>();
    private final int globalDataSize; // Cached size of all global variables

    public TamTranslator(List<TacInstr> tac, AddressManager addressManager, Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap) {
        this.tac = tac;
        this.addressManager = addressManager;
        this.scopeMap = scopeMap;

        // Find the "main" program scope to start
        this.currentScope = scopeMap.values().stream()
                .filter(s -> s.getDepth() == 0)
                .findFirst().orElseThrow(() -> new IllegalStateException("Could not find global scope."));

        // Build a type map for all variables AND temporaries
        buildParamTypeMap(typeMap);

        // Cache global data size
        int globalSize = 0;
        for (Symbol s : this.currentScope.getSymbols().values()) {
            if (s.kind() == Symbol.Kind.VARIABLE) globalSize += addressManager.getSize(s.type());
        }
        this.globalDataSize = globalSize;
    }

    /**
     * Pre-populates a map from TacParam.Variable -> Type.
     * This is crucial for 'loadAddress' to know if a var is a pointer.
     */
    private void buildParamTypeMap(Map<Ast, Type> typeMap) {
        // 1. Add all declared variables from the AST type map
        for (Map.Entry<Ast, Type> entry : typeMap.entrySet()) {
            if (entry.getKey() instanceof Ast.Expression.NameExpr name) {
                paramTypeMap.put(new TacParam.Variable(name.name()), entry.getValue());
            }
        }

        // 2. Infer types for temporaries from the TAC
        SymbolTable activeScope = this.currentScope;
        for (TacInstr instr : tac) {
            // Update scope if we see a function/program label
            if(instr.op() == TacOp.LABEL && !instr.dst().toString().startsWith("_L_")) {
                activeScope = findScope(instr.dst().toString());
            }

            if (instr.dst() instanceof TacParam.Variable dstVar) {
                if (paramTypeMap.containsKey(dstVar)) continue; // Already typed

                Type inferredType = Type.Intrinsic.INTEGER; // Default
                switch (instr.op()) {
                    case ALLOCATE:
                        // Dst is a pointer. Find its declaration.
                        Symbol s = activeScope.getSymbol(dstVar.value()).orElse(null);
                        if(s != null) {
                            inferredType = s.type(); // This is a declared pointer, e.g. 'integer pointer'
                        } else {
                            // This is a temporary pointer, e.g. from a string literal
                            // or string concat. We'll type it as CHARACTER (our base heap type).
                            inferredType = Type.Intrinsic.CHARACTER;
                        }
                        break;
                    case LOAD_CONST:
                        inferredType = Type.Intrinsic.INTEGER;
                        break;
                    case FIELD_READ:
                        // Infer from field type
                        Type baseType = getParamType(new TacParam.Variable(instr.src1().toString()));
                        if (baseType instanceof Type.PointerType pt) baseType = pt.baseType();
                        if (baseType instanceof Type.ArrayType at) baseType = at.baseType();

                        if (baseType instanceof Type.DerivedType dt) {
                            String fieldName = ((TacParam.ConstString)instr.src2()).value();
                            inferredType = dt.fields().get(fieldName);
                        }
                        break;
                    case INDEX_READ:
                        // Infer from base type
                        Type base = getParamType(new TacParam.Variable(instr.src1().toString()));
                        if (base instanceof Type.PointerType pt) {
                            base = pt.baseType();
                        }

                        if (base instanceof Type.ArrayType at) {
                            inferredType = at.baseType();
                        } else {
                            inferredType = base; // e.g., simple pointer 'integer pointer'
                        }
                        break;
                    case CALL: // Function return
                        Symbol funcSym = this.currentScope.getSymbol( ((TacParam.Label)instr.src1()).value() ).orElse(null);
                        if(funcSym != null && funcSym.type() instanceof Type.FunctionType ft) {
                            inferredType = ft.returnType();
                        }
                        break;
                    //
                    // *** REMOVED INVALID "CASE CONCAT:" ***
                    //
                }
                paramTypeMap.put(dstVar, inferredType);
            }
        }
    }


    public List<TamInstruction.Instruction> translate() {
        passOneBuildLabelMap();
        passTwoGenerateTamCode();
        return tamCode;
    }

    // --- Pass 1: Build Label Map ---

    private void passOneBuildLabelMap() {
        int currentAddress = 0;
        for (TacInstr instr : tac) {
            if (instr.op() == TacOp.LABEL) {
                labelMap.put(instr.dst().toString(), currentAddress);
            } else {
                currentAddress += estimateTamSize(instr);
            }
        }
    }

    /**
     * Estimates the *exact* number of TAM instructions a TAC op will generate.
     */
    private int estimateTamSize(TacInstr instr) {
        return switch (instr.op()) {
            case HALT, GOTO, PARAM -> 1;
            case RETURN -> (instr.src1() == null) ? 1 : 2; // 1 (no val) or 2 (LOAD + RETURN)
            case LOAD_CONST, ASSIGN, GOTO_IF_FALSE, GOTO_IF_TRUE, DEALLOCATE, READ, WRITE, WRITE_CHAR -> 2;
            case NEG -> 3; // LOADL 0, LOAD, CALL sub
            case NOT -> 2; // LOAD, CALL not
            case ADD, SUB, MUL, DIV, LSS, LEQ, GRT, GEQ, EQL, NEQ, AND, OR -> 3; // L, L, C
            case CALL -> (instr.dst() == null) ? 2 : 3; // PUSH + CALL (+ STORE)
            case ALLOCATE -> 3; // LOAD size, CALL new, STORE ptr
            case INDEX_READ, FIELD_READ -> 5; // loadAddr + loadOffset + CALL add + LOADI + STORE
            case INDEX_WRITE, FIELD_WRITE -> 5; // loadAddr + loadOffset + CALL add + loadVal + STOREI
            case LABEL -> 0; // Labels take no space
        };
    }


    // --- Pass 2: Generate TAM Code ---

    private void passTwoGenerateTamCode() {
        tamCode.clear();
        tempAddressMap.clear();

        // Start in the global scope
        currentScope = scopeMap.values().stream()
                .filter(s -> s.getDepth() == 0)
                .findFirst().orElseThrow();
        currentRegister = TamRegister.SB; // Globals are on SB

        SymbolTable mainScope = scopeMap.entrySet().stream()
                .filter(e -> e.getKey() instanceof ProgramDecl)
                .map(Map.Entry::getValue).findFirst().orElseThrow();

        // Temporaries start after globals + main's declared locals
        currentTempTop = globalDataSize + addressManager.getLocalDataSize(mainScope);


        for (TacInstr instr : tac) {
            // Most ops generate a value that then needs to be stored
            boolean storeResult = switch (instr.op()) {
                case ASSIGN, LOAD_CONST, ADD, SUB, MUL, DIV, LSS, LEQ, GRT, GEQ, EQL, NEQ, AND, OR, NEG, NOT, ALLOCATE, INDEX_READ, FIELD_READ -> true;
                default -> false; // CALL stores its own result
            };

            switch (instr.op()) {
                case LABEL:
                    String labelName = instr.dst().toString();
                    if (!labelName.startsWith("_L_")) {
                        // This is a function/sub/program label. Switch scope.
                        tempAddressMap.clear();
                        currentScope = findScope(labelName);

                        if (currentScope.getDepth() == 0) {
                            // Main program scope
                            currentRegister = TamRegister.SB;
                            currentTempTop = globalDataSize + addressManager.getLocalDataSize(currentScope);
                        } else {
                            // Function/Subroutine scope
                            currentRegister = TamRegister.LB;
                            // Temps start after link data (3 words) + declared locals
                            currentTempTop = 3 + addressManager.getLocalDataSize(currentScope);
                        }
                    }
                    break;
                case HALT:
                    add(TamOpcode.HALT, 0, TamRegister.CB, 0);
                    break;
                case LOAD_CONST:
                    int val = ((TacParam.Value) instr.src1()).value();
                    add(TamOpcode.LOADL, val, TamRegister.CB, 0);
                    break;
                case ASSIGN:
                    load(instr.src1());
                    break;
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case LSS:
                case LEQ:
                case GRT:
                case GEQ:
                case EQL:
                case NEQ:
                case AND:
                case OR:
                    load(instr.src1());
                    load(instr.src2());
                    add(TamOpcode.CALL, 0, TamRegister.PB, getPrimitiveAddr(instr.op()));
                    break;
                case NEG:
                    add(TamOpcode.LOADL, 0, TamRegister.CB, 0);
                    load(instr.src1());
                    add(TamOpcode.CALL, 0, TamRegister.PB, getPrimitiveAddr(TacOp.SUB));
                    break;
                case NOT:
                    load(instr.src1());
                    add(TamOpcode.CALL, 0, TamRegister.PB, getPrimitiveAddr(TacOp.NOT));
                    break;
                case GOTO:
                    int gotoAddr = labelMap.get(instr.dst().toString());
                    add(TamOpcode.JUMP, 0, TamRegister.CB, gotoAddr);
                    break;
                case GOTO_IF_FALSE:
                    load(instr.src1()); // Load the boolean
                    int jumpAddrFalse = labelMap.get(instr.dst().toString());
                    add(TamOpcode.JUMPIF, 0, TamRegister.CB, jumpAddrFalse); // Jump if 0 (false)
                    break;
                case GOTO_IF_TRUE:
                    load(instr.src1()); // Load the boolean
                    int jumpAddrTrue = labelMap.get(instr.dst().toString());
                    add(TamOpcode.JUMPIF, 1, TamRegister.CB, jumpAddrTrue); // Jump if 1 (true)
                    break;
                case PARAM:
                    load(instr.src1());
                    break;
                case CALL:
                    SymbolTable funcScope = findScope(instr.src1().toString());
                    int localDataSize = addressManager.getLocalDataSize(funcScope);
                    // PUSH space for callee's *local variables*
                    add(TamOpcode.PUSH, localDataSize, TamRegister.CB, 0);

                    int callAddr = labelMap.get(instr.src1().toString());
                    // Static link (n) is SB (reg 4), as this language has no nested scopes.
                    add(TamOpcode.CALL, 4, TamRegister.CB, callAddr);

                    if (instr.dst() != null) {
                        store(instr.dst()); // Store the return value
                    }
                    break;
                case RETURN:
                    int paramSize = addressManager.getParameterDataSize(currentScope);
                    int resultSize = 0;
                    if (instr.src1() != null) {
                        load(instr.src1()); // Load the return value
                        resultSize = 1; // All results are 1 word
                    }
                    add(TamOpcode.RETURN, resultSize, TamRegister.CB, paramSize);
                    break;
                case ALLOCATE: // dst_ptr = allocate(src1_size)
                    load(instr.src1()); // Push size
                    add(TamOpcode.CALL, 0, TamRegister.PB, 27);  // new
                    break;
                case DEALLOCATE: // deallocate src1_ptr
                    load(instr.src1()); // Push pointer
                    add(TamOpcode.CALL, 0, TamRegister.PB, 28);  // dispose
                    break;
                case READ: // read src1_dst_address
                    loadAddress(instr.src1()); // Push address
                    add(TamOpcode.CALL, 0, TamRegister.PB, 25);  // getint
                    break;
                case WRITE: // write src1_val
                    load(instr.src1());
                    add(TamOpcode.CALL, 0, TamRegister.PB, 26);  // putint
                    break;
                case WRITE_CHAR: // write_char src1_val
                    load(instr.src1());
                    add(TamOpcode.CALL, 0, TamRegister.PB, 22);  // put
                    break;
                case FIELD_READ: // dst = src1_base.%src2_field
                    loadAddress(instr.src1()); // Push base address (e.g., &s)
                    int offset = getFieldOffset(instr.src1(), instr.src2().toString());
                    add(TamOpcode.LOADL, offset, TamRegister.CB, 0); // Push byte offset
                    add(TamOpcode.CALL, 0, TamRegister.PB, 8);  // add
                    add(TamOpcode.LOADI, 1, TamRegister.CB, 0);  // Load 1 word from computed address
                    break;
                case FIELD_WRITE: // dst_base.%src1_field = src2_val
                    loadAddress(instr.dst()); // Push base address (e.g., &s)
                    int offsetWrite = getFieldOffset(instr.dst(), instr.src1().toString());
                    add(TamOpcode.LOADL, offsetWrite, TamRegister.CB, 0); // Push byte offset
                    add(TamOpcode.CALL, 0, TamRegister.PB, 8);  // add
                    load(instr.src2()); // Push value to write
                    add(TamOpcode.STOREI, 1, TamRegister.CB, 0);  // Store 1 word at computed address
                    break;
                case INDEX_READ: // dst = src1_base[src2_index_bytes]
                    loadAddress(instr.src1()); // Push base address (e.g., 'p' or '&arr')
                    load(instr.src2()); // Push *byte offset*
                    add(TamOpcode.CALL, 0, TamRegister.PB, 8);  // add
                    add(TamOpcode.LOADI, 1, TamRegister.CB, 0);  // Load 1 word from computed address
                    break;
                case INDEX_WRITE: // dst_base[src1_index_bytes] = src2_val
                    loadAddress(instr.dst()); // Push base address (e.g., 'p' or '&arr')
                    load(instr.src1()); // Push *byte offset*
                    add(TamOpcode.CALL, 0, TamRegister.PB, 8);  // add
                    load(instr.src2()); // Push value to write
                    add(TamOpcode.STOREI, 1, TamRegister.CB, 0);  // Store 1 word at computed address
                    break;
                default:
                    throw new CompilerException(null, "TAM translation not implemented for: " + instr.op());
            }

            if (storeResult) {
                store(instr.dst());
            }
        }
    }

    // --- Helper Methods ---

    private void add(TamOpcode op, int n, TamRegister r, int d) {
        tamCode.add(new TamInstruction.Instruction(op, r, n, d));
    }

    private SymbolTable findScope(String name) {
        for (Map.Entry<Ast, SymbolTable> entry : scopeMap.entrySet()) {
            String nodeName = switch(entry.getKey()) {
                case ProgramDecl pd -> pd.name();
                case FunctionDecl fd -> fd.name();
                case SubroutineDecl sd -> sd.name();
                default -> null;
            };
            if (name.equals(nodeName)) {
                return entry.getValue();
            }
        }
        // Fallback for labels that might not be in the map
        return this.currentScope;
    }

    private Type getParamType(TacParam param) {
        if (param instanceof TacParam.Variable var) {
            Type t = paramTypeMap.get(var);
            if (t != null) return t;
        }
        // Default for literals or un-inferred temps
        return Type.Intrinsic.INTEGER;
    }

    /**
     * Gets the address for a variable, allocating a new temporary slot if needed.
     */
    private AddressManager.TamAddress getAddress(TacParam param) {
        if (!(param instanceof TacParam.Variable var)) {
            throw new IllegalArgumentException("Cannot get address of non-variable: " + param);
        }
        String name = var.value();

        // 1. Check if it's a declared variable
        Symbol symbol = currentScope.getSymbol(name).orElse(null);
        if (symbol != null && symbol.kind() == Symbol.Kind.VARIABLE) {
            return addressManager.getAddress(symbol);
        }

        // 2. Check if it's a temporary we've already seen
        if (tempAddressMap.containsKey(name)) {
            return tempAddressMap.get(name);
        }

        // 3. It's a new temporary. Allocate a 1-word slot for it.
        int offset = currentTempTop;
        currentTempTop += 1; // Increment for next temp
        AddressManager.TamAddress newAddr = new AddressManager.TamAddress(currentRegister, offset, 1);
        tempAddressMap.put(name, newAddr);
        return newAddr;
    }

    /**
     * Generates TAM code to load a value *onto the stack top*.
     */
    private void load(TacParam param) {
        if (param instanceof TacParam.Value val) {
            add(TamOpcode.LOADL, val.value(), TamRegister.CB, 0);
        } else if (param instanceof TacParam.Variable var) {
            AddressManager.TamAddress addr = getAddress(var);
            add(TamOpcode.LOAD, addr.size(), addr.register(), addr.offset());
        } else {
            throw new IllegalArgumentException("Cannot load: " + param);
        }
    }

    /**
     * Generates TAM code to load an *address* onto the stack top.
     */
    private void loadAddress(TacParam param) {
        if (!(param instanceof TacParam.Variable var)) {
            throw new IllegalArgumentException("Cannot load address of: " + param);
        }

        Type type = getParamType(var);
        AddressManager.TamAddress addr = getAddress(var);

        if (type instanceof Type.PointerType || type == Type.Intrinsic.CHARACTER || (type instanceof Type.ArrayType arr && arr.dimensions().stream().anyMatch(d -> d instanceof Dimension.Unknown))) {
            // It's a pointer, a string (which is a pointer), or a dynamic array parameter.
            // Its *value* (the address it holds) is what we want.
            add(TamOpcode.LOAD, 1, addr.register(), addr.offset());
        } else {
            // It's a static array or struct. We *compute* its address.
            add(TamOpcode.LOADA, 0, addr.register(), addr.offset());
        }
    }

    /**
     * Generates TAM code to pop a value from the stack top and store it.
     */
    private void store(TacParam param) {
        if (param instanceof TacParam.Variable var) {
            AddressManager.TamAddress addr = getAddress(var);
            add(TamOpcode.STORE, addr.size(), addr.register(), addr.offset());
        }
    }

    private int getFieldOffset(TacParam baseParam, String fieldNameWithQuotes) {
        Type baseType = getParamType(baseParam);

        // Unwrap pointers/arrays to find the base DerivedType
        if (baseType instanceof Type.PointerType pt) {
            baseType = pt.baseType();
            if(baseType instanceof Type.ArrayType at) {
                baseType = at.baseType();
            }
        }

        if (baseType instanceof Type.DerivedType dt) {
            // fieldNameWithQuotes is like "\"score\""
            String fieldName = fieldNameWithQuotes.substring(1, fieldNameWithQuotes.length() - 1);
            return addressManager.getFieldOffset(dt, fieldName);
        }
        throw new IllegalStateException("Cannot get field offset for non-derived type: " + baseType);
    }

    /**
     * Maps a TAC arithmetic/logic op to its TAM primitive address.
     */
    private int getPrimitiveAddr(TacOp op) {
        return switch (op) {
            case ADD -> 8;  // add
            case SUB -> 9;  // sub
            case MUL -> 10; // mult
            case DIV -> 11; // div
            case LSS -> 13; // lt
            case LEQ -> 14; // le
            case GEQ -> 15; // ge
            case GRT -> 16; // gt
            case EQL -> 17; // eq
            case NEQ -> 18; // ne
            case AND -> 3;  // and
            case OR -> 4;   // or
            case NOT -> 2;  // not
            case NEG -> 7;  // neg
            default -> throw new IllegalStateException("Not a primitive: " + op);
        };
    }
}