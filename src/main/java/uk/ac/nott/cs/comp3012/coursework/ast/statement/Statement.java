package uk.ac.nott.cs.comp3012.coursework.ast.statement;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

public sealed interface Statement extends Ast permits AllocateStmt, AssignmentStmt, CallStmt, DeallocateStmt, DoLoopStmt, DoWhileStmt, IfSimpleStmt, IfThenElseStmt, ReadStmt, WriteStmt {
}
