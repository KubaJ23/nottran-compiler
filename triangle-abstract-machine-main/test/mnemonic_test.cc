#include "tam/tam.h"
#include "tam/test/integration_test.h"

TEST_F(EmulatorTest, GetMnemonicTest) {
    // CALL special case for primitives
    EXPECT_EQ("CALL put", tam::GetMnemonic({tam::CALL, tam::PB, 0, 22}));

    // CALL regular case
    EXPECT_EQ("CALL(8) 42[CB]", tam::GetMnemonic({tam::CALL, tam::CB, tam::LB, 42}));

    // Another OPCODE(n) d[r] case
    EXPECT_EQ("JUMPIF(0) 9[CB]", tam::GetMnemonic({tam::JUMPIF, tam::CB, 0, 9}));

    // OPCODE d[r]
    EXPECT_EQ("JUMP 9[CB]", tam::GetMnemonic({tam::JUMP, tam::CB, 0, 9}));

    // OPCODE(n) d
    EXPECT_EQ("RETURN(1) 3", tam::GetMnemonic({tam::RETURN, 0, 1, 3}));

    // OPCODE (n)
    EXPECT_EQ("LOADI (4)", tam::GetMnemonic({tam::LOADI, 0, 4, 0}));

    // OPCODE d
    EXPECT_EQ("LOADL 42", tam::GetMnemonic({tam::LOADL, 0, 0, 42}));

    // OPCODE
    EXPECT_EQ("HALT", tam::GetMnemonic({tam::HALT, 0, 0, 0}));
}
