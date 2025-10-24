package uk.ac.nott.cs.comp3012.coursework.ast.literal;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

public class RealLiteral extends AbstractAstNode implements Literal {
    public final double value; // You will parse this from the token text
    public RealLiteral(Token t, double value) { super(t); this.value = value; }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
