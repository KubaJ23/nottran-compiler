add_executable(tam_exe main.cc cli.cc)
target_include_directories(tam_exe PRIVATE ${CMAKE_SOURCE_DIR}/include)
target_link_libraries(tam_exe tam)
set_property(TARGET tam_exe PROPERTY OUTPUT_NAME tam)
