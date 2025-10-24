package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

public class AssignmentStmt extends AbstractAstNode implements Statement {
    public final Expression lvalue; // A 'postfixExpr', to be validated as lvalue
    public final Expression rvalue; // The 'expr'

    public AssignmentStmt(Token startToken, Expression lvalue, Expression rvalue) {
        super(startToken);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
