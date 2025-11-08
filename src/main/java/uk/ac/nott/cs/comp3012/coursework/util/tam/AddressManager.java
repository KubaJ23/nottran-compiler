package uk.ac.nott.cs.comp3012.coursework.util.tam;

import uk.ac.nott.cs.comp3012.coursework.Symbol;
import uk.ac.nott.cs.comp3012.coursework.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.FunctionDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.ProgramDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.SubroutineDecl;
import uk.ac.nott.cs.comp3012.coursework.util.types.Dimension;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Symbols (from SymbolTable) to concrete TAM addresses (Register + offset)
 * and tracks memory requirements for each scope.
 */
public class AddressManager {

    /**
     * @param register TAM register (e.g., SB, LB)
     * @param offset   Offset from the register (can be negative for parameters)
     * @param size     Size of the data in words (e.g., an array of 10 integers has size 10)
     */
    public record TamAddress(TamRegister register, int offset, int size) {}

    private final Map<Symbol, TamAddress> addressMap = new HashMap<>();
    private final Map<SymbolTable, Integer> localDataSizes = new HashMap<>();
    private final Map<SymbolTable, Integer> paramDataSizes = new HashMap<>();
    private final Map<Type.DerivedType, Map<String, Integer>> fieldOffsetMaps = new HashMap<>();
    private final Map<Ast, SymbolTable> scopeMap;
    private SymbolTable globalScope;

    /**
     * Constructs an AddressManager using the maps from the frontend.
     */
    public AddressManager(Map<Ast, SymbolTable> scopeMap) {
        this.scopeMap = scopeMap;
        buildAddressMap();
    }

    /**
     * Get the address assigned to a given symbol.
     */
    public TamAddress getAddress(Symbol symbol) {
        TamAddress addr = addressMap.get(symbol);
        if (addr == null) {
            // This can happen if the symbol is e.g. a function name
            throw new IllegalStateException("No address found for symbol: " + symbol.name());
        }
        return addr;
    }

    /**
     * Get the total size (in words) of all local variables (not parameters)
     * for a given function/subroutine scope.
     */
    public int getLocalDataSize(SymbolTable scope) {
        return localDataSizes.getOrDefault(scope, 0);
    }

    /**
     * Get the total size (in words) of all parameters for a given
     * function/subroutine scope.
     */
    public int getParameterDataSize(SymbolTable scope) {
        return paramDataSizes.getOrDefault(scope, 0);
    }

    /**
     * Gets the byte offset for a field within a derived type.
     */
    public int getFieldOffset(Type.DerivedType type, String fieldName) {
        Map<String, Integer> offsetMap = fieldOffsetMaps.get(type);
        if (offsetMap == null) {
            // This type might not have been computed yet if only used as pointer
            computeLayout(type);
            offsetMap = fieldOffsetMaps.get(type);
        }
        Integer offset = offsetMap.get(fieldName);
        if (offset == null) {
            throw new IllegalStateException("Type '" + type.typeName() + "' has no field '" + fieldName + "'");
        }
        return offset;
    }

    /**
     * Recursively calculate the size of a type in TAM words.
     */
    public int getSize(Type type) {
        return switch (type) {
            case Type.Intrinsic i -> 1;
            case Type.PointerType p -> 1; // A pointer is 1 word
            case Type.ArrayType a -> {
                // If dimensions are unknown (*), it's a pointer (e.g., a parameter)
                if (a.dimensions().stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
                    yield 1; // Passed by reference (pointer)
                }
                // Product of all dimensions * base type size
                int arraySize = a.dimensions().stream()
                        .map(d -> (Dimension.Known) d)
                        .mapToInt(Dimension.Known::size)
                        .reduce(1, (x, y) -> x * y);
                yield arraySize * getSize(a.baseType());
            }
            case Type.DerivedType d -> {
                // Lazily compute/cache derived type layouts
                if (!fieldOffsetMaps.containsKey(d)) {
                    computeLayout(d);
                }
                // Return the sum of all field sizes
                yield d.fields().values().stream().mapToInt(this::getSize).sum();
            }
            case Type.FunctionType f -> 1; // Function reference (not used)
            case Type.SubroutineType s -> 1; // Subroutine reference (not used)
        };
    }

