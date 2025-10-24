package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

/**
 * The root of the AST, representing the entire file.
 * Corresponds to 'compilationUnit'.
 */
public record CompilationUnit(Token startToken, ProgramDecl program, List<ProgramUnit> programUnits) {
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
