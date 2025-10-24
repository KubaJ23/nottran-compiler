package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class DoLoopStmt extends AbstractAstNode implements Statement {
    public final String loopVariable;
    public final Expression start;
    public final Expression end;
    /** The step expression, or null if not specified (defaults to 1). */
    public final Expression step;
    public final List<Statement> body;

    public DoLoopStmt(Token startToken, String loopVariable, Expression start, Expression end, Expression step, List<Statement> body) {
        super(startToken);
        this.loopVariable = loopVariable;
        this.start = start;
        this.end = end;
        this.step = step;
        this.body = List.copyOf(body);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
