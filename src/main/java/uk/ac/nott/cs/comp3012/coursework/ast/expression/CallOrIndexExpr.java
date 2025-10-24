package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

public record CallOrIndexExpr(Token startToken, Expression base, List<Expression> arguments) implements Expression {
    @Override
    public <T> T accept(AstVisitor<T> v) {
        return v.visit(this);
    }
}
