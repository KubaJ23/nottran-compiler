package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

/**
 * Represents all infix binary operations (+, -, *, /, **, //, .and., .or., ==, etc.)
 */
public class BinaryOpExpr extends AbstractAstNode implements Expression {
    public final Expression left;
    public final Token operator; // The operator token (PLUS, AND_OP, etc.)
    public final Expression right;

    public BinaryOpExpr(Expression left, Token operator, Expression right) {
        super(left.getStartToken()); // Precedence is handled by parser
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
