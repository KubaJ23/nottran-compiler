cmake_minimum_required(VERSION 3.28)
project(tam VERSION 1.0.0 
            DESCRIPTION "Triangle Abstract Machine implementation"
            LANGUAGES CXX)

set(CMAKE_C_STANDARD 17)
set(CMAKE_C_STANDARD_REQUIRED Yes)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED Yes)
set(CMAKE_EXPORT_COMPILE_COMMANDS Yes)
cmake_policy(SET CMP0135 NEW)

add_subdirectory(src)
add_subdirectory(app)
add_subdirectory(test)
add_subdirectory(docs)

