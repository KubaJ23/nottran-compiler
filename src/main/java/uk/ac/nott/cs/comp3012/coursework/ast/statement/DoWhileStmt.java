package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class DoWhileStmt extends AbstractAstNode implements Statement {
    public final Expression condition;
    public final List<Statement> body;

    public DoWhileStmt(Token startToken, Expression condition, List<Statement> body) {
        super(startToken);
        this.condition = condition;
        this.body = List.copyOf(body);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
