package uk.ac.nott.cs.comp3012.coursework.ast;

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

/**
 * A visitor that prints a readable, indented, and coloured tree structure of the Ast.
 * This class visits the AST (records) after it has been built, not the
 * ANTLR parse tree.
 */
public class AstPrinter {

    private int indentLevel = 0;

    // ANSI escape codes for colours
    public static final String ANSI_RESET = "\u001B[0m";
    // We'll cycle through these colours based on indent level
    private static final String[] ANSI_COLORS = {"\u001B[31m", // Red
            "\u001B[32m", // Green
            "\u001B[33m", // Yellow
            "\u001B[34m", // Blue
            "\u001B[35m", // Magenta
            "\u001B[36m"  // Cyan
    };

    /**
     * Public entry point to print any AST node and its children.
     *
     * @param node The Ast node to print.
     */
    public void print(Ast node) {
        visit(node);
    }

    /**
     * Core recursive visit method.
     * Uses pattern matching to dispatch to the correct node type.
     */
    private void visit(Ast node) {
        if (node == null) {
            return; // Safety check for optional nodes
        }

        // Use pattern matching for sealed Ast interface
        switch (node) {
            // --- Top Level ---
            case CompilationUnit cu -> {
                printLine("CompilationUnit");
                indent();
                visit(cu.program());
                visitList("Other Units:", cu.programUnits());
                dedent();
            }

            // --- Program Units ---
            case ProgramUnit.ProgramDecl pd -> {
                printLine("Program: " + pd.name());
                indent();
                visitList("Declarations:", pd.declarations());
                visitList("Statements:", pd.statements());
                dedent();
            }
            case ProgramUnit.FunctionDecl fd -> {
                printLine("Function: " + fd.name() + ", Params: " + fd.parameterNames() + (fd.resultName() != null ? ", Result: " + fd.resultName() : ""));
                indent();
                visitList("Declarations:", fd.declarations());
                visitList("Statements:", fd.statements());
                dedent();
            }
            case ProgramUnit.SubroutineDecl sd -> {
                printLine("Subroutine: " + sd.name() + ", Params: " + sd.parameterNames());
                indent();
                visitList("Declarations:", sd.declarations());
                visitList("Statements:", sd.statements());
                dedent();
            }
            case ProgramUnit.DerivedTypeDecl dtd -> {
                printLine("Type: " + dtd.name());
                indent();
                visitList("Fields:", dtd.fields());
                dedent();
            }

            // --- Declarations & Types ---
            case VariableDecl vd -> {
                printLine("VarDecl: " + vd.names() + (vd.isPointer() ? ", POINTER" : ""));
                indent();
                visit(vd.type());
                dedent();
            }
            case TypeSpec.IntrinsicType it -> {
                printLine("Type: " + it.type() + dimToString(it.d()));
            }
            case TypeSpec.DerivedType dt -> {
                printLine("Type: " + dt.name() + dimToString(dt.d()));
            }

            // --- Statements ---
            case Statement.AssignmentStmt s -> {
                printLine("Assignment:");
                indent();
                visit(s.lvalue());
                visit(s.rvalue());
                dedent();
            }
            case Statement.CallStmt s -> {
                printLine("Call: " + s.subroutineName());
                indent();
                visitList("Arguments:", s.arguments());
                dedent();
            }
            case Statement.IfSimpleStmt s -> {
                printLine("IfSimple:");
                indent();
                visit(s.condition());
                visit(s.thenStatement());
                dedent();
            }
            case Statement.IfThenElseStmt s -> {
                printLine("IfThenElse:");
                indent();
                visit(s.condition());
                visitList("Then:", s.thenBlock());
                visitList("Else:", s.elseBlock());
                dedent();
            }
            case Statement.DoLoopStmt s -> {
                printLine("DoLoop: " + s.loopVariable() + " = " + (s.step() != null ? "start, end, step" : "start, end"));
                indent();
                visit(s.start());
                visit(s.end());
                visit(s.step());
                visitList("Body:", s.body());
                dedent();
            }
            case Statement.DoWhileStmt s -> {
                printLine("DoWhile:");
                indent();
                visit(s.condition());
                visitList("Body:", s.body());
                dedent();
            }
            case Statement.ReadStmt s -> {
                printLine("Read:");
                indent();
                visitList("LValues:", s.lvalues());
                dedent();
            }
            case Statement.WriteStmt s -> {
                printLine("Write:");
                indent();
                visitList("Expressions:", s.expressions());
                dedent();
            }
            case Statement.AllocateStmt s -> {
                printLine("Allocate: " + s.pointerName());
                indent();
                visitList("Dimensions:", s.dimensions());
                dedent();
            }
            case Statement.DeallocateStmt s -> {
                printLine("Deallocate: " + s.pointerName());
            }

            // --- Expressions ---
            case Expression.BinaryOpExpr e -> {
                printLine("BinaryOp: " + e.operator());
                indent();
                visit(e.left());
                visit(e.right());
                dedent();
            }
            case Expression.FieldAccessExpr e -> {
                printLine("FieldAccess: " + e.fieldName());
                indent();
                visit(e.base());
                dedent();
            }
            case Expression.CallOrIndexExpr e -> {
                printLine("CallOrIndex:");
                indent();
                visit(e.base());
                visitList("Arguments:", e.arguments());
                dedent();
            }
            case Expression.NameExpr e -> {
                printLine("Name: " + e.name());
            }

            // --- Literals ---
            case Expression.Literal.IntLiteral l -> {
                printLine("Int: " + l.value());
            }
            case Expression.Literal.RealLiteral l -> {
                printLine("Real: " + l.value());
            }
            case Expression.Literal.LogicalLiteral l -> {
                printLine("Logical: " + l.value());
            }
            case Expression.Literal.CharLiteral l -> {
                printLine("Char: \"" + l.value() + "\"");
            }
            case Expression.Literal.BozLiteral l -> {
                printLine("Boz: " + l.base() + "'" + l.value() + "'");
            }
        }
    }

    // ==================================================================
    // == Helper Methods
    // ==================================================================

    /**
     * Prints a label and then visits all nodes in a list.
     * Does nothing if the list is empty.
     */
    private void visitList(String label, List<? extends Ast> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        printLine(label); // Use the new printLine helper
        indent();
        for (Ast node : list) {
            visit(node);
        }
        dedent();
    }

    /**
     * Converts a list of dimensions to a string like "(10, *)".
     */
    private String dimToString(List<Dimension> dims) {
        if (dims == null || dims.isEmpty()) {
            return "";
        }
        return dims.stream().map(d -> {
            if (d instanceof LiteralDim ld) {
                return String.valueOf(ld.size());
            } else {
                return "*";
            }
        }).collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Prints the correct indentation with the correct colour.
     */
    private void printIndent() {
        // Select colour based on indent level
        String color = ANSI_COLORS[(indentLevel / 2) % ANSI_COLORS.length];
        System.out.print(color);
        System.out.print(" ".repeat(indentLevel));
    }

    /**
     * A new helper to print a line with indentation, colour, and reset.
     * This avoids repeating the ANSI codes in every visit method.
     */
    private void printLine(String text) {
        printIndent(); // This now sets the colour and prints spaces
        System.out.println(text + ANSI_RESET); // Print text and reset colour
    }

    private void indent() {
        indentLevel += 2;
    }

    private void dedent() {
        indentLevel -= 2;
    }
}