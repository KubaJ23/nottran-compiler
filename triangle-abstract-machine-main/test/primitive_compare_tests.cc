#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include "gtest/gtest.h"
#include "rapidcheck/Assertions.h"
#include "rapidcheck/gtest.h"

class PrimitiveCompareTests : public EmulatorTest {};

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestLt,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveLt());

    RC_ASSERT(l < r == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestLe,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveLe());

    RC_ASSERT(l <= r == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestGe,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveGe());

    RC_ASSERT(l >= r == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestGt,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveGt());

    RC_ASSERT(l > r == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestEqTrue,
                      (std::vector<tam::TamData> data)) {
    for (auto n : data) this->PushData(n);
    for (auto n : data) this->PushData(n);
    this->PushData(data.size());

    ASSERT_NO_THROW(this->PrimitiveEq());

    RC_ASSERT(this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveCompareTests, TestEqFalse,
                      (std::vector<tam::TamData> data1,
                       std::vector<tam::TamData> data2)) {
    int len = std::min(data1.size(), data2.size());
    RC_PRE(len != 0);

    data1.resize(len);
    data2.resize(len);
    RC_PRE(data1 != data2);

    for (auto n : data1) this->PushData(n);
    for (auto n : data2) this->PushData(n);
    this->PushData(std::min(data1.size(), data2.size()));

    ASSERT_NO_THROW(this->PrimitiveEq());

    RC_ASSERT_FALSE(this->data_store_[0]);
}
