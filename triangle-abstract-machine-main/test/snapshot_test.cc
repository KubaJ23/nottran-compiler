#include "tam/tam.h"
#include "tam/test/integration_test.h"

TEST_F(EmulatorTest, GetSnapshotTest) {
    DataVec data = {123, 456, 789, 11112, 11415};
    this->setData(data);

    tam::TamAddr addr = this->Allocate(3);
    this->data_store_[addr] = 246;
    this->data_store_[addr + 1] = 8112;
    this->data_store_[addr + 2] = 1416;

    std::string snapshot = this->GetSnapshot();
    std::string expected_snapshot =
        "Stack\n007b 01c8 0315 2b68 2c97 \nHeap fffd\n00f6 1fb0 0588 ";
    EXPECT_EQ(expected_snapshot, snapshot);
}
