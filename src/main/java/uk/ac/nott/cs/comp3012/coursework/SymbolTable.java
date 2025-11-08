package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SymbolTable {

    private final SymbolTable parent;
    private final Map<String, Symbol> symbols = new HashMap<>();
    private final int depth;

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
        this.depth = (parent == null) ? 0 : parent.depth + 1;
    }

    public int getDepth() {
        return depth;
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public void add(Symbol symbol, Token token) {
        if (symbols.containsKey(symbol.name())) {
            throw new CompilerException(token, "Symbol '" + symbol.name() + "' is already defined in this scope.");
        }
        symbols.put(symbol.name(), symbol);
    }

    public Optional<Symbol> getSymbol(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        if (parent != null) {
            return parent.getSymbol(name);
        }
        return Optional.empty();
    }

    public Optional<Symbol> getSymbolLocal(String name) {
        return Optional.ofNullable(symbols.get(name));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toSimpleString());
        if (parent != null) {
            sb.append("\n").append(parent);
        }
        return sb.toString();
    }

    public String toSimpleString() {
        String indent = "  ".repeat(depth);
        String header = indent + "Scope (Depth " + depth + ")";
        if (symbols.isEmpty()) {
            return header + "\n" + indent + "[Empty]";
        }

        String body = symbols.values().stream().map(s -> String.format("%s%s : %-15s : %s", indent, s.name(), s.kind(), s.type().toString())).collect(Collectors.joining("\n"));

        return header + "\n" + body;
    }
}