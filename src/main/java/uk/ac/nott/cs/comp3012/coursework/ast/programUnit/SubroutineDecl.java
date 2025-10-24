package uk.ac.nott.cs.comp3012.coursework.ast.programUnit;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.VariableDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.Statement;

import java.util.List; /**
 * A 'subroutine' block.
 * Corresponds to 'subroutineDeclaration'.
 */
public class SubroutineDecl extends AbstractAstNode implements ProgramUnit {
    public final String name;
    public final List<String> parameterNames;
    public final List<VariableDecl> declarations;
    public final List<Statement> statements;

    public SubroutineDecl(Token startToken, String name, List<String> parameterNames, List<VariableDecl> declarations, List<Statement> statements) {
        super(startToken);
        this.name = name;
        this.parameterNames = List.copyOf(parameterNames);
        this.declarations = List.copyOf(declarations);
        this.statements = List.copyOf(statements);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
