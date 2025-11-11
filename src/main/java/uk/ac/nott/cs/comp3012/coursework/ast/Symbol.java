package uk.ac.nott.cs.comp3012.coursework.ast;

import uk.ac.nott.cs.comp3012.coursework.types.Type;

public record Symbol(String name, Type type, Kind kind) {
    public enum Kind {VARIABLE, FUNCTION, SUBROUTINE, TYPE_DEFINITION}

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}