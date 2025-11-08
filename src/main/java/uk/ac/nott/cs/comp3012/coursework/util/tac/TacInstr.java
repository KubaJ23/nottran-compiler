package uk.ac.nott.cs.comp3012.coursework.util.tac;

import java.util.UUID;

/**
 * Represents a single three-address code instruction.
 * <p>
 * Use the constructor without an id parameter to automatically generate a unique ID for this node.
 * It is really quite wasteful to use a full UUID for each node, but it was simple to implement.
 * You are most welcome to change or improve this if you wish.
 *
 * @param id   unique ID of this instruction
 * @param op   operator
 * @param dst  destination
 * @param src1 first argument
 * @param src2 second argument (may be {@code null})
 */
public record TacInstr(UUID id, TacOp op, TacParam dst, TacParam src1, TacParam src2) {

    public TacInstr(TacOp op, TacParam dst, TacParam src1, TacParam src2) {
        this(UUID.randomUUID(), op, dst, src1, src2);
    }

    @Override
    public String toString() {
        switch (op) {
            // Binary (dst = src1 op src2)
            case ADD, SUB, MUL, DIV, LSS, LEQ, GRT, GEQ, EQL, NEQ, AND, OR:
                return String.format("%s = %s %s %s", dst, src1, op, src2);

            // Unary (dst = op src1)
            case NEG, NOT:
                return String.format("%s = %s %s", dst, op, src1);

            // Assignment (dst = src1)
            case ASSIGN, LOAD_CONST:
                return String.format("%s = %s", dst, src1);

            // Control Flow
            case LABEL:
                return String.format("%s:", dst);
            case GOTO:
                return String.format("GOTO %s", dst);
            case GOTO_IF_FALSE:
                return String.format("IF_FALSE %s GOTO %s", src1, dst);
            case GOTO_IF_TRUE:
                return String.format("IF_TRUE %s GOTO %s", src1, dst);

            // Procedure Calls
            case PARAM:
                return String.format("PARAM %s", src1);
            case CALL:
                if (dst == null) {
                    return String.format("CALL %s, %s", src1, src2); // Subroutine
                } else {
                    return String.format("%s = CALL %s, %s", dst, src1, src2); // Function
                }
            case RETURN:
                return (src1 == null) ? "RETURN" : String.format("RETURN %s", src1);

            // Memory
            case INDEX_READ:
                return String.format("%s = %s[%s]", dst, src1, src2);
            case INDEX_WRITE:
                return String.format("%s[%s] = %s", dst, src1, src2);
            case FIELD_READ:
                return String.format("%s = %s.%s", dst, src1, src2);
            case FIELD_WRITE:
                return String.format("%s.%s = %s", dst, src1, src2);

            // Heap
            case ALLOCATE:
                return String.format("%s = ALLOCATE(%s)", dst, src1);
            case DEALLOCATE:
                return String.format("DEALLOCATE %s", src1);

            // I/O
            case READ:
                return String.format("READ %s", src1);
            case WRITE:
                return String.format("WRITE %s", src1);
            case WRITE_CHAR:
                return String.format("WRITE_CHAR %s", src1);

            // System
            case HALT:
                return "HALT";

            default:
                // Fallback for any unhandled ops
                return String.format("%s: %s, %s, %s", op, dst, src1, src2);
        }
    }
}