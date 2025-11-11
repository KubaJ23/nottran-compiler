package uk.ac.nott.cs.comp3012.coursework;

import org.antlr.v4.runtime.*;
import uk.ac.nott.cs.comp3012.coursework.ast.*;
import uk.ac.nott.cs.comp3012.coursework.types.Type;

import java.util.*;

public class Frontend implements Compiler.Frontend {
    private Map<Ast, SymbolTable> scopeMap;
    private Map<Ast, Type> typeMap;


    @Override
    public Ast runFrontend(String programText) {
        NottscriptLexer lexer = new NottscriptLexer(CharStreams.fromString(programText));
        lexer.removeErrorListeners();
        AntlrErrorListener lexerErrors = new AntlrErrorListener();
        lexer.addErrorListener(lexerErrors);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        NottscriptParser parser = new NottscriptParser(tokenStream);
        parser.removeErrorListeners();
        AntlrErrorListener parserErrors = new AntlrErrorListener();
        parser.addErrorListener(parserErrors);

        NottscriptParser.CompilationUnitContext fileContext = parser.compilationUnit();

        if (lexerErrors.hasErrors() || parserErrors.hasErrors()) {
            List<String> allErrors = new ArrayList<>();
            allErrors.addAll(lexerErrors.getErrors());
            allErrors.addAll(parserErrors.getErrors());
            throw new CompilerException(null, String.join("\n", allErrors));
        }

        AstBuilder astBuilder = new AstBuilder();
        Ast ast = fileContext.accept(astBuilder);

        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
        symbolTableBuilder.build((Ast.CompilationUnit) ast);

        TypeChecker typeChecker = new TypeChecker(symbolTableBuilder.getScopeMap());
        typeChecker.visit(ast);

        this.scopeMap = symbolTableBuilder.getScopeMap();
        this.typeMap = typeChecker.getTypeMap();

        return ast;
    }

    public Map<Ast, SymbolTable> getScopeMap() {
        return scopeMap;
    }

    public Map<Ast, Type> getTypeMap() {
        return typeMap;
    }
}
