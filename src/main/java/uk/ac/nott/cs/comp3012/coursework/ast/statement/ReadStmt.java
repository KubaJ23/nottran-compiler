package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class ReadStmt extends AbstractAstNode implements Statement {
    public final List<Expression> designators; // List of 'postfixExpr'

    public ReadStmt(Token startToken, List<Expression> designators) {
        super(startToken);
        this.designators = List.copyOf(designators);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
