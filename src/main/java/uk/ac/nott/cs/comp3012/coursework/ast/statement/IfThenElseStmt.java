package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class IfThenElseStmt extends AbstractAstNode implements Statement {
    public final Expression condition;
    public final List<Statement> thenBlock;
    public final List<Statement> elseBlock; // Will be empty if no 'else'

    public IfThenElseStmt(Token startToken, Expression condition, List<Statement> thenBlock, List<Statement> elseBlock) {
        super(startToken);
        this.condition = condition;
        this.thenBlock = List.copyOf(thenBlock);
        this.elseBlock = List.copyOf(elseBlock);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
