package uk.ac.nott.cs.comp3012.coursework.ast;

import org.antlr.v4.runtime.Token;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.Dimension;
import uk.ac.nott.cs.comp3012.coursework.ast.interfaces.TypeSpec;

import java.util.List; /**
 * Base class for types that can be arrays.
 */
public abstract class ArrayableTypeSpec extends AbstractAstNode implements TypeSpec {
    public final List<Dimension> dimensions;

    public ArrayableTypeSpec(Token startToken, List<Dimension> dimensions) {
        super(startToken);
        this.dimensions = List.copyOf(dimensions);
    }
}
