package uk.ac.nott.cs.comp3012.coursework.ast.type;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

/**
 * A variable declaration line (e.g., 'integer :: a, b').
 */

public record VariableDecl(Token startToken, TypeSpec type, boolean isPointer, List<String> names) implements Ast {
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
