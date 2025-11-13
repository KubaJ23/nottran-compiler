package uk.ac.nott.cs.comp3012.coursework.tam;

/**
 * TAM Registers as defined in the TAM specification.
 * Each register has a dedicated purpose.
 */
public enum TamRegister {
    /**
     * Code Base - Points to base of code segment (constant).
     */
    CB(0),

    /**
     * Code Top - Points to top of code segment (constant).
     */
    CT(1),

    /**
     * Primitives Base - Points to base of primitive segment (constant).
     */
    PB(2),

    /**
     * Primitives Top - Points to top of primitive segment (constant).
     */
    PT(3),

    /**
     * Stack Base - Points to base of stack (constant).
     */
    SB(4),

    /**
     * Stack Top - Points to top of stack (changed by most instructions).
     */
    ST(5),

    /**
     * Heap Base - Points to base of heap (constant).
     */
    HB(6),

    /**
     * Heap Top - Points to top of heap (changed by heap routines).
     */
    HT(7),

    /**
     * Local Base - Points to base of topmost frame (changed by CALL and RETURN).
     */
    LB(8),

    /**
     * Local Base 1 - Pseudo-register: L1 = content(LB).
     * Points to frame of routine textually enclosing current routine.
     */
    L1(9),

    /**
     * Local Base 2 - Pseudo-register: L2 = content(content(LB)).
     * Points to frame 2 levels up in static scope chain.
     */
    L2(10),

    /**
     * Local Base 3 - Pseudo-register: L3 = content(content(content(LB))).
     * Points to frame 3 levels up in static scope chain.
     */
    L3(11),

    /**
     * Local Base 4 - Pseudo-register: L4 = content(content(content(content(LB)))).
     * Points to frame 4 levels up in static scope chain.
     */
    L4(12),

    /**
     * Local Base 5 - Pseudo-register: L5 = content(content(content(content(content(LB))))).
     * Points to frame 5 levels up in static scope chain.
     */
    L5(13),

    /**
     * Local Base 6 - Pseudo-register: L6 = content(content(content(content(content(content(LB)))))).
     * Points to frame 6 levels up in static scope chain.
     */
    L6(14),

    /**
     * Code Pointer - Points to next instruction to execute (changed by all instructions).
     */
    CP(15);

    public final int value;

    TamRegister(int value) {
        this.value = value;
    }
}