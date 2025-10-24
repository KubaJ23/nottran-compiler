package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

public record AssignmentStmt(Token startToken, Expression lvalue, Expression rvalue) implements Statement {
    @Override
    public <T> T accept(AstVisitor<T> v) {
        return v.visit(this);
    }
}
