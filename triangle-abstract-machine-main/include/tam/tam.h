//===-----------------------------------------------------------------------===//
//
// This file is part of tam-cpp, copyright (c) Ian Knight 2025.
//
// tam-cpp is free software: you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the Free Software
// Foundation, either version 3 of the License, or (at your option) any later
// version.
//
// tam-cpp is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with tam-cpp. If not, see <https://www.gnu.org/licenses/>.
//
//===-----------------------------------------------------------------------===//
//
/// @file tam.h
/// This file declares the core of the TAM interface. It declares the
/// `TamEmulator` class itself, along with the `TamInstruction` class used to
/// pass instructions.
//
//===-----------------------------------------------------------------------===//

#ifndef TAM_TAM_H__
#define TAM_TAM_H__

#include <stdint.h>
#include <stdio.h>

#include <array>
#include <map>
#include <string>
#include <vector>

namespace tam {

typedef uint32_t TamCode;
typedef int16_t TamData;
typedef uint16_t TamAddr;

/// Number of addressable words in memory.
///
constexpr const int kMemSize = 65536;

/// Index of highest-addressed word in memory.
///
constexpr const int kMaxAddr = kMemSize - 1;

/// Index of code top register
///
constexpr const uint8_t CT = 1;

/// Index of primitive base register
///
constexpr const uint8_t PB = 2;

/// Index of primitive top register
///
constexpr const uint8_t PT = 3;

/// Index of stack base register
///
constexpr const uint8_t SB = 4;

/// Index of stack top register
///
constexpr const uint8_t ST = 5;

/// Index of heap base register
///
constexpr const uint8_t HB = 6;

/// Index of heap top register
///
constexpr const uint8_t HT = 7;

/// Index of local base register
///
constexpr const uint8_t LB = 8;

/// Index of code pointer register
///
constexpr const uint8_t CP = 15;

/// A single TAM instruction.
///
struct TamInstruction {
    uint8_t op;  ///< Opcode
    uint8_t r;   ///< Register
    uint8_t n;   ///< Unsigned operand
    int16_t d;   ///< Signed operand
};

/// A TAM emulator.
///
/// The emulator class is responsible for simulating all operations that would
/// happen on a physical CPU. Methods are provided for the fetch-decode-execute
/// cycle, and all methods will directly update registers and memory
/// appropriately.
class TamEmulator {
   public:
    /// Construct a new emulator that uses `stdin` and `stdout` for I/O.
    ///
    /// On creation, all memory is zeroed and registers are set to default
    /// values. Registers default to 0 except for `HB`` and `HT`, which default
    /// to the highest address.
    TamEmulator();

    /// Construct a new emulator that uses the specified file streams for I/O.
    ///
    /// Members are initialised in the same manner as the default constructor.
    TamEmulator(FILE*, FILE*);

    /// Close the input and output streams if they are not stdin or stdout.
    ///
    ~TamEmulator() {
        if (this->instream_ != stdin) {
            fclose(this->instream_);
        }

        if (this->outstream_ != stdout) {
            fclose(this->outstream_);
        }
    }

    /// Sets the program to be run by this emulator.
    ///
    /// Code words are copied into the code store, and CT, PB, and PT registers
    /// are set accordingly.
    ///
    /// @param program program code to load
    /// @throws std::runtime_exception if the provided program is too large to
    /// fit in memory
    void LoadProgram(const std::vector<TamCode>& program);

    /// Obtains the next instruction to execute.
    ///
    /// This method increments the code pointer as part of its operation.
    ///
    /// @return the instruction
    /// @throws std::runtime_exception if the code pointer pointed outside of
    /// allocated code memory
    TamInstruction FetchDecode();

    /// Executes the given instruction.
    ///
    /// @param instr instruction to execute
    /// @return `true` if execution should continue, `false` if not
    /// @throws std::runtime_exception if any error occurred during execution
    bool Execute(TamInstruction instr);

    /// Return a string representing the current contents of the stack and any
    /// allocated blocks on the heap.
    ///
    /// @return the stack and heap contents
    const std::string GetSnapshot() const;

   protected:
    /// Attempt to allocate some memory on the heap.
    ///
    /// @param n size of requested block
    /// @return address of first word in the block
    TamAddr Allocate(int n);

    /// Attempt to free the allocated block beginning at `Addr`.
    ///
    /// @param addr start address of block
    /// @param size expected size of block
    void Free(TamAddr addr, TamData size);

    /// Push a value to the top of the stack.
    ///
    /// This method updates the stack top register.
    ///
    /// @param value value to push
    void PushData(TamData value);

    /// Remove and return the top value of the stack.
    ///
    /// This method updates the stack top register, but does not actually delete
    /// the value that was stored. It will remain until overwritten by a future
    /// call.
    ///
    /// @return the data
    TamData PopData();

    void ExecuteLoad(TamInstruction instr);
    void ExecuteLoada(TamInstruction instr);
    void ExecuteLoadi(TamInstruction instr);
    void ExecuteLoadl(TamInstruction instr);
    void ExecuteStore(TamInstruction instr);
    void ExecuteStorei(TamInstruction instr);
    void ExecuteCall(TamInstruction instr);
    void ExecuteCallPrimitive(TamInstruction instr);
    void ExecuteCalli(TamInstruction instr);
    void ExecuteReturn(TamInstruction instr);
    void ExecutePush(TamInstruction instr);
    void ExecutePop(TamInstruction instr);
    void ExecuteJump(TamInstruction instr);
    void ExecuteJumpi(TamInstruction instr);
    void ExecuteJumpif(TamInstruction instr);

    void PrimitiveNot();
    void PrimitiveAnd();
    void PrimitiveOr();
    void PrimitiveSucc();
    void PrimitivePred();
    void PrimitiveNeg();
    void PrimitiveAdd();
    void PrimitiveSub();
    void PrimitiveMult();
    void PrimitiveDiv();
    void PrimitiveMod();
    void PrimitiveLt();
    void PrimitiveLe();
    void PrimitiveGe();
    void PrimitiveGt();
    void PrimitiveEq();
    void PrimitiveNe();
    void PrimitiveEol();
    void PrimitiveEof();
    void PrimitiveGet();
    void PrimitivePut();
    void PrimitiveGeteol();
    void PrimitivePuteol();
    void PrimitiveGetint();
    void PrimitivePutint();
    void PrimitiveNew();
    void PrimitiveDispose();

    std::array<TamCode, kMemSize> code_store_;  ///< Stores code words
    std::array<TamData, kMemSize> data_store_;  ///< Stores data words
    std::array<TamAddr, 16> registers_;         ///< Stores register values

    std::map<TamAddr, int>
        allocated_blocks_,  ///< Records blocks of heap memory in use
        free_blocks_;       ///< Records blocks of unused heap memory

    FILE *instream_,  ///< File that input is read from
        *outstream_;  ///< File that output is written to
};

}  // namespace tam

#endif  // TAM_TAM_H__
