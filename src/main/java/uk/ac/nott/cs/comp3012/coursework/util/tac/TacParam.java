package uk.ac.nott.cs.comp3012.coursework.util.tac;

public sealed interface TacParam {
    record Value(int value) implements TacParam {
        @Override
        public String toString() {return String.valueOf(value);}
    }

    record Variable(String value) implements TacParam {
        @Override
        public String toString() {return value;}
    }

    // NEW
    record Label(String value) implements TacParam {
        @Override
        public String toString() {return value;}
    }

    // NEW: For string literals
    record ConstString(String value) implements TacParam {
        @Override
        public String toString() {return "\"" + value + "\"";}
    }
}