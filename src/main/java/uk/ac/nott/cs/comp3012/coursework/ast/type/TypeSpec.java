package uk.ac.nott.cs.comp3012.coursework.ast.type;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

public sealed interface TypeSpec extends Ast permits DerivedType, IntrinsicType {}