    /**
     * Computes and caches the offset of each field for a derived type.
     */
    private void computeLayout(Type.DerivedType d) {
        // Use LinkedHashMap to preserve field order
        Map<String, Integer> offsetMap = new LinkedHashMap<>();
        int currentOffset = 0;
        for (Map.Entry<String, Type> fieldEntry : d.fields().entrySet()) {
            offsetMap.put(fieldEntry.getKey(), currentOffset);
            currentOffset += getSize(fieldEntry.getValue());
        }
        fieldOffsetMaps.put(d, offsetMap);
    }

    private void buildAddressMap() {
        // Find the global scope
        this.globalScope = scopeMap.values().stream()
                .filter(s -> s.getDepth() == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find global scope."));

        // 1. Assign globals (from global scope) relative to SB
        int globalOffset = 0;
        for (Symbol symbol : globalScope.getSymbols().values()) {
            if (symbol.kind() == Symbol.Kind.VARIABLE) {
                int size = getSize(symbol.type());
                addressMap.put(symbol, new TamAddress(TamRegister.SB, globalOffset, size));
                globalOffset += size;
            } else if (symbol.kind() == Symbol.Kind.TYPE_DEFINITION) {
                // Ensure layout is computed for all global types
                getSize(symbol.type());
            }
        }

        // 2. Assign locals for main program (also relative to SB)
        // And assign locals/params for functions/subroutines (relative to LB)
        for (Map.Entry<Ast, SymbolTable> entry : scopeMap.entrySet()) {
            Ast node = entry.getKey();
            SymbolTable localScope = entry.getValue();

            if (node instanceof ProgramDecl pd) {
                // Main program's locals are allocated with globals
                int mainLocalOffset = globalOffset;
                int mainLocalSize = 0;
                for (Symbol symbol : localScope.getSymbols().values()) {
                    if (symbol.kind() == Symbol.Kind.VARIABLE) {
                        int size = getSize(symbol.type());
                        addressMap.put(symbol, new TamAddress(TamRegister.SB, mainLocalOffset, size));
                        mainLocalOffset += size;
                        mainLocalSize += size;
                    }
                }
                localDataSizes.put(localScope, mainLocalSize);
                paramDataSizes.put(localScope, 0); // Main has no params

            } else if (node instanceof FunctionDecl fd) {
                assignLocalFrame(localScope, fd.parameterNames());
            } else if (node instanceof SubroutineDecl sd) {
                assignLocalFrame(localScope, sd.parameterNames());
            }
        }
    }

    /**
     * Assigns addresses for a local stack frame (function or subroutine).
     * - [cite_start]Parameters are at negative offsets from LB. [cite: 1]
     * - [cite_start]Link data (SL, DL, RA) is at 0, 1, 2 [LB]. [cite: 1]
     * - [cite_start]Local variables are at positive offsets (starting at 3[LB]). [cite: 1]
     */
    private void assignLocalFrame(SymbolTable localScope, java.util.List<String> parameterNames) {
        // --- 1. Assign Parameters (Negative Offsets) ---
        int totalParamSize = 0;
        for (String paramName : parameterNames) {
            Symbol paramSymbol = localScope.getSymbolLocal(paramName).get();
            totalParamSize += getSize(paramSymbol.type());
        }
        paramDataSizes.put(localScope, totalParamSize);

        int currentParamOffset = -totalParamSize;
        for (String paramName : parameterNames) {
            Symbol paramSymbol = localScope.getSymbolLocal(paramName).get();
            int size = getSize(paramSymbol.type());
            // Address is at the *start* of the parameter block
            addressMap.put(paramSymbol, new TamAddress(TamRegister.LB, currentParamOffset, size));
            currentParamOffset += size;
        }

        // --- 2. Assign Local Variables (Positive Offsets) ---
        int currentLocalOffset = 3; // 0=SL, 1=DL, 2=RA
        int localVariableSize = 0;
        for (Symbol symbol : localScope.getSymbols().values()) {
            // If it's a variable AND not in the parameter list, it's a local
            if (symbol.kind() == Symbol.Kind.VARIABLE && !parameterNames.contains(symbol.name())) {
                int size = getSize(symbol.type());
                addressMap.put(symbol, new TamAddress(TamRegister.LB, currentLocalOffset, size));
                currentLocalOffset += size;
                localVariableSize += size;
            }
        }

        // Store the total size of *local variables only*
        localDataSizes.put(localScope, localVariableSize);
    }
}
