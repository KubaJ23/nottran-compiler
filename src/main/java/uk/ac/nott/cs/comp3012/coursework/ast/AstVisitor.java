package uk.ac.nott.cs.comp3012.coursework.ast;

/**
 * A generic visitor interface for the Ast sealed hierarchy.
 */
public interface AstVisitor<T> {
    T visit(Ast node);
}