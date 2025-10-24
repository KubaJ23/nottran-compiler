package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.Dimension;

public class PointerDim extends AbstractAstNode implements Dimension {
    public PointerDim(Token t) { super(t); }
    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
