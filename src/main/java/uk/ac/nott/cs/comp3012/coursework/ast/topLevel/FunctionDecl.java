package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.type.VariableDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.Statement;

import java.util.List;

public record FunctionDecl(Token startToken, String name, List<String> parameterNames, String resultName,
                           List<VariableDecl> declarations, List<Statement> statements) implements ProgramUnit {
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
