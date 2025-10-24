package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.type.VariableDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List; /**
 * A 'type' definition block.
 * Corresponds to 'derivedTypeDeclaration'.
 */
public class DerivedTypeDecl extends AbstractAstNode implements ProgramUnit {
    public final String name;
    public final List<VariableDecl> fields;

    public DerivedTypeDecl(Token startToken, String name, List<VariableDecl> fields) {
        super(startToken);
        this.name = name;
        this.fields = List.copyOf(fields);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
