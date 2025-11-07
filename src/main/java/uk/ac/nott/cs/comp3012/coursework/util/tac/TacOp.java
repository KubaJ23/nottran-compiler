package uk.ac.nott.cs.comp3012.coursework.util.tac;

public enum TacOp {
    // Binary Arithmetic/Logic (dst, src1, src2)
    ADD, SUB, MUL, DIV,
    LSS, LEQ, GRT, GEQ, EQL, NEQ,
    AND, OR,

    // Unary (dst, src1)
    NEG, // Negation (e.g., -x)
    NOT, // Logical NOT

    // Assignment (dst, src1)
    ASSIGN,       // dst = src1
    LOAD_CONST,   // dst = const(src1)

    // Control Flow (dst_label, src1_cond)
    GOTO,         // goto dst_label
    GOTO_IF_FALSE, // if_false src1_cond goto dst_label
    GOTO_IF_TRUE,  // if_true src1_cond goto dst_label
    LABEL,        // dst_label:

    // Procedure Calls (dst_result, src1_name, src2_arg_count)
    PARAM,        // param src1
    CALL,         // dst_result = call src1_name, src2_arg_count
    RETURN,       // return src1

    // Memory (dst, src1, src2)
    FIELD_READ,   // dst = src1_base.%src2_field
    FIELD_WRITE,  // dst_base.%src1_field = src2_val
    INDEX_READ,   // dst = src1_base[src2_index]
    INDEX_WRITE,  // dst_base[src1_index] = src2_val

    // Heap (dst_ptr, src1_size)
    ALLOCATE,     // dst_ptr = allocate(src1_size_elements)
    DEALLOCATE,   // deallocate src1_ptr

    // I/O (src1)
    READ,         // read src1 (int/logical)
    WRITE,        // write src1 (int/logical/pointer address)
    WRITE_CHAR,   // write src1 (a single char value)

    // System
    HALT          // halt
}