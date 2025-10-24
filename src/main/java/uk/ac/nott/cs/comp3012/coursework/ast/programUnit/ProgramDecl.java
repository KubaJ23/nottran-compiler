package uk.ac.nott.cs.comp3012.coursework.ast.programUnit;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.VariableDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.Statement;

import java.util.List; /**
 * A 'program' block (main program).
 * Corresponds to 'programDeclaration'.
 */
public class ProgramDecl extends AbstractAstNode implements ProgramUnit {
    public final String name;
    public final List<VariableDecl> declarations;
    public final List<Statement> statements;

    public ProgramDecl(Token startToken, String name, List<VariableDecl> declarations, List<Statement> statements) {
        super(startToken);
        this.name = name;
        this.declarations = List.copyOf(declarations);
        this.statements = List.copyOf(statements);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
