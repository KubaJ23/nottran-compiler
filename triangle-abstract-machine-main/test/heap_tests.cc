#include <map>

#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include <gtest/gtest.h>

class HeapTest : public EmulatorTest {};

TEST_F(HeapTest, HeapAllocateExpandHeap) {
    ASSERT_NO_THROW({ this->Allocate(3); });
    EXPECT_EQ(65532, this->registers_[tam::HT]);
    EXPECT_TRUE(this->allocated_blocks_.count(this->registers_[tam::HT] + 1));
    EXPECT_EQ(3, this->allocated_blocks_[65533]);
}

TEST_F(HeapTest, HeapAllocateReuseBlock) {
    this->registers_[tam::HT] = 65530;
    this->allocated_blocks_[65531] = 2;
    this->free_blocks_[65533] = 3;

    ASSERT_NO_THROW({ this->Allocate(2); });
    EXPECT_TRUE(this->allocated_blocks_.count(65533))
        << "Block not allocated successfully";
    EXPECT_EQ(2, this->allocated_blocks_[65533])
        << "Allocated block has wrong size";
    EXPECT_FALSE(this->free_blocks_.count(65533))
        << "Free block not marked as used";
    EXPECT_TRUE(this->free_blocks_.count(65535))
        << "Leftover heap not marked as free";
    EXPECT_EQ(1, this->free_blocks_[65535])
        << "Unallocated block has wrong size";
}

TEST_F(HeapTest, FreeEndOfHeap) {
    this->registers_[tam::HT] = 65533;
    this->allocated_blocks_[65534] = 2;

    ASSERT_NO_THROW({ this->Free(65534, 2); });
    EXPECT_FALSE(this->allocated_blocks_.count(65534));
    EXPECT_EQ(65535, this->registers_[tam::HT]);
}

TEST_F(HeapTest, FreeMiddleOfHeap) {
    this->registers_[tam::HT] = 65530;
    this->allocated_blocks_[65531] = 2;
    this->allocated_blocks_[65533] = 3;

    ASSERT_NO_THROW({ this->Free(65533, 3); });
    EXPECT_EQ(65530, this->registers_[tam::HT]) << "HT unexpectedly changed";
    EXPECT_FALSE(this->allocated_blocks_.count(65533));
    EXPECT_TRUE(this->free_blocks_.count(65533));
    EXPECT_EQ(3, this->free_blocks_[65533]);
}
