package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

public record BinaryOpExpr(Token startToken, Expression left, Token operator, Expression right) implements Expression {
    @Override
    public <T> T accept(AstVisitor<T> v) {
        return v.visit(this);
    }
}
