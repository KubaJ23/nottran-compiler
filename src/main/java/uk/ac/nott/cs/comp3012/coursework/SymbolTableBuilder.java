package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.AstBaseVisitor;
import uk.ac.nott.cs.comp3012.coursework.util.types.Dimension;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

import java.util.*;

public class SymbolTableBuilder extends AstBaseVisitor<Void> {

    private SymbolTable globalScope;
    private SymbolTable currentScope;
    private final Map<Ast, SymbolTable> scopeMap = new HashMap<>();

    public SymbolTable getGlobalScope() {
        return globalScope;
    }

    public Map<Ast, SymbolTable> getScopeMap() {
        return scopeMap;
    }

    public void build(CompilationUnit cu) {
        this.globalScope = new SymbolTable(null);
        this.currentScope = globalScope;
        visit(cu);
        this.currentScope = null;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnit cu) {
        scopeMap.put(cu, globalScope);
        for (ProgramUnit unit : cu.programUnits()) {
            if (unit instanceof DerivedTypeDecl) {
                visit(unit);
            }
        }

        for (ProgramUnit unit : cu.programUnits()) {
            if (unit instanceof FunctionDecl || unit instanceof SubroutineDecl) {
                visit(unit);
            }
        }

        visit(cu.program());

        return null;
    }

    @Override
    public Void visitDerivedTypeDecl(DerivedTypeDecl dtd) {
        currentScope = globalScope;

        LinkedHashMap<String, Type> fields = new LinkedHashMap<>();

        for (VariableDecl fieldDecl : dtd.fields()) {
            if (fieldDecl.isPointer()) throw new CompilerException(fieldDecl.startToken(), "Derived types cannot contain POINTER fields.");
            if (fieldDecl.type() instanceof TypeSpec.DerivedType dt && dt.name().equals(dtd.name())) throw new CompilerException(dt.startToken(), "Derived types cannot be self-referential.");

            Type fieldType = resolveTypeSpec(fieldDecl.type());

            for (String fieldName : fieldDecl.names()) {
                if (fields.containsKey(fieldName)) throw new CompilerException(fieldDecl.startToken(), "Duplicate field name '" + fieldName + "' in type '" + dtd.name() + "'.");

                fields.put(fieldName, fieldType);
            }
        }

        Type derivedType = new Type.DerivedType(dtd.name(), fields);
        Symbol typeSymbol = new Symbol(dtd.name(), derivedType, Symbol.Kind.TYPE_DEFINITION);
        globalScope.add(typeSymbol, dtd.startToken());

        return defaultResult();
    }

    @Override
    public Void visitFunctionDecl(FunctionDecl fd) {
        SymbolTable localScope = new SymbolTable(globalScope);
        scopeMap.put(fd, localScope);
        currentScope = localScope;

        for (VariableDecl vd : fd.declarations()) {
            defineVariables(vd);
        }

        List<Type> paramTypes = new ArrayList<>();
        for (String paramName : fd.parameterNames()) {
            Symbol paramSymbol = currentScope.getSymbolLocal(paramName).orElseThrow(() -> new CompilerException(fd.startToken(), "Function parameter '" + paramName + "' is not declared in function '" + fd.name() + "'."));
            paramTypes.add(paramSymbol.type());
        }

        String resultName = fd.resultName();
        Symbol resultSymbol = currentScope.getSymbolLocal(resultName).orElseThrow(() -> new CompilerException(fd.startToken(), "Result variable '" + resultName + "' is not declared in function '" + fd.name() + "'."));
        Type returnType = resultSymbol.type();

        Type funcType = new Type.FunctionType(returnType, paramTypes);
        Symbol funcSymbol = new Symbol(fd.name(), funcType, Symbol.Kind.FUNCTION);
        globalScope.add(funcSymbol, fd.startToken());

        return defaultResult();
    }

