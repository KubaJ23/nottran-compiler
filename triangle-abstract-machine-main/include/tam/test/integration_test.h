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
/// @file integration_test.h
/// This file defines the GoogleTest test fixture for testing the internal
/// state of a `TamEmulator`.
//
//===-----------------------------------------------------------------------===//_

#ifndef TAM_TEST_INTEGRATION_TEST_H__
#define TAM_TEST_INTEGRATION_TEST_H__

#include <assert.h>

#include <vector>

#include "tam/tam.h"

#include <gtest/gtest.h>

typedef std::vector<tam::TamCode>
    CodeVec;  ///< Type alias for a vector of code words
typedef std::vector<tam::TamData>
    DataVec;  ///< Type alias for a vector of data words

class EmulatorTest : public testing::Test, public tam::TamEmulator {
   protected:
    /// Populate the emulator's code memory with the provided code and
    /// set the `CT`, `PB`, and `PT` registers.
    ///
    /// @param code words of code memory to set
    void setCode(CodeVec& code) {
        assert(code.size() < 65536);
        this->code_store_.fill(0);

        std::copy(code.begin(), code.end(), this->code_store_.begin());
        for (int I = 0; I < code.size(); ++I) {
            assert(code[I] == this->code_store_[I]);
        }

        this->registers_[tam::CT] = code.size();
        this->registers_[tam::PB] = this->registers_[tam::CT];
        this->registers_[tam::PT] = this->registers_[tam::PB] + 29;
        assert(this->registers_[tam::CT] == code.size());
    }

    /// Populate the emulator's data memory with the provided data and set
    /// the `ST` register.
    ///
    /// @param data words of data memory to set
    void setData(DataVec& data) {
        assert(data.size() < 65536);
        this->data_store_.fill(0);

        std::copy(data.begin(), data.end(), this->data_store_.begin());
        for (int I = 0; I < data.size(); ++I) {
            assert(data[I] == this->data_store_[I]);
        }

        this->registers_[tam::ST] = data.size();
        assert(this->registers_[tam::ST] == data.size());
    }

    /// Set the emulator's input stream.
    ///
    /// @param instream
    void setInstream(FILE* instream) {
        assert(instream);
        this->instream_ = instream;
    }

    /// Set the emulator's output stream.
    ///
    /// @param outstream
    void setOutstream(FILE* outstream) {
        assert(outstream);
        this->outstream_ = outstream;
    }
};

#endif  // TAM_TEST_INTEGRATION_TEST_H__
