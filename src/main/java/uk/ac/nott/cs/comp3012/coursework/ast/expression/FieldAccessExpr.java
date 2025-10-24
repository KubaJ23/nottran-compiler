package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

/**
 * A field access, e.g., 'a % b'.
 * Corresponds to 'postfixSuffix #FieldAccess'.
 * The AST builder will chain these: 'a % b % c' -> FieldAccess(FieldAccess("a", "b"), "c")
 */
public class FieldAccessExpr extends AbstractAstNode implements Expression {
    public final Expression base;
    public final String fieldName;

    public FieldAccessExpr(Token startToken, Expression base, String fieldName) {
        super(startToken);
        this.base = base;
        this.fieldName = fieldName;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
