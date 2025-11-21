#include <stdexcept>

#include "tam/tam.h"
#include "tam/test/integration_test.h"

#include "gtest/gtest.h"
#include "rapidcheck/Assertions.h"
#include "rapidcheck/gtest.h"

class PrimitiveArithTests : public EmulatorTest {};

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestSucc, (tam::TamData n)) {
    this->PushData(n);

    ASSERT_NO_THROW(this->PrimitiveSucc());

    RC_ASSERT(n + 1 == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestPred, (tam::TamData n)) {
    this->PushData(n);

    ASSERT_NO_THROW(this->PrimitivePred());

    RC_ASSERT(n - 1 == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestNeg, (tam::TamData n)) {
    this->PushData(n);

    ASSERT_NO_THROW(this->PrimitiveNeg());

    RC_ASSERT(-n == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestAdd,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveAdd());

    RC_ASSERT((tam::TamData)(l + r) == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestSub,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveSub());

    RC_ASSERT((tam::TamData)(l - r) == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestMult,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    ASSERT_NO_THROW(this->PrimitiveMult());

    RC_ASSERT((tam::TamData)(l * r) == this->data_store_[0]);
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestDiv,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    if (r == 0) {
        ASSERT_THROW({ this->PrimitiveDiv(); }, std::runtime_error);
    } else {
        ASSERT_NO_THROW(this->PrimitiveDiv());
        RC_ASSERT((tam::TamData)(l / r) == this->data_store_[0]);
    }
}

RC_GTEST_FIXTURE_PROP(PrimitiveArithTests, TestMod,
                      (tam::TamData l, tam::TamData r)) {
    this->PushData(l);
    this->PushData(r);

    if (r == 0) {
        ASSERT_THROW({ this->PrimitiveMod(); }, std::runtime_error);
    } else {
        ASSERT_NO_THROW(this->PrimitiveMod());
        RC_ASSERT(l % r == this->data_store_[0]);
    }
}
