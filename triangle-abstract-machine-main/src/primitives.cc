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
/// @file primitives.cc
/// This file defines all the methods for executing primitive operations
/// in `TamEmulator`.
//
//===-----------------------------------------------------------------------===//

#include <assert.h>

#include <cstdint>
#include <cstdio>
#include <stack>

#include "tam/error.h"
#include "tam/tam.h"

namespace tam {

void TamEmulator::ExecuteCallPrimitive(TamInstruction instr) {
    assert(instr.d > 0 && instr.d < 29);
    switch (instr.d) {
        case 1:
            break;
        case 2:
            this->PrimitiveNot();
            break;
        case 3:
            this->PrimitiveAnd();
            break;
        case 4:
            this->PrimitiveOr();
            break;
        case 5:
            this->PrimitiveSucc();
            break;
        case 6:
            this->PrimitivePred();
            break;
        case 7:
            this->PrimitiveNeg();
            break;
        case 8:
            this->PrimitiveAdd();
            break;
        case 9:
            this->PrimitiveSub();
            break;
        case 10:
            this->PrimitiveMult();
            break;
        case 11:
            this->PrimitiveDiv();
            break;
        case 12:
            this->PrimitiveMod();
            break;
        case 13:
            this->PrimitiveLt();
            break;
        case 14:
            this->PrimitiveLe();
            break;
        case 15:
            this->PrimitiveGe();
            break;
        case 16:
            this->PrimitiveGt();
            break;
        case 17:
            this->PrimitiveEq();
            break;
        case 18:
            this->PrimitiveNe();
            break;
        case 19:
            this->PrimitiveEol();
            break;
        case 20:
            this->PrimitiveEof();
            break;
        case 21:
            this->PrimitiveGet();
            break;
        case 22:
            this->PrimitivePut();
            break;
        case 23:
            this->PrimitiveGeteol();
            break;
        case 24:
            this->PrimitivePuteol();
            break;
        case 25:
            this->PrimitiveGetint();
            break;
        case 26:
            this->PrimitivePutint();
            break;
        case 27:
            this->PrimitiveNew();
            break;
        case 28:
            this->PrimitiveDispose();
            break;
    }
}

void TamEmulator::PrimitiveNot() {
    TamData value = this->PopData();
    this->PushData(value ? 0 : 1);
}

void TamEmulator::PrimitiveAnd() {
    TamData op2 = this->PopData(), op1 = this->PopData();
    this->PushData(op1 * op2 ? 1 : 0);
}

void TamEmulator::PrimitiveOr() {
    TamData op2 = this->PopData(), op1 = this->PopData();
    this->PushData(op1 + op2 || op1 == -op2 ? 1 : 0);
}

void TamEmulator::PrimitiveSucc() {
    TamData op = this->PopData();
    this->PushData(op + 1);
}

void TamEmulator::PrimitivePred() {
    TamData op = this->PopData();
    this->PushData(op - 1);
}

void TamEmulator::PrimitiveNeg() {
    TamData op = this->PopData();
    this->PushData(-op);
}

void TamEmulator::PrimitiveAdd() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 + arg2);
}

void TamEmulator::PrimitiveSub() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 - arg2);
}
void TamEmulator::PrimitiveMult() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 * arg2);
}

void TamEmulator::PrimitiveDiv() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 / arg2);
}

void TamEmulator::PrimitiveMod() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 % arg2);
}

void TamEmulator::PrimitiveLt() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 < arg2 ? 1 : 0);
}

void TamEmulator::PrimitiveLe() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 <= arg2 ? 1 : 0);
}

void TamEmulator::PrimitiveGe() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 >= arg2 ? 1 : 0);
}

void TamEmulator::PrimitiveGt() {
    TamData arg2 = this->PopData(), arg1 = this->PopData();
    this->PushData(arg1 > arg2 ? 1 : 0);
}

void TamEmulator::PrimitiveEq() {
    TamData width = this->PopData();
    std::stack<TamData> arg1, arg2;

    for (int i = 0; i < width; ++i) arg2.push(this->PopData());
    for (int i = 0; i < width; ++i) arg1.push(this->PopData());

    assert(arg1.size() == arg2.size());
    this->PushData(arg1 == arg2 ? 1 : 0);
}

void TamEmulator::PrimitiveNe() {
    TamData width = this->PopData();
    std::stack<TamData> arg1, arg2;

    for (int i = 0; i < width; ++i) arg2.push(this->PopData());
    for (int i = 0; i < width; ++i) arg1.push(this->PopData());

    assert(arg1.size() == arg2.size());
    this->PushData(arg1 != arg2 ? 1 : 0);
}

static void CheckStream(FILE* stream) {
    if (!stream || ferror(stream)) throw IoError("failed to get stream for IO");
}

void TamEmulator::PrimitiveEol() {
    CheckStream(this->instream_);

    char c = fgetc(this->instream_);
    this->PushData(c == '\n' ? 1 : 0);
    ungetc(c, this->instream_);
}

void TamEmulator::PrimitiveEof() {
    CheckStream(this->instream_);

    this->PushData(feof(this->instream_) ? 1 : 0);
}

void TamEmulator::PrimitiveGet() {
    CheckStream(this->instream_);

    TamAddr addr = this->PopData();
    char c = getc(this->instream_);
    this->data_store_[addr] = c;
}

void TamEmulator::PrimitivePut() {
    CheckStream(this->outstream_);

    char c = this->PopData();
    putc(c, this->outstream_);
}

void TamEmulator::PrimitiveGeteol() {
    CheckStream(this->instream_);

    char c;
    while ((c = fgetc(this->instream_)) != '\n');
}

void TamEmulator::PrimitivePuteol() {
    CheckStream(this->outstream_);

    putc('\n', this->outstream_);
}

void TamEmulator::PrimitiveGetint() {
    CheckStream(this->instream_);

    int n;
    fscanf(this->instream_, "%d", &n);
    if (n < INT16_MIN || n > INT16_MAX) {
        throw IoError("integer out of range");
    }
    this->PrimitiveGeteol();  // flush line

    TamAddr addr = this->PopData();
    this->data_store_[addr] = n;
}

void TamEmulator::PrimitivePutint() {
    CheckStream(this->outstream_);

    TamData n = this->PopData();
    fprintf(this->outstream_, "%d", n);
}

void TamEmulator::PrimitiveNew() {
    TamData n = this->PopData();
    TamAddr addr = this->Allocate(n);
    this->PushData(addr);
}

void TamEmulator::PrimitiveDispose() {
    TamAddr addr = this->PopData();
    TamData size = this->PopData();
    this->Free(addr, size);
}

}  // namespace tam
