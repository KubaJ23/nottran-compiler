package uk.ac.nott.cs.comp3012.coursework.ast.type;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

public record DerivedType(Token startToken, String name, List<Dimension> d) implements TypeSpec {
    @Override
    public <T> T accept(AstVisitor<T> v) {
        return v.visit(this);
    }
}
