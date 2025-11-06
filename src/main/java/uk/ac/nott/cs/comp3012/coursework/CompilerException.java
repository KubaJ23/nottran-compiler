package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.Token;

/**
 * A custom, unchecked exception for fatal compilation errors.
 */
public class CompilerException extends RuntimeException {

    public CompilerException(Token token, String message) {
        super(formatMessage(token, message));
    }

    private static String formatMessage(Token token, String message) {
        if (token == null) {
            return "Error: " + message;
        }
        String location = "line " + token.getLine() + ":" + token.getCharPositionInLine();
        return "Error " + location + ": " + message;
    }
}