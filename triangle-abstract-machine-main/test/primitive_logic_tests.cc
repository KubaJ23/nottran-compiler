#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include "gtest/gtest.h"
#include "rapidcheck/gtest.h"

class PrimitiveLogicTests : public EmulatorTest {};

RC_GTEST_FIXTURE_PROP(PrimitiveLogicTests, TestAnd,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveAnd());

    ASSERT_EQ(l && r, this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveLogicTests, TestOr,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveOr());

    ASSERT_EQ(l || r, this->data_store_[0]);
}

TEST_F(PrimitiveLogicTests, TestNotTrue) {
    this->PushData(1);

    ASSERT_NO_THROW(this->PrimitiveNot());

    int result = this->data_store_[0];
    ASSERT_EQ(0, result);
}

TEST_F(PrimitiveLogicTests, TestNotFalse) {
    this->PushData(0);

    ASSERT_NO_THROW(this->PrimitiveNot());

    int result = this->data_store_[0];
    ASSERT_EQ(1, result);
}
