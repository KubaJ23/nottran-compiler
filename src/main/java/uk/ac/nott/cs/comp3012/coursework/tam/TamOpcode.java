package uk.ac.nott.cs.comp3012.coursework.tam;

/**
 * TAM Opcodes as defined in the TAM specification.
 * Format: op(4 bits) | r(4 bits) | n(8 bits) | d(16 bits signed)
 */
public enum TamOpcode {
    /**
     * LOAD(n) d[r]
     * Fetch an n-word object from data address (d + register r) and push onto stack.
     */
    LOAD(0),

    /**
     * LOADA d[r]
     * Push the data address (d + register r) onto the stack.
     */
    LOADA(1),

    /**
     * LOADI(n)
     * Pop a data address from stack, fetch n-word object from that address, and push onto stack.
     */
    LOADI(2),

    /**
     * LOADL d
     * Push the 1-word literal value d onto the stack.
     */
    LOADL(3),

    /**
     * STORE(n) d[r]
     * Pop an n-word object from stack and store at data address (d + register r). ST is changed immediately after the first pop.
     */
    STORE(4),

    /**
     * STOREI(n)
     * Pop an address from stack, then pop n-word object from stack and store at that address.
     */
    STOREI(5),

    /**
     * CALL(n) d[r]
     * Call routine at code address (d + register r), using address in register n as static link.
     */
    CALL(6),

    /**
     * CALLI
     * Pop closure (static link and code address) from stack, then call routine at that code address.
     */
    CALLI(7),

    /**
     * RETURN(n) d
     * Return from current routine: pop n-word result, pop topmost frame, pop d words of arguments,
     * then push result back onto stack.
     */
    RETURN(8),

    /**
     * PUSH d
     * Push d (uninitialized) words onto the stack.
     */
    PUSH(10),

    /**
     * POP(n) d
     * Pop n-word result from stack, pop d more words, then push result back onto stack.
     */
    POP(11),

    /**
     * JUMP d[r]
     * Jump to code address (d + register r).
     * Result: CP = d + register r.
     */
    JUMP(12),

    /**
     * JUMPI
     * Pop a code address from stack, then jump to that address.
     */
    JUMPI(13),

    /**
     * JUMPIF(n) d[r]
     * Pop 1-word value from stack, jump to code address (d + register r) if and only if value equals n.
     */
    JUMPIF(14),

    /**
     * HALT
     * Stop execution of the program.
     */
    HALT(15);

    public final int value;

    TamOpcode(int value) {
        this.value = value;
    }
}