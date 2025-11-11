package uk.ac.nott.cs.comp3012.coursework.types;

/**
 * A sealed interface to represent all types in the Nottran language.
 */
public sealed interface Dimension {
    record Known(int size) implements Dimension {}

    record Unknown() implements Dimension {}
}
