package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.Dimension;

import java.util.List;

// --- Concrete TypeSpec Nodes ---
public class IntegerType extends ArrayableTypeSpec {
    public IntegerType(Token t, List<Dimension> d) { super(t, d); }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
