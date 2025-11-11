package uk.ac.nott.cs.comp3012.coursework.tam;

/**
 * A sealed interface for the intermediate representation (IR) of TAM.
 * <p>
 * This IR uses symbolic labels for jumps and calls, which are resolved into absolute addresses by the TamAssembler.
 */
public sealed interface IntermediateTam {

    /**
     * Represents a symbolic label in the code.
     */
    record Label(String name) implements IntermediateTam {}

    /**
     * Represents a standard TAM instruction that does NOT jump.
     */
    record Instr(TamOpcode op, TamRegister r, int n, int d) implements IntermediateTam {}

    /**
     * Represents a jump or call instruction that targets a symbolic label.
     */
    record Jump(TamOpcode op, TamRegister r, int n, String label) implements IntermediateTam {}
}