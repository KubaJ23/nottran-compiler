package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class AllocateStmt extends AbstractAstNode implements Statement {
    public final String pointerName;
    public final List<Expression> dimensions;

    public AllocateStmt(Token startToken, String pointerName, List<Expression> dimensions) {
        super(startToken);
        this.pointerName = pointerName;
        this.dimensions = List.copyOf(dimensions);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
