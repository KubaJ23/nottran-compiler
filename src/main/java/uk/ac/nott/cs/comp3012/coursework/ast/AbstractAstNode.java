package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.Ast;

/**
 * Abstract base class for all AST nodes, storing the start token.
 */
public abstract class AbstractAstNode implements Ast {
    protected final Token startToken;

    public AbstractAstNode(Token startToken) {
        this.startToken = startToken;
    }

    @Override
    public Token getStartToken() {
        return startToken;
    }
}
