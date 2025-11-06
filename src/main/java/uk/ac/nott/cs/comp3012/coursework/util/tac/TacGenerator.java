package uk.ac.nott.cs.comp3012.coursework.util.tac;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.AstVisitor;

import java.util.List;

public class TacGenerator implements AstVisitor<List<TacInstr>> {

    @Override
    public List<TacInstr> visit(Ast node) {
        return List.of();
    }
}
