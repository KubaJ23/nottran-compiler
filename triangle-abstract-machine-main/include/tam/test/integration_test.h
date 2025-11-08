#ifndef INTEGRATION_TEST_H__
#define INTEGRATION_TEST_H__

#include <assert.h>

#include <vector>

#include "tam/tam.h"

#include <gtest/gtest.h>

typedef std::vector<tam::TamCode> CodeVec;
typedef std::vector<tam::TamData> DataVec;

class EmulatorTest : public testing::Test, public tam::TamEmulator {
   protected:
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

    void setInstream(FILE* instream) {
        assert(instream);
        this->instream_ = instream;
    }

    void setOutstream(FILE* outstream) {
        assert(outstream);
        this->outstream_ = outstream;
    }
};

#endif  // INTEGRATION_TEST_H__
