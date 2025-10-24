package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

public class DeallocateStmt extends AbstractAstNode implements Statement {
    public final String pointerName;

    public DeallocateStmt(Token startToken, String pointerName) {
        super(startToken);
        this.pointerName = pointerName;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
