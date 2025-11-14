package uk.ac.nott.cs.comp3012.coursework.tam;

/**
 * TAM Primitive routine offsets as defined in the TAM specification.
 * each primitive performs an elementary operation on the stack.
 * <p>
 * a denotes a data address
 * c denotes a character
 * i denotes an integer
 * n denotes a non-negative integer
 * t denotes a truth value (0 for false or 1 for true)
 * v denotes a value of any type
 * w denotes any 1-word value
 */
public enum Primitives {
    /**
     * Identity: w -> w' (Set w' = w)
     */
    ID(1),

    /**
     * Logical NOT: t -> t' (Set t' = ¬t). A Logical NOT, as opposed to a bit-wise NOT.
     */
    NOT(2),

    /**
     * Logical AND: t1, t2 -> t' (Set t' = t1 ∧ t2)
     */
    AND(3),

    /**
     * Logical OR: t1, t2 -> t' (Set t' = t1 ∨ t2)
     */
    OR(4),

    /**
     * Successor: i -> i' (Set i' = i + 1)
     */
    SUCC(5),

    /**
     * Predecessor: i -> i' (Set i' = i - 1)
     */
    PRED(6),

    /**
     * Negation: i -> i' (Set i' = -i)
     */
    NEG(7),

    /**
     * Addition: i1, i2 -> i' (Set i' = i1 + i2)
     */
    ADD(8),

    /**
     * Subtraction: i1, i2 -> i' (Set i' = i1 - i2)
     */
    SUB(9),

    /**
     * Multiplication: i1, i2 -> i' (Set i' = i1 × i2)
     */
    MULT(10),

    /**
     * Division: i1, i2 -> i' (Set i' = i1 / i2, truncated)
     */
    DIV(11),

    /**
     * Modulo: i1, i2 -> i' (Set i' = i1 mod i2)
     */
    MOD(12),

    /**
     * Less than: i1, i2 -> t' (Set t' = true iff i1 < i2)
     */
    LT(13),

    /**
     * Less than or equal: i1, i2 -> t' (Set t' = true iff i1 ≤ i2)
     */
    LE(14),

    /**
     * Greater than or equal: i1, i2 -> t' (Set t' = true iff i1 ≥ i2)
     */
    GE(15),

    /**
     * Greater than: i1, i2 -> t' (Set t' = true iff i1 > i2)
     */
    GT(16),

    /**
     * Equal: v1, v2, n -> t' (Set t' = true iff v1 = v2, where v1 and v2 are n-word values)
     */
    EQ(17),

    /**
     * Not equal: v1, v2, n -> t' (Set t' = true iff v1 ≠ v2, where v1 and v2 are n-word values)
     */
    NE(18),

    /**
     * End of line: -> t' (Set t' = true iff next character is end-of-line)
     */
    EOL(19),

    /**
     * End of file: -> t' (Set t' = true iff no more characters to read)
     */
    EOF(20),

    /**
     * Get character: a -> (Read character and store at address a)
     */
    GET(21),

    /**
     * Put character: c -> (Write character c)
     */
    PUT(22),

    /**
     * Get end of line: -> (Read characters up to and including next end-of-line)
     */
    GETEOL(23),

    /**
     * Put end of line: -> (Write end-of-line)
     */
    PUTEOL(24),

    /**
     * Get integer: a -> (Read integer literal and store at address a)
     */
    GETINT(25),

    /**
     * Put integer: i -> (Write integer literal with value i)
     */
    PUTINT(26),

    /**
     * Allocate heap: n -> a' (Set a' = address of newly allocated n-word object in heap)
     */
    NEW(27),

    /**
     * Deallocate heap: n, a -> (Deallocate n-word object at address a in heap)
     */
    DISPOSE(28);


    final int offset;

    Primitives(int offset) {
        this.offset = offset;
    }
}