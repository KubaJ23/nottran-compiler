package uk.ac.nott.cs.comp3012.coursework.ast.type;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

public class DerivedType extends ArrayableTypeSpec {
    public final String name;
    public DerivedType(Token t, String name, List<Dimension> d) {
        super(t, d);
        this.name = name;
    }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
