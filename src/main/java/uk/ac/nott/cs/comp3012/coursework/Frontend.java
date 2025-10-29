package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBuilder;
import uk.ac.nott.cs.comp3012.coursework.ast.AstPrinter;

public class Frontend implements Compiler.Frontend {
    @Override
    public Ast runFrontend(String programText) {
        NottscriptLexer lexer = new NottscriptLexer(CharStreams.fromString(programText));
        TokenStream tokenStream = new CommonTokenStream(lexer);
        NottscriptParser parser = new NottscriptParser(tokenStream);

        NottscriptParser.CompilationUnitContext fileContext = parser.compilationUnit();

        Ast ast = fileContext.accept(new AstBuilder());
        AstPrinter printer = new AstPrinter();
        printer.print(ast);


        return null;
    }
}
