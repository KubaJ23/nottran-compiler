package uk.ac.nott.cs.comp3012.coursework.util.tam;

import uk.ac.nott.cs.comp3012.coursework.Symbol;
import uk.ac.nott.cs.comp3012.coursework.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.SymbolTableBuilder;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Symbols (from SymbolTable) to concrete TAM addresses (Register + offset).
 */
public class AddressManager {

    public record TamAddress(TamRegister register, int offset) {}

    private final Map<Symbol, TamAddress> addressMap = new HashMap<>();
    private final SymbolTable globalScope;
    private final Map<Ast, SymbolTable> scopeMap;

    public AddressManager(SymbolTableBuilder stb) {
        this.globalScope = stb.getGlobalScope();
        this.scopeMap = stb.getScopeMap();
        buildAddressMap();
    }

    public TamAddress getAddress(Symbol symbol) {
        TamAddress addr = addressMap.get(symbol);
        if (addr == null) {
            throw new IllegalStateException("No address found for symbol: " + symbol.name());
        }
        return addr;
    }

    private void buildAddressMap() {
        // 1. Globals (relative to SB)
        int globalOffset = 0;
//        for (Symbol symbol : globalScope.getSymbols().values()) {
//            if (symbol.kind() == Symbol.Kind.VARIABLE) {
//                // TODO: This assumes all variables are 1 word.
//                // You must change this to account for array sizes.
//                addressMap.put(symbol, new TamAddress(TamRegister.SB, globalOffset));
//                globalOffset += 1; // Increment by symbol size
//            }
//        }

        // 2. Locals (relative to LB)
        for (SymbolTable localScope : scopeMap.values()) {
            // Note: This logic is simplified. A real implementation
            // needs to know which symbols are parameters (negative offset)
            // and which are locals (positive offset, after link data).

            // Per Fortran spec, parameters are just variables in the local scope.
            // We'll need to use the FunctionDecl/SubroutineDecl node to find them.

            int localOffset = 3; // 0=StaticLink, 1=DynamicLink, 2=ReturnAddress
            int paramOffset = -1; // Starts at -1 and goes down

            // This is complex. A simple-but-working version:
//            for (Symbol symbol : localScope.getSymbols().values()) {
//                if (symbol.kind() == Symbol.Kind.VARIABLE) {
//                    // TODO: Differentiate params vs locals
//                    // For now, treat all as locals
//                    addressMap.put(symbol, new TamAddress(TamRegister.LB, localOffset));
//                    localOffset += 1; // Increment by symbol size
//                }
//            }
        }
    }
}