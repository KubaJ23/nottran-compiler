package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.Dimension;

import java.util.List;

public class RealType extends ArrayableTypeSpec {
    public RealType(Token t, List<Dimension> d) { super(t, d); }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
