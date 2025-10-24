package uk.ac.nott.cs.comp3012.coursework.ast.topLevel;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

public sealed interface ProgramUnit extends Ast permits DerivedTypeDecl, FunctionDecl, ProgramDecl, SubroutineDecl {}
