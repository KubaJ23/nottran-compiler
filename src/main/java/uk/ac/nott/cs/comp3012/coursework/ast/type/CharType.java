package uk.ac.nott.cs.comp3012.coursework.ast.type;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

public class CharType extends ArrayableTypeSpec {
    public CharType(Token t, List<Dimension> d) { super(t, d); }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
