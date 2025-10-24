package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.type.VariableDecl;

import java.util.List;

public record DerivedTypeDecl(Token startToken, String name, List<VariableDecl> fields) implements ProgramUnit {
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
