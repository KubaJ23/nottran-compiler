#include <memory>

#include "tam/cli.h"

#include <gtest/gtest.h>

TEST(CliTests, ParseHelp) {
    const char* argv[] = {"-h"};
    std::unique_ptr<CliArgs> args = ParseCli(1, argv);
    ASSERT_NE(nullptr, args);
    ASSERT_TRUE(args->help);
}

TEST(CliTests, ParseFnameOk) {
    const char* argv[] = {"test.tam"};
    std::unique_ptr<CliArgs> args = ParseCli(1, argv);
    ASSERT_NE(nullptr, args);
    ASSERT_FALSE(args->help || args->trace || args->step);
    ASSERT_EQ("test.tam", args->filename);
}

TEST(CliTests, ParseTraceWithFnameOk) {
    const char* argv[] = {"-t", "test.tam"};
    std::unique_ptr<CliArgs> args = ParseCli(2, argv);
    ASSERT_NE(nullptr, args);
    ASSERT_FALSE(args->help || args->step);
    ASSERT_TRUE(args->trace);
    ASSERT_EQ("test.tam", args->filename);
}

TEST(CliTests, ParseTraceNoFilenameFail) {
    const char* argv[] = {"-t"};
    std::unique_ptr<CliArgs> args = ParseCli(1, argv);
    ASSERT_EQ(nullptr, args);
}

TEST(CliTests, ParseTraceStepOk) {
    const char* argv[] = {"-t", "-s", "test.tam"};
    std::unique_ptr<CliArgs> args = ParseCli(3, argv);
    ASSERT_NE(nullptr, args);
    ASSERT_FALSE(args->help);
    ASSERT_TRUE(args->trace);
    ASSERT_TRUE(args->step);
    ASSERT_EQ("test.tam", args->filename);
}
