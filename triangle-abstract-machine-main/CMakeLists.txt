cmake_minimum_required(VERSION 3.20)
project(tam VERSION 0.1.0)

set(CMAKE_C_STANDARD 20)
set(CMAKE_C_STANDARD_REQUIRED Yes)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED Yes)
set(CMAKE_EXPORT_COMPILE_COMMANDS Yes)
cmake_policy(SET CMP0135 NEW)

add_subdirectory(src)
add_subdirectory(test)
add_subdirectory(docs)

