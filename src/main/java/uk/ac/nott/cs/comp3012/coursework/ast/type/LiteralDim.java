package uk.ac.nott.cs.comp3012.coursework.ast.type;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

// --- Dimensions ---
public class LiteralDim extends AbstractAstNode implements Dimension {
    public final int size;
    public LiteralDim(Token t, int size) { super(t); this.size = size; }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
