package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class WriteStmt extends AbstractAstNode implements Statement {
    public final List<Expression> expressions;

    public WriteStmt(Token startToken, List<Expression> expressions) {
        super(startToken);
        this.expressions = List.copyOf(expressions);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
