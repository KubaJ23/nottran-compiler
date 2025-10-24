package uk.ac.nott.cs.comp3012.coursework.ast.type;

public sealed interface Dimension permits Dimension.LiteralDim, Dimension.PointerDim {
    record LiteralDim(int size) implements Dimension {
    }

    record PointerDim() implements Dimension {
    }
}

