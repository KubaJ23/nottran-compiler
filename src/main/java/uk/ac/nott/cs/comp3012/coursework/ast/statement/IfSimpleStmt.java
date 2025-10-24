package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

public class IfSimpleStmt extends AbstractAstNode implements Statement {
    public final Expression condition;
    public final Statement thenStatement;

    public IfSimpleStmt(Token startToken, Expression condition, Statement thenStatement) {
        super(startToken);
        this.condition = condition;
        this.thenStatement = thenStatement;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
