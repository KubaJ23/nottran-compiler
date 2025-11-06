package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

import java.util.Map;

public class Backend implements Compiler.Backend {
    @Override
    public byte[] runBackend(Ast program, Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap) {
        

        return new byte[0];
    }
}
