#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include <gtest/gtest.h>

TEST_F(EmulatorTest, TestLoadOk) {
    std::vector<tam::TamData> data = {1, 2, 3, 4, 5};
    this->setData(data);

    tam::TamInstruction instr = {0, tam::SB, 2, 0};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(7, this->registers_[tam::ST]);
    EXPECT_EQ(1, this->data_store_[5]);
    EXPECT_EQ(2, this->data_store_[6]);
}

TEST_F(EmulatorTest, TestLoadaOk) {
    std::vector<tam::TamData> data = {1, 2, 3};
    this->setData(data);

    tam::TamInstruction instr = {1, tam::SB, 0, 1};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(4, this->registers_[tam::ST]);
    EXPECT_EQ(1, this->data_store_[3]);
}

TEST_F(EmulatorTest, TestLoadiOk) {
    std::vector<tam::TamData> data = {4, 3, 2, 1, 1};
    this->setData(data);

    tam::TamInstruction instr = {2, tam::SB, 2, 0};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(6, this->registers_[tam::ST]);
    EXPECT_EQ(3, this->data_store_[4]);
    EXPECT_EQ(2, this->data_store_[5]);
}

TEST_F(EmulatorTest, TestLoadlOk) {
    std::vector<tam::TamData> data = {1, 2, 3};
    this->setData(data);

    tam::TamInstruction instr = {3, tam::SB, 0, 10};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(4, this->registers_[tam::ST]);
    EXPECT_EQ(10, this->data_store_[3]);
}

TEST_F(EmulatorTest, TestStoreOk) {
    std::vector<tam::TamData> data = {1, 2, 3, 4, 5, 10, 20};
    this->setData(data);

    tam::TamInstruction instr = {4, tam::SB, 2, 1};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(5, this->registers_[tam::ST]);
    EXPECT_EQ(10, this->data_store_[1]);
    EXPECT_EQ(20, this->data_store_[2]);
}

TEST_F(EmulatorTest, TestStoreiOk) {
    std::vector<tam::TamData> data = {1, 2, 3, 4, 5, 10, 20, 1};
    this->setData(data);

    tam::TamInstruction instr = {5, tam::SB, 2, 1};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(5, this->registers_[tam::ST]);
    EXPECT_EQ(10, this->data_store_[1]);
    EXPECT_EQ(20, this->data_store_[2]);
}

TEST_F(EmulatorTest, TestCallUserDefinedOk) {
    DataVec data = {1, 2, 3, 4, 5};
    this->setData(data);
    CodeVec code = {1, 2, 3};
    this->setCode(code);
    this->registers_[tam::CP] = 1;
    this->registers_[tam::SB] = 1;

    tam::TamInstruction instr = {6, 0, 4, 2};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(1, this->data_store_[5]);
    EXPECT_EQ(0, this->data_store_[6]);
    EXPECT_EQ(1, this->data_store_[7]);
    EXPECT_EQ(8, this->registers_[tam::ST]);
    EXPECT_EQ(2, this->registers_[tam::CP]);
    EXPECT_EQ(5, this->registers_[tam::LB]);
}

TEST_F(EmulatorTest, TestReturnOk) {
    DataVec data = {1,
                    2,
                    3,
                    5 /*arg1*/,
                    5 /*arg2*/,
                    0 /*static*/,
                    1 /*dynamic*/,
                    2 /*ret addr*/,
                    44,
                    88,
                    42};
    this->setData(data);
    CodeVec code = {10, 20, 30, 40, 50};
    this->setCode(code);
    this->registers_[tam::LB] = 5;

    tam::TamInstruction instr = {8, 0, 1, 2};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(42, this->data_store_[3]);
    EXPECT_EQ(4, this->registers_[tam::ST]);
    EXPECT_EQ(1, this->registers_[tam::LB]);
    EXPECT_EQ(2, this->registers_[tam::CP]);
}

TEST_F(EmulatorTest, TestPushOk) {
    tam::TamInstruction instr = {10, 0, 0, 3};
    ASSERT_NO_THROW({ this->Execute(instr); });
    EXPECT_EQ(3, this->registers_[tam::ST]) << "ST not incremented";
}

TEST_F(EmulatorTest, TestPopOk) {
    DataVec data = {10, 20, 30, 40, 50};
    this->setData(data);

    tam::TamInstruction instr = {11, 0, 2, 3};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(2, this->registers_[tam::ST]) << "Stack not correct size";
    EXPECT_EQ(40, this->data_store_[0]);
    EXPECT_EQ(50, this->data_store_[1]);
}

TEST_F(EmulatorTest, TestJumpOk) {
    this->registers_[tam::CT] = 20;
    this->registers_[tam::CP] = 10;

    tam::TamInstruction instr = {12, tam::SB, 0, 5};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(5, this->registers_[tam::CP]);
}

TEST_F(EmulatorTest, TestJumpiOk) {
    DataVec data = {10, 20, 30, 1};
    this->setData(data);
    this->registers_[tam::CT] = 20;

    tam::TamInstruction instr = {13, 0, 0, 0};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(3, this->registers_[tam::ST]);
    EXPECT_EQ(1, this->registers_[tam::CP]);
}

TEST_F(EmulatorTest, TestJumpifCmpPassOk) {
    DataVec data = {10, 20, 30, 2};
    this->setData(data);
    this->registers_[tam::CT] = 20;

    tam::TamInstruction instr = {14, tam::SB, 2, 4};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(3, this->registers_[tam::ST]);
    EXPECT_EQ(4, this->registers_[tam::CP]);
}

TEST_F(EmulatorTest, TestJumpifCmpFailOk) {
    DataVec data = {10, 20, 30, 7};
    this->setData(data);
    this->registers_[tam::CT] = 20;

    tam::TamInstruction instr = {14, tam::SB, 2, 4};
    ASSERT_NO_THROW({ this->Execute(instr); });

    EXPECT_EQ(3, this->registers_[tam::ST]);
    EXPECT_EQ(0, this->registers_[tam::CP]);
}
