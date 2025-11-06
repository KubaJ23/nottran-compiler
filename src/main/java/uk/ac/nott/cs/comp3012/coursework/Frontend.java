package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBuilder;
import uk.ac.nott.cs.comp3012.coursework.ast.AstPrinter;
import uk.ac.nott.cs.comp3012.coursework.util.types.TypeChecker;


public class Frontend implements Compiler.Frontend {
    @Override
    public Ast runFrontend(String programText) {
        NottscriptLexer lexer = new NottscriptLexer(CharStreams.fromString(programText));
        TokenStream tokenStream = new CommonTokenStream(lexer);
        NottscriptParser parser = new NottscriptParser(tokenStream);
        NottscriptParser.CompilationUnitContext fileContext = parser.compilationUnit();

        AstBuilder astBuilder = new AstBuilder();
        Ast ast = fileContext.accept(astBuilder);

//        new AstPrinter().print(ast);

//        System.out.println("Creating Symbol Table...\n");
        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();

        symbolTableBuilder.build((Ast.CompilationUnit) ast);
//        System.out.println(symbolTableBuilder);

//        System.out.println("\nRunning Type Checking pass...");
        TypeChecker typeChecker = new TypeChecker(symbolTableBuilder.getGlobalScope(), symbolTableBuilder.getScopeMap());
        typeChecker.visit(ast);

        return ast;
    }
}
