#include <stdio.h>

#include <cstdio>
#include <vector>

#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include <gtest/gtest.h>

using namespace tam;

TEST_F(EmulatorTest, TestLoadProgram) {
    std::vector<tam::TamCode> code{0x12345678, 0x9abcdef0, 0xfedcba98};

    ASSERT_NO_THROW({ this->LoadProgram(code); });

    EXPECT_EQ(0x12345678, this->code_store_[0]);
    EXPECT_EQ(0x9abcdef0, this->code_store_[1]);
    EXPECT_EQ(0xfedcba98, this->code_store_[2]);
    EXPECT_EQ(3, this->registers_[tam::CT]);
    EXPECT_EQ(3, this->registers_[tam::PB]);
    EXPECT_EQ(32, this->registers_[tam::PT]);
}

TEST_F(EmulatorTest, TestSimpleCycle) {
    this->code_store_[0] = 0x08020000;
    this->data_store_[0] = 0x1234;
    this->data_store_[1] = 0x5678;
    this->data_store_[2] = 0x9abc;

    this->registers_[tam::CT] = 1;
    this->registers_[tam::CP] = 0;
    this->registers_[tam::ST] = 3;

    ASSERT_NO_THROW({
        tam::TamInstruction instr = this->FetchDecode();
        this->Execute(instr);
    });

    EXPECT_EQ(0x1234, this->data_store_[3]);
    EXPECT_EQ(0x5678, this->data_store_[4]);
}

TEST_F(EmulatorTest, TestSimpleProgram) {
    // LOADL 88, CALL put, HALT
    CodeVec code{0x3e000058, 0x62000016, 0xf0000000};

    ASSERT_NO_THROW({ this->LoadProgram(code); });

    FILE* outstream = tmpfile();
    this->setOutstream(outstream);

    EXPECT_EQ(0, this->registers_[CP]);

    ASSERT_NO_THROW({
        tam::TamInstruction instr = this->FetchDecode();
        EXPECT_EQ(true, this->Execute(instr));
    });

    EXPECT_EQ(1, this->registers_[CP]);

    ASSERT_NO_THROW({
        tam::TamInstruction instr = this->FetchDecode();
        EXPECT_EQ(true, this->Execute(instr));
    });

    EXPECT_EQ(2, this->registers_[CP]);

    ASSERT_NO_THROW({
        tam::TamInstruction instr = this->FetchDecode();
        EXPECT_EQ(false, this->Execute(instr));
    });

    rewind(outstream);
    char c = getc(outstream);
    ASSERT_EQ(88, c);
}
