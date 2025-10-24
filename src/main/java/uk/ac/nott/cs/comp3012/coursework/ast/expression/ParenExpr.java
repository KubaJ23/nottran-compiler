package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

/**
 * A parenthesised expression.
 * Corresponds to 'atom #ParenExpr'.
 */
public class ParenExpr extends AbstractAstNode implements Expression {
    public final Expression inner;

    public ParenExpr(Token startToken, Expression inner) {
        super(startToken);
        this.inner = inner;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
