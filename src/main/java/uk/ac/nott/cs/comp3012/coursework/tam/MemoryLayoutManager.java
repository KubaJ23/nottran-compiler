package uk.ac.nott.cs.comp3012.coursework.tam;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.FunctionDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.ProgramDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.SubroutineDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.Symbol;
import uk.ac.nott.cs.comp3012.coursework.ast.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.types.Dimension;
import uk.ac.nott.cs.comp3012.coursework.types.Type;

import java.util.*;

/**
 * Calculates and stores the static memory layout for a Nottscript program.
 * <p>
 * All offsets are relative to LB (Local Base Register).
 * <p>
 * Parameters have negative offsets.
 * <p>
 * Local variables have positive offsets starting at 3
 */
public class MemoryLayoutManager {

    /**
     * Represents the address of a variable in a TAM stack frame.
     */
    public record TamAddress(int offset, int size) {}

    /**
     * Stores the final computed address for every declared variable.
     * Symbols are used instead of variable names for keys. This allows 2 variables with the same name in different scopes to be mapped correctly.
     */
    private final Map<Symbol, TamAddress> variableAddressMap = new HashMap<>();

    /**
     * Caches the layout of derived types. Maps derived type to a map of its fields and their offsets relative to the variable's start
     */
    private final Map<Type.DerivedType, Map<String, Integer>> typeLayoutCache = new HashMap<>();

    /**
     * Caches the total size (in words) of all local variables (not parameters) for a given scope.
     * Used by TamGenerator to PUSH space.
     */
    private final Map<SymbolTable, Integer> localDataSizeMap = new HashMap<>();

    /**
     * Caches the total size (in words) of all parameters for a given scope.
     * Used by TamGenerator for RETURN.
     */
    private final Map<SymbolTable, Integer> parameterDataSizeMap = new HashMap<>();

    public MemoryLayoutManager(Ast.CompilationUnit ast, Map<Ast, SymbolTable> scopeMap) {
        SymbolTable globalScope = scopeMap.get(ast);
        if (globalScope == null) {
            throw new IllegalStateException("Could not find global scope for CompilationUnit.");
        }

        // Pre-compute the field layouts for derived types
        preComputeTypeLayouts(globalScope);

        // Map the variables for every scope.
        for (Map.Entry<Ast, SymbolTable> entry : scopeMap.entrySet()) {
            if (entry.getKey() instanceof ProgramDecl || entry.getKey() instanceof FunctionDecl || entry.getKey() instanceof SubroutineDecl) {
                processScope(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Finds all type definitions in the global scope and computes/caches their layout.
     */
    private void preComputeTypeLayouts(SymbolTable globalScope) {
        for (Symbol symbol : globalScope.getSymbols().values()) {
            if (symbol.kind() == Symbol.Kind.TYPE_DEFINITION) {
                // Calling getSize will trigger computeLayout if not already cached.
                getSize(symbol.type());
            }
        }
    }

    /**
     * Calculates and stores the offsets for all parameters and local variables within a single scope.
     */
    private void processScope(Ast astNode, SymbolTable scope) {
        List<String> paramNames = getParamNames(astNode);

        int totalParamSize = 0;
        Map<String, Symbol> paramSymbols = new HashMap<>();
        for (String paramName : paramNames) {
            Symbol paramSymbol = scope.getSymbolLocal(paramName).orElseThrow(() -> new IllegalStateException("Parameter " + paramName + " not in scope."));
            totalParamSize += getSize(paramSymbol.type());
            paramSymbols.put(paramName, paramSymbol);
        }
        parameterDataSizeMap.put(scope, totalParamSize);

        int currentParamOffset = -totalParamSize;
        for (String paramName : paramNames) {
            Symbol paramSymbol = paramSymbols.get(paramName);
            int size = getSize(paramSymbol.type());
            variableAddressMap.put(paramSymbol, new TamAddress(currentParamOffset, size));
            currentParamOffset += size;
        }

        int currentLocalOffset = 3;
        int totalLocalSize = 0;

        for (Symbol symbol : scope.getSymbols().values()) {
            if (symbol.kind() == Symbol.Kind.VARIABLE && !paramSymbols.containsKey(symbol.name())) {
                int size = getSize(symbol.type());
                variableAddressMap.put(symbol, new TamAddress(currentLocalOffset, size));
                currentLocalOffset += size;
                totalLocalSize += size;
            }
        }
        localDataSizeMap.put(scope, totalLocalSize);
    }

    private List<String> getParamNames(Ast astNode) {
        if (astNode instanceof FunctionDecl fd) {
            return fd.parameterNames();
        } else if (astNode instanceof SubroutineDecl sd) {
            return sd.parameterNames();
        }
        // ProgramDecl has no parameters
        return Collections.emptyList();
    }

    public TamAddress getAddress(Symbol symbol) {
        TamAddress addr = variableAddressMap.get(symbol);
        if (addr == null) {
            throw new IllegalStateException("No address computed for symbol: " + symbol.name());
        }
        return addr;
    }

    public int getLocalDataSize(SymbolTable scope) {
        return localDataSizeMap.getOrDefault(scope, 0);
    }

    public int getParameterDataSize(SymbolTable scope) {
        return parameterDataSizeMap.getOrDefault(scope, 0);
    }

    public int getFieldOffset(Type.DerivedType type, String fieldName) {
        Map<String, Integer> offsetMap = typeLayoutCache.get(type);
        Integer offset = offsetMap.get(fieldName);
        if (offset == null) {
            throw new IllegalStateException("Type '" + type.typeName() + "' has no field '" + fieldName + "'");
        }
        return offset;
    }

    public int getSize(Type type) {
        return switch (type) {
            case Type.Intrinsic i -> 1;
            case Type.PointerType p -> 1;
            case Type.FunctionType f -> 1;
            case Type.SubroutineType s -> 1;

            case Type.ArrayType a -> {
                // An array with unknown dimensions is treated as a simple pointer, size 1.
                if (a.dimensions().stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
                    yield 1;
                }
                // A static array's size is the product of its dimensions * base type size.
                int arraySize = a.dimensions().stream().map(d -> (Dimension.Known) d).mapToInt(Dimension.Known::size).reduce(1, (x, y) -> x * y);
                yield arraySize * getSize(a.baseType());
            }
            case Type.DerivedType d -> {
                if (!typeLayoutCache.containsKey(d)) {
                    computeLayout(d);
                }
                // The size is the sum of the sizes of all its fields.
                yield d.fields().values().stream().mapToInt(this::getSize).sum();
            }
        };
    }

    private void computeLayout(Type.DerivedType d) {
        // Use LinkedHashMap to preserve the declared field order.
        Map<String, Integer> offsetMap = new LinkedHashMap<>();
        int currentOffset = 0;
        for (Map.Entry<String, Type> fieldEntry : d.fields().entrySet()) {
            String fieldName = fieldEntry.getKey();
            Type fieldType = fieldEntry.getValue();

            offsetMap.put(fieldName, currentOffset);
            currentOffset += getSize(fieldType);
        }
        typeLayoutCache.put(d, offsetMap);
    }
}