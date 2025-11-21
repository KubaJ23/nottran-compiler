set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
set(RC_ENABLE_GTEST Yes)

include(FetchContent)

FetchContent_Declare(
  googletest
  GIT_REPOSITORY https://github.com/google/googletest.git
  GIT_TAG v1.17.0
)

FetchContent_Declare(
  rapidcheck 
  GIT_REPOSITORY https://github.com/emil-e/rapidcheck.git 
)

FetchContent_MakeAvailable(googletest rapidcheck)

enable_testing()

add_executable(tests 
  integration_tests.cc 
  execute_tests.cc 
  snapshot_test.cc
  mnemonic_test.cc
  primitive_heap_tests.cc 
  primitive_io_tests.cc
  primitive_logic_tests.cc
  primitive_arithmetic_tests.cc
  primitive_compare_tests.cc
  cli_tests.cc
  ${CMAKE_SOURCE_DIR}/app/cli.cc
)

target_include_directories(tests PRIVATE ${CMAKE_SOURCE_DIR}/include)
target_link_libraries(tests tam GTest::gtest_main rapidcheck rapidcheck_gtest)

include(GoogleTest)
gtest_discover_tests(tests)
