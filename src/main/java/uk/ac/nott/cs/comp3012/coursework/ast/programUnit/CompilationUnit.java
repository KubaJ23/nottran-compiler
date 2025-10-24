package uk.ac.nott.cs.comp3012.coursework.ast.programUnit;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AbstractAstNode;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.AstVisitor;

import java.util.List; /**
 * The root of the AST, representing the entire file.
 * Corresponds to 'compilationUnit'.
 */
public class CompilationUnit extends AbstractAstNode {
    public final List<ProgramUnit> programUnits;

    public CompilationUnit(Token startToken, List<ProgramUnit> programUnits) {
        super(startToken);
        this.programUnits = List.copyOf(programUnits);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
