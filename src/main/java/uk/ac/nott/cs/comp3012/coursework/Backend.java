package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

public class Backend implements Compiler.Backend {
    @Override
    public byte[] runBackend(Ast program) {
        return new byte[0];
    }
}
