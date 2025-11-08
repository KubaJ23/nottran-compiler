#include <stdio.h>

#include "tam/test/integration_test.h"

#include <gtest/gtest.h>

class IoTest : public EmulatorTest {};

TEST_F(IoTest, EolTestTrue) {
    FILE* instream = tmpfile();
    fprintf(instream, "\n456");
    rewind(instream);
    this->setInstream(instream);

    ASSERT_NO_THROW({ this->PrimitiveEol(); });
    ASSERT_EQ(1, this->data_store_[0]);
    char c = getc(instream);
    ASSERT_EQ('\n', c);
}

TEST_F(IoTest, EolTestFalse) {
    FILE* instream = tmpfile();
    fprintf(instream, "4\n56");
    rewind(instream);
    this->setInstream(instream);

    ASSERT_NO_THROW({ this->PrimitiveEol(); });
    ASSERT_EQ(0, this->data_store_[0]);
    char c = getc(instream);
    ASSERT_EQ('4', c);
}

TEST_F(IoTest, EofTestTrue) {
    FILE* instream = tmpfile();
    fprintf(instream, "\n456");
    fseek(instream, SEEK_END, SEEK_CUR);
    getc(instream);
    this->setInstream(instream);

    ASSERT_NO_THROW({ this->PrimitiveEof(); });
    ASSERT_EQ(1, this->data_store_[0]);
}

TEST_F(IoTest, EofTestFalse) {
    FILE* instream = tmpfile();
    fprintf(instream, "\n456");
    rewind(instream);
    this->setInstream(instream);

    ASSERT_NO_THROW({ this->PrimitiveEof(); });
    ASSERT_EQ(0, this->data_store_[0]);
}

TEST_F(IoTest, GetTest) {
    FILE* instream = tmpfile();
    fprintf(instream, "Hello, world");
    rewind(instream);
    this->setInstream(instream);

    DataVec data = {0};
    this->setData(data);

    ASSERT_NO_THROW({ this->PrimitiveGet(); });
    ASSERT_EQ('H', this->data_store_[0]);
    ASSERT_EQ('e', getc(instream));
}

TEST_F(IoTest, PutTest) {
    FILE* outstream = tmpfile();
    this->setOutstream(outstream);

    DataVec data = {'H', 'i'};
    this->setData(data);

    ASSERT_NO_THROW({ this->PrimitivePut(); });
    rewind(outstream);
    char c = getc(outstream);
    ASSERT_EQ('i', c);
}

TEST_F(IoTest, GeteolTest) {
    FILE* instream = tmpfile();
    fprintf(instream, "123 \n456");
    rewind(instream);
    this->setInstream(instream);

    ASSERT_NO_THROW({ this->PrimitiveGeteol(); });
    char c = getc(instream);
    ASSERT_EQ('4', c);
}

TEST_F(IoTest, PuteolTest) {
    FILE* outstream = tmpfile();
    this->setOutstream(outstream);

    ASSERT_NO_THROW({ this->PrimitivePuteol(); });
    rewind(outstream);
    char c = getc(outstream);
    ASSERT_EQ('\n', c);
}

TEST_F(IoTest, GetintTest) {
    FILE* instream = tmpfile();
    fprintf(instream, " 8008\n2");
    rewind(instream);
    this->setInstream(instream);

    DataVec data = {0};
    this->setData(data);

    ASSERT_NO_THROW({ this->PrimitiveGetint(); });
    ASSERT_EQ(8008, this->data_store_[0]);
    ASSERT_EQ('2', getc(instream));
}

TEST_F(IoTest, PutintTest) {
    FILE* outstream = tmpfile();
    this->setOutstream(outstream);

    DataVec data = {123, 456};
    this->setData(data);

    ASSERT_NO_THROW({ this->PrimitivePutint(); });
    rewind(outstream);
    int output;
    fscanf(outstream, "%d", &output);
    ASSERT_EQ(456, output);
}
