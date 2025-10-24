package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.type.VariableDecl;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;
import uk.ac.nott.cs.comp3012.coursework.ast.statement.Statement;

import java.util.List; /**
 * A 'function' block.
 * Corresponds to 'functionDeclaration'.
 */
public class FunctionDecl extends AbstractAstNode implements ProgramUnit {
    public final String name;
    public final List<String> parameterNames;
    /** The result variable name, or null if not specified (defaults to function name). */
    public final String resultName;
    public final List<VariableDecl> declarations;
    public final List<Statement> statements;

    public FunctionDecl(Token startToken, String name, List<String> parameterNames, String resultName, List<VariableDecl> declarations, List<Statement> statements) {
        super(startToken);
        this.name = name;
        this.parameterNames = List.copyOf(parameterNames);
        this.resultName = resultName;
        this.declarations = List.copyOf(declarations);
        this.statements = List.copyOf(statements);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
