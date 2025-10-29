package uk.ac.nott.cs.comp3012.coursework.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Import all AST node definitions
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.ProgramUnit.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Statement.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Dimension.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.Expression.Literal.*;
import uk.ac.nott.cs.comp3012.coursework.ast.Ast.TypeSpec.IntrinsicType.VarType;

public class AstPrinter {

    private final List<String> prefixes = new ArrayList<>();
    //    private static final String BRANCH = "├── ";
//    private static final String LAST_BRANCH = "└── ";
//    private static final String VERTICAL = "│  ";
//    private static final String INDENT = "   ";
    private static final String BRANCH = "|-- ";
    private static final String LAST_BRANCH = "`-- ";
    private static final String VERTICAL = "|  ";
    private static final String INDENT = "   ";

    public static final String ANSI_RESET = "\u001B[0m";
    private static final String[] ANSI_COLORS = {"\u001B[31m", // Red
            "\u001B[32m", // Green
            "\u001B[33m", // Yellow
            "\u001B[34m", // Blue
            "\u001B[35m", // Magenta
            "\u001B[36m"  // Cyan
    };

    public void print(Ast node) {
        visit(node, true);
    }

    private void visit(Ast node, boolean isLast) {
        if (node == null) {
            return;
        }

        printNode(node, isLast);
        prefixes.add(isLast ? INDENT : VERTICAL);

        switch (node) {
            case CompilationUnit cu -> {
                visit(cu.program(), cu.programUnits().isEmpty());
                visitNodeList(cu.programUnits());
            }
            case ProgramUnit.ProgramDecl pd -> {
                visitNodeList("Declarations", pd.declarations(), pd.statements().isEmpty());
                visitNodeList("Statements", pd.statements(), true);
            }
            case ProgramUnit.FunctionDecl fd -> {
                visitNodeList("Declarations", fd.declarations(), fd.statements().isEmpty());
                visitNodeList("Statements", fd.statements(), true);
            }
            case ProgramUnit.SubroutineDecl sd -> {
                visitNodeList("Declarations", sd.declarations(), sd.statements().isEmpty());
                visitNodeList("Statements", sd.statements(), true);
            }
            case ProgramUnit.DerivedTypeDecl dtd -> visitNodeList("Fields", dtd.fields(), true);
            case VariableDecl vd -> {
                visit(vd.type(), true);
            }
            case Statement.AssignmentStmt s -> {
                visit(s.lvalue(), false);
                visit(s.rvalue(), true);
            }
            case Statement.CallStmt s -> {
                visitNodeList("Arguments", s.arguments(), true);
            }
            case Statement.IfSimpleStmt s -> {
                visit(s.condition(), false);
                visit(s.thenStatement(), true);
            }
            case Statement.IfThenElseStmt s -> {
                visit(s.condition(), s.thenBlock().isEmpty() && s.elseBlock().isEmpty());
                visitNodeList("Then", s.thenBlock(), s.elseBlock().isEmpty());
                visitNodeList("Else", s.elseBlock(), true);
            }
            case Statement.DoLoopStmt s -> {
                visit(s.start(), false);
                visit(s.end(), s.step() == null && s.body().isEmpty());
                visit(s.step(), s.body().isEmpty());
                visitNodeList("Body", s.body(), true);
            }
            case Statement.DoWhileStmt s -> {
                visit(s.condition(), s.body().isEmpty());
                visitNodeList("Body", s.body(), true);
            }
            case Statement.ReadStmt s -> {
                visitNodeList("LValues", s.lvalues(), true);
            }
            case Statement.WriteStmt s -> visitNodeList("Expressions", s.expressions(), true);
            case Statement.AllocateStmt s -> {
                visitNodeList("Dimensions", s.dimensions(), true);
            }
            case Expression.BinaryOpExpr e -> {
                visit(e.left(), false);
                visit(e.right(), true);
            }
            case Expression.FieldAccessExpr e -> {
                visit(e.base(), true);
            }
            case Expression.CallOrIndexExpr e -> {
                visit(e.base(), e.arguments().isEmpty());
                visitNodeList("Arguments", e.arguments(), true);
            }
            // Leaf nodes have no children to visit
            case TypeSpec _, DeallocateStmt _, NameExpr _, Literal _ -> {}
        }

        if (!prefixes.isEmpty()) {
            prefixes.remove(prefixes.size() - 1);
        }
    }

