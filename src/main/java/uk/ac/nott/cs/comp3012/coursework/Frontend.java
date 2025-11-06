package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBuilder;
import uk.ac.nott.cs.comp3012.coursework.ast.AstPrinter;

public class Frontend implements Compiler.Frontend {
    SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();

    @Override
    public Ast runFrontend(String programText) {
        NottscriptLexer lexer = new NottscriptLexer(CharStreams.fromString(programText));
        TokenStream tokenStream = new CommonTokenStream(lexer);
        NottscriptParser parser = new NottscriptParser(tokenStream);
        NottscriptParser.CompilationUnitContext fileContext = parser.compilationUnit();

        AstBuilder astBuilder = new AstBuilder();
        Ast ast = fileContext.accept(astBuilder);

        symbolTableBuilder.build((Ast.CompilationUnit) ast);

        TypeChecker typeChecker = new TypeChecker(symbolTableBuilder.getGlobalScope(), symbolTableBuilder.getScopeMap());
        typeChecker.visit(ast);

        return ast;
    }
}
