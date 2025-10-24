package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

/**
 * A variable name used as an expression.
 * Corresponds to 'atom #NameExpr'.
 */
public class NameExpr extends AbstractAstNode implements Expression {
    public final String name;

    public NameExpr(Token startToken, String name) {
        super(startToken);
        this.name = name;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
