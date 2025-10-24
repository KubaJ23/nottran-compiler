package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

import java.util.List; /**
 * A function call OR an array index, e.g., 'a(1)' or 'f(x)'.
 * Corresponds to 'postfixSuffix #CallOrIndex'.
 * The AST builder will chain these: 'a(1)(2)' -> CallOrIndex(CallOrIndex("a", [1]), [2])
 */
public class CallOrIndexExpr extends AbstractAstNode implements Expression {
    public final Expression base;
    public final List<Expression> arguments;

    public CallOrIndexExpr(Token startToken, Expression base, List<Expression> arguments) {
        super(startToken);
        this.base = base;
        this.arguments = List.copyOf(arguments);
    }

    @Override public <T> T accept(AstVisitor<T> v) { return v.visit(this); }
}
