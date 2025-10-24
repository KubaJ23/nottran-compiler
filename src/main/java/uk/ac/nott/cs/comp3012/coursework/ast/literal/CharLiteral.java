package uk.ac.nott.cs.comp3012.coursework.ast.literal;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

public class CharLiteral extends AbstractAstNode implements Literal {
    public final String value; // The un-escaped, un-quoted string content
    public CharLiteral(Token t, String value) { super(t); this.value = value; }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
