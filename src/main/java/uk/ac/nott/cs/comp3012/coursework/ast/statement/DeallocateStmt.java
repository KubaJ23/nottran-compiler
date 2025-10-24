package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

public record DeallocateStmt(Token startToken, String pointerName) implements Statement {
    @Override
    public <T> T accept(AstVisitor<T> v) {
        return v.visit(this);
    }
}
