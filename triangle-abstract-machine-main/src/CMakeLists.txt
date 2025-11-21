add_library(tam STATIC tam.cc primitives.cc error.cc heap.cc)
target_include_directories(tam PUBLIC ${CMAKE_SOURCE_DIR}/include)