    @Override
    public Void visitSubroutineDecl(SubroutineDecl sd) {
        SymbolTable localScope = new SymbolTable(globalScope);
        scopeMap.put(sd, localScope);
        currentScope = localScope;

        for (VariableDecl vd : sd.declarations()) {
            defineVariables(vd);
        }

        List<Type> paramTypes = new ArrayList<>();
        for (String paramName : sd.parameterNames()) {
            Symbol paramSymbol = currentScope.getSymbolLocal(paramName).orElseThrow(() -> new CompilerException(sd.startToken(), "Subroutine parameter '" + paramName + "' is not declared in subroutine '" + sd.name() + "'."));
            paramTypes.add(paramSymbol.type());
        }

        Type subType = new Type.SubroutineType(paramTypes);
        Symbol subSymbol = new Symbol(sd.name(), subType, Symbol.Kind.SUBROUTINE);
        globalScope.add(subSymbol, sd.startToken());

        return defaultResult();
    }

    @Override
    public Void visitProgramDecl(ProgramDecl pd) {
        SymbolTable localScope = new SymbolTable(globalScope);
        scopeMap.put(pd, localScope);
        currentScope = localScope;

        for (VariableDecl vd : pd.declarations()) {
            defineVariables(vd);
        }

        return defaultResult();
    }

    // Helpers

    // Add all variables in the variable declaration to the current scope
    private void defineVariables(VariableDecl vd) {
        Type baseType = resolveTypeSpec(vd.type());
        if (vd.isPointer()) {
            baseType = new Type.PointerType(baseType);
        } else {
            if (baseType instanceof Type.ArrayType arrType) {
                if (arrType.dimensions().stream().anyMatch(d -> d instanceof Dimension.Unknown)) {
                    throw new CompilerException(vd.startToken(), "Array with unknown dimensions (*) must be declared as a POINTER.");
                }
            }
        }
        for (String name : vd.names()) {
            Symbol varSymbol = new Symbol(name, baseType, Symbol.Kind.VARIABLE);
            currentScope.add(varSymbol, vd.startToken());
        }
    }

    // Translate an AST TypeSpec into a Type, handling array dimensions and derived types
    private Type resolveTypeSpec(TypeSpec typeSpec) {
        Type baseType;
        List<Dimension> dimensions;

        switch (typeSpec) {
            case TypeSpec.IntrinsicType it -> {
                baseType = it.type();
                dimensions = it.dimensions();
            }
            case TypeSpec.DerivedType dt -> {
                Symbol typeSymbol = globalScope.getSymbol(dt.name()).orElseThrow(() -> new CompilerException(dt.startToken(), "Unknown type '" + dt.name() + "'."));
                if (typeSymbol.kind() != Symbol.Kind.TYPE_DEFINITION) throw new CompilerException(dt.startToken(), "'" + dt.name() + "' is not a type.");
                baseType = typeSymbol.type();
                dimensions = dt.dimensions();
            }
            default -> throw new IllegalStateException("Unexpected TypeSpec node: " + typeSpec.getClass().getSimpleName());
        }

        if (dimensions.isEmpty()) {
            return baseType;
        } else {
            if (baseType instanceof Type.ArrayType) throw new CompilerException(typeSpec.startToken(), "Arrays of arrays are not allowed.");

            return new Type.ArrayType(baseType, dimensions);
        }
    }

    @Override
    public String toString() {
        List<String> lines = new ArrayList<>();
        lines.add("Global Scope:\n" + getGlobalScope().toSimpleString());
        lines.add(this.getGlobalScope().toSimpleString());

        this.getScopeMap().forEach((node, scope) -> {
            String scopeName = switch (node) {
                case Ast.ProgramUnit.ProgramDecl pd -> "PROGRAM " + pd.name();
                case Ast.ProgramUnit.FunctionDecl fd -> "FUNCTION " + fd.name();
                case Ast.ProgramUnit.SubroutineDecl sd -> "SUBROUTINE " + sd.name();
                case Ast.ProgramUnit.DerivedTypeDecl dtd -> "DERIVED TYPE " + dtd.name();
                default -> node.getClass().getSimpleName();
            };
            lines.add("\nScope for: " + scopeName);
            lines.add(scope.toSimpleString());
        });
        return String.join("\n", lines);
    }
}