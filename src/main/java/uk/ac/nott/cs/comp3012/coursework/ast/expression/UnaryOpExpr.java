package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

/**
 * Represents unary operations (currently just + and -)
 */
public class UnaryOpExpr extends AbstractAstNode implements Expression {
    public final Token operator; // PLUS or MINUS
    public final Expression operand;

    public UnaryOpExpr(Token operator, Expression operand) {
        super(operator);
        this.operator = operator;
        this.operand = operand;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
