package uk.ac.nott.cs.comp3012.coursework.util.tac;

public sealed interface TacParam {
    record Value(int value) implements TacParam {}
    record Variable(String value) implements TacParam {}

}
