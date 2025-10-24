package uk.ac.nott.cs.comp3012.coursework.ast.literal;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

public class IntLiteral extends AbstractAstNode implements Literal {
    public final int value; // You will parse this from the token text
    public IntLiteral(Token t, int value) { super(t); this.value = value; }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
