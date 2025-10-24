package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.TypeSpec;

import java.util.List; /**
 * A variable declaration line (e.g., 'integer :: a, b').
 * Corresponds to 'variableDecl' and 'derivedTypeVarDecl'.
 */
public class VariableDecl extends AbstractAstNode {
    public final TypeSpec type;
    public final boolean isPointer;
    public final List<String> names;

    public VariableDecl(Token startToken, TypeSpec type, boolean isPointer, List<String> names) {
        super(startToken);
        this.type = type;
        this.isPointer = isPointer;
        this.names = List.copyOf(names);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
