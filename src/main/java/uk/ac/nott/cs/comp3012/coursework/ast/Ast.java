package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;

// --- Base AST Node Interface ---

/**
 * Base interface for all AST nodes.
 */
public interface Ast {
    /**
     * @return The starting ANTLR token, used for error reporting (line/column).
     */
    Token getStartToken();

    /**
     * Accepts a visitor.
     *
     * @param visitor The visitor to accept.
     * @param <T>     The return type of the visitor.
     * @return The result of the visitor's operation.
     */
    <T> T accept(AstVisitor<T> visitor);
}