    private void printNode(Ast node, boolean isLast) {
        String prefix = prefixes.stream().collect(Collectors.joining());
        String branch = isLast ? LAST_BRANCH : BRANCH;
        String color = ANSI_COLORS[prefixes.size() % ANSI_COLORS.length];
        String nodeText = getNodeText(node);

        System.out.print(prefix + branch);
        System.out.println(color + nodeText + ANSI_RESET);
    }

    private String getNodeText(Ast node) {
        return switch (node) {
            case CompilationUnit cu -> "CompilationUnit";
            case ProgramUnit.ProgramDecl pd -> "Program: " + pd.name();
            case ProgramUnit.FunctionDecl fd -> "Function: " + fd.name() + "(Params: " + String.join(", ", fd.parameterNames()) + ")" + (fd.resultName() != null ? " Result: " + fd.resultName() : "");
            case ProgramUnit.SubroutineDecl sd -> "Subroutine: " + sd.name() + "(Params: " + String.join(", ", sd.parameterNames()) + ")";
            case ProgramUnit.DerivedTypeDecl dtd -> "Type: " + dtd.name();
            case VariableDecl vd -> "VarDecl: " + String.join(", ", vd.names()) + (vd.isPointer() ? " :: POINTER" : " ::");
            case TypeSpec.IntrinsicType it -> "TypeSpec: Intrinsic(" + it.type() + dimToString(it.d()) + ")";
            case TypeSpec.DerivedType dt -> "TypeSpec: Derived(" + dt.name() + dimToString(dt.d()) + ")";
            case Statement.AssignmentStmt s -> "Assignment (=)";
            case Statement.CallStmt s -> "Call: " + s.subroutineName();
            case Statement.IfSimpleStmt s -> "IfSimple";
            case Statement.IfThenElseStmt s -> "IfThenElse";
            case Statement.DoLoopStmt s -> "DoLoop: " + s.loopVariable();
            case Statement.DoWhileStmt s -> "DoWhile";
            case Statement.ReadStmt s -> "Read";
            case Statement.WriteStmt s -> "Write";
            case Statement.AllocateStmt s -> "Allocate: " + s.pointerName();
            case Statement.DeallocateStmt s -> "Deallocate: " + s.pointerName();
            case Expression.BinaryOpExpr e -> "BinaryOp: " + e.operator();
            case Expression.FieldAccessExpr e -> "FieldAccess: ." + e.fieldName();
            case Expression.CallOrIndexExpr e -> "CallOrIndex";
            case Expression.NameExpr e -> "Name: " + e.name();
            case Expression.Literal.IntLiteral l -> "Literal: " + l.value() + " (Int)";
            case Expression.Literal.RealLiteral l -> "Literal: " + l.value() + " (Real)";
            case Expression.Literal.LogicalLiteral l -> "Literal: ." + l.value() + ". (Logical)";
            case Expression.Literal.CharLiteral l -> "Literal: '" + l.value() + "' (Char)";
            case Expression.Literal.BozLiteral l -> "Literal: " + l.value() + " (Boz:" + l.base() + ")";
        };
    }

    private void visitNodeList(String label, List<? extends Ast> list, boolean isParentLast) {
        if (list == null || list.isEmpty()) {
            return;
        }

        String prefix = prefixes.stream().collect(Collectors.joining());
        String branch = isParentLast ? LAST_BRANCH : BRANCH;
        String color = ANSI_COLORS[prefixes.size() % ANSI_COLORS.length];

        System.out.print(prefix + branch);
        System.out.println(color + label + ANSI_RESET);

        prefixes.add(isParentLast ? INDENT : VERTICAL);
        visitNodeList(list);
        prefixes.remove(prefixes.size() - 1);
    }

    private void visitNodeList(List<? extends Ast> list) {
        if (list == null || list.isEmpty()) return;
        for (int i = 0; i < list.size(); i++) {
            visit(list.get(i), i == list.size() - 1);
        }
    }

    private String dimToString(List<Dimension> dims) {
        if (dims == null || dims.isEmpty()) {
            return "";
        }
        return dims.stream().map(d -> {
            if (d instanceof LiteralDim(int size)) {
                return String.valueOf(size);
            } else {
                return "*";
            }
        }).collect(Collectors.joining(", ", "(", ")"));
    }
}