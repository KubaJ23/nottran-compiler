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
/// @file tam.cc
/// This file defines all methods of `TamEmulator` except for `Allocate`,
/// `free`, and the methods for executing primitive operations.
//
//===-----------------------------------------------------------------------===//

#include "tam/tam.h"

#include <assert.h>
#include <stdint.h>
#include <stdio.h>

#include <algorithm>
#include <iomanip>
#include <sstream>
#include <stack>
#include <vector>

#include "tam/error.h"

namespace tam {
TamEmulator::TamEmulator() {
    this->code_store_.fill(0);
    this->data_store_.fill(0);
    this->registers_.fill(0);

    this->registers_[HB] = kMaxAddr;
    this->registers_[HT] = kMaxAddr;

    this->instream_ = stdin;
    this->outstream_ = stdout;
}

TamEmulator::TamEmulator(FILE* instream, FILE* outstream) {
    if (!(instream && outstream)) {
        throw IoError("NULL passed for input or output");
    }

    this->code_store_.fill(0);
    this->data_store_.fill(0);
    this->registers_.fill(0);

    this->registers_[HB] = kMaxAddr;
    this->registers_[HT] = kMaxAddr;

    this->instream_ = instream;
    this->outstream_ = outstream;
}

void TamEmulator::LoadProgram(const std::vector<TamCode>& program) {
    if (program.size() > kMemSize) throw IoError("program file too large");

    this->code_store_.fill(0);
    std::copy(program.begin(), program.end(), this->code_store_.begin());
    this->registers_[CT] = program.size();
    this->registers_[PB] = program.size();
    this->registers_[PT] = this->registers_[PB] + 29;
}

TamInstruction TamEmulator::FetchDecode() {
    TamAddr addr = this->registers_[CP]++;
    if (addr >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation, addr);

    TamCode code = this->code_store_[addr];
    uint8_t op = (code & 0xf0000000) >> 28;
    assert(op <= 0xf);
    uint8_t r = (code & 0x0f000000) >> 24;
    assert(r <= 0xf);
    uint8_t n = (code & 0x00ff0000) >> 16;
    assert(n <= 0xff);
    int16_t d = code & 0x0000ffff;
    return TamInstruction{op, r, n, d};
}

void TamEmulator::PushData(TamData value) {
    TamAddr addr = this->registers_[ST];
    if (addr >= this->registers_[HT])
        throw RuntimeError(ExceptionKind::kStackOverflow,
                           this->registers_[CP] - 1);

    this->data_store_[addr] = value;
    this->registers_[ST]++;
    assert(this->data_store_[addr] == value);
}

TamData TamEmulator::PopData() {
    TamAddr addr = this->registers_[ST];
    if (this->registers_[ST] == 0)
        throw RuntimeError(ExceptionKind::kStackUnderflow,
                           this->registers_[CP] - 1);

    this->registers_[ST]--;
    return this->data_store_[this->registers_[ST]];
}

bool TamEmulator::Execute(TamInstruction instr) {
    switch (instr.op) {
        case 0:  // LOAD
            this->ExecuteLoad(instr);
            break;
        case 1:  // LOADA
            this->ExecuteLoada(instr);
            break;
        case 2:  // LOADI
            this->ExecuteLoadi(instr);
            break;
        case 3:  // LOADL
            this->ExecuteLoadl(instr);
            break;
        case 4:  // STORE
            this->ExecuteStore(instr);
            break;
        case 5:  // STOREI
            this->ExecuteStorei(instr);
            break;
        case 6:  // CALL
            if (instr.r == PB && instr.d > 0 && instr.d < 29) {
                this->ExecuteCallPrimitive(instr);
            } else {
                this->ExecuteCall(instr);
            }
            break;
        case 7:  // CALLI
            this->ExecuteCalli(instr);
            break;
        case 8:  // RETURN
            this->ExecuteReturn(instr);
            break;
        case 10:  // PUSH
            this->ExecutePush(instr);
            break;
        case 11:  // POP
            this->ExecutePop(instr);
            break;
        case 12:  // JUMP
            this->ExecuteJump(instr);
            break;
        case 13:  // JUMPI
            this->ExecuteJumpi(instr);
            break;
        case 14:  // JUMPIF
            this->ExecuteJumpif(instr);
            break;
        case 15:  // HALT
            return false;
        default:
            throw RuntimeError(ExceptionKind::kUnknownOpcode,
                               this->registers_[CP] - 1);
    }
    return true;
}

const std::string TamEmulator::GetSnapshot() const {
    std::stringstream ss;
    ss << std::hex << std::setfill('0');

    ss << "stack";
    for (int I = 0; I < this->registers_[ST]; ++I) {
        if (I % 8 == 0) ss << std::endl;
        ss << std::setw(4) << this->data_store_[I] << " ";
    }
    ss << std::endl;

    for (auto Block : this->allocated_blocks_) {
        ss << "heap " << std::setw(4) << Block.first;
        for (int I = 0; I < Block.second; ++I) {
            if (I % 8 == 0) {
                ss << std::endl;
            }
            ss << std::setw(4) << this->data_store_[Block.first + I] << " ";
        }
    }

    return ss.str();
}

void TamEmulator::ExecuteLoad(TamInstruction instr) {
    TamAddr base_addr = this->registers_[instr.r] + instr.d;

    for (int I = 0; I < instr.n; ++I) {
        TamAddr addr = base_addr + I;
        if (addr >= this->registers_[ST] && addr <= this->registers_[HT])
            throw RuntimeError(ExceptionKind::kDataAccessViolation,
                               this->registers_[CP] - 1);

        TamData value = this->data_store_[addr];
        this->PushData(value);
    }
}

void TamEmulator::ExecuteLoada(TamInstruction instr) {
    TamAddr addr = this->registers_[instr.r] + instr.d;
    this->PushData(addr);
}

void TamEmulator::ExecuteLoadi(TamInstruction instr) {
    TamAddr base_addr = this->PopData();

    for (int I = 0; I < instr.n; ++I) {
        TamAddr addr = base_addr + I;
        if (addr >= this->registers_[ST] && addr <= this->registers_[HT])
            throw RuntimeError(ExceptionKind::kDataAccessViolation,
                               this->registers_[CP] - 1);

        TamData value = this->data_store_[addr];
        this->PushData(value);
    }
}

void TamEmulator::ExecuteLoadl(TamInstruction instr) {
    this->PushData(instr.d);
}

void TamEmulator::ExecuteStore(TamInstruction instr) {
    std::stack<TamData> Data;
    for (int I = 0; I < instr.n; ++I) Data.push(this->PopData());

    TamAddr base_addr = this->registers_[instr.r] + instr.d;
    for (int I = 0; I < instr.n; ++I) {
        TamAddr addr = base_addr + I;
        if (addr >= this->registers_[ST] && addr <= this->registers_[HT])
            throw RuntimeError(ExceptionKind::kDataAccessViolation,
                               this->registers_[CP] - 1);

        this->data_store_[addr] = Data.top();
        Data.pop();
    }

    assert(Data.empty());
}

void TamEmulator::ExecuteStorei(TamInstruction instr) {
    TamAddr base_addr = this->PopData();

    std::stack<TamData> Data;
    for (int I = 0; I < instr.n; ++I) Data.push(this->PopData());

    for (int I = 0; I < instr.n; ++I) {
        TamAddr addr = base_addr + I;
        if (addr >= this->registers_[ST] && addr <= this->registers_[HT])
            throw RuntimeError(ExceptionKind::kDataAccessViolation,
                               this->registers_[CP] - 1);

        this->data_store_[addr] = Data.top();
        Data.pop();
    }

    assert(Data.empty());
}

void TamEmulator::ExecuteCall(TamInstruction instr) {
    if (this->registers_[instr.r] + instr.d >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    TamAddr static_link = this->registers_[instr.n];
    assert(static_link < this->registers_[ST]);
    TamAddr dynamic_link = this->registers_[LB];
    assert(dynamic_link < this->registers_[ST]);
    TamAddr return_addr = this->registers_[CP];
    assert(return_addr < this->registers_[CT]);

    this->PushData(static_link);
    this->PushData(dynamic_link);
    this->PushData(return_addr);

    this->registers_[LB] = this->registers_[ST] - 3;
    this->registers_[CP] = this->registers_[instr.r] + instr.d;
}

void TamEmulator::ExecuteCalli(TamInstruction instr) {
    TamAddr call_address = this->PopData();
    TamAddr static_link = this->PopData();
    assert(static_link < this->registers_[ST]);

    if (call_address >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    TamAddr dynamic_link = this->registers_[LB];
    assert(dynamic_link < this->registers_[ST]);
    TamAddr return_addr = this->registers_[CP];
    assert(return_addr < this->registers_[CT]);

    this->PushData(static_link);
    this->PushData(dynamic_link);
    this->PushData(return_addr);

    this->registers_[LB] = this->registers_[ST] - 3;
    this->registers_[CP] = call_address;
}

void TamEmulator::ExecuteReturn(TamInstruction instr) {
    std::stack<TamData> return_val;
    for (int I = 0; I < instr.n; ++I) return_val.push(this->PopData());

    assert(return_val.size() == instr.n);

    TamAddr dynamic_link = this->data_store_[this->registers_[LB] + 1];
    TamAddr return_addr = this->data_store_[this->registers_[LB] + 2];
    if (return_addr >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    // pop stack frame
    while (this->registers_[ST] > this->registers_[LB]) this->PopData();
    assert(this->registers_[ST] == this->registers_[LB]);

    // pop arguments
    for (int I = 0; I < instr.d; ++I) this->PopData();
    assert(this->registers_[ST] == this->registers_[LB] - instr.d);

    // push result
    for (int I = 0; I < instr.n; ++I) {
        assert(!return_val.empty());
        this->PushData(return_val.top());
        return_val.pop();
    }
    assert(return_val.empty());

    this->registers_[LB] = dynamic_link;
    assert(this->registers_[LB] == dynamic_link);
    this->registers_[CP] = return_addr;
    assert(this->registers_[CP] == return_addr);
}

void TamEmulator::ExecutePush(TamInstruction instr) {
    if (this->registers_[ST] + instr.d >= this->registers_[HT])
        throw RuntimeError(ExceptionKind::kStackOverflow,
                           this->registers_[CT] - 1);

    this->registers_[ST] += instr.d;
}

void TamEmulator::ExecutePop(TamInstruction instr) {
    std::stack<TamData> data;
    for (int i = 0; i < instr.n; ++i) data.push(this->PopData());

    for (int i = 0; i < instr.d; ++i) this->PopData();

    while (!data.empty()) {
        this->PushData(data.top());
        data.pop();
    }

    assert(data.empty());
}

void TamEmulator::ExecuteJump(TamInstruction instr) {
    TamAddr addr = this->registers_[instr.r] + instr.d;
    if (addr >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    this->registers_[CP] = addr;
    assert(this->registers_[CP] == addr);
}

void TamEmulator::ExecuteJumpi(TamInstruction instr) {
    TamAddr addr = this->PopData();
    if (addr >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    this->registers_[CP] = addr;
    assert(this->registers_[CP] == addr);
}

void TamEmulator::ExecuteJumpif(TamInstruction instr) {
    TamData value = this->PopData();
    if (value != instr.n) return;

    assert(value == instr.n);

    TamAddr addr = this->registers_[instr.r] + instr.d;
    if (addr >= this->registers_[CT])
        throw RuntimeError(ExceptionKind::kCodeAccessViolation,
                           this->registers_[CP] - 1);

    this->registers_[CP] = addr;
    assert(this->registers_[CP] == addr);
}

}  // namespace tam
