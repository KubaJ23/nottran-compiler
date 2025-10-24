package uk.ac.nott.cs.comp3012.coursework.ast.literal;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

/**
 * Binary, Octal, or Hex literals.
 */
public class BozLiteral extends AbstractAstNode implements Literal {
    public final String value; // e.g., "01101"
    public final char base; // 'b', 'o', or 'z'
    public BozLiteral(Token t, String value, char base) {
        super(t);
        this.value = value;
        this.base = base;
    }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
