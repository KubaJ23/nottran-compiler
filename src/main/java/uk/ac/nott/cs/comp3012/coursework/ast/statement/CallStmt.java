package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.expression.Expression;

import java.util.List;

public class CallStmt extends AbstractAstNode implements Statement {
    public final String subroutineName;
    public final List<Expression> arguments;

    public CallStmt(Token startToken, String subroutineName, List<Expression> arguments) {
        super(startToken);
        this.subroutineName = subroutineName;
        this.arguments = List.copyOf(arguments);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
