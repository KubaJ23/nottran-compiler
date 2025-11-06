package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

public record Symbol(String name, Type type, Kind kind) {
    public enum Kind {VARIABLE, FUNCTION, SUBROUTINE, TYPE_DEFINITION}
}