package uk.ac.nott.cs.comp3012.coursework.ast.expression;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

public sealed interface Expression extends Ast permits BinaryOpExpr, BozLiteral, CallOrIndexExpr, CharLiteral, FieldAccessExpr, IntLiteral, LogicalLiteral, NameExpr, RealLiteral {
}
