//===-----------------------------------------------------------------------===//
//
// This file is part of tam-cpp, copyright (c) Ian Knight 2025.
//
// tam-cpp is free software: you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the Free Software
// Foundation, either version 3 of the License, or (at your option) any later
// version.
//
// tam-cpp is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with tam-cpp. If not, see <https://www.gnu.org/licenses/>.
//
//===-----------------------------------------------------------------------===//
//
/// @file main.cc
/// This file defines the entry point of the program, along with some auxiliary
/// functions. All file IO is handled in this file.
//
//===-----------------------------------------------------------------------===//

#include <stdint.h>

#include <exception>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "tam/cli.h"
#include "tam/error.h"
#include "tam/tam.h"

/// Load a TAM program from a file.
///
/// This function does not verify that the bytes read from the file form valid
/// TAM bytecode.
///
/// @param filename name of file to read from
/// @return a vector of 32-bit code words
static std::vector<uint32_t> ReadProgramFromFile(const std::string &filename) {
    std::ifstream in_stream(filename, std::ios::binary);

    // find file size
    in_stream.seekg(0, in_stream.end);
    int file_len = in_stream.tellg();
    in_stream.seekg(0, in_stream.beg);

    if (file_len % 4 != 0)
        throw tam::IoError("program file contained incomplete instruction");

    // read instructions
    std::vector<uint32_t> codes;
    for (int j = 0; j < file_len / 4; ++j) {
        int c;
        uint32_t code = 0;
        for (int i = 0; i < 4; ++i) {
            c = in_stream.get();
            code = (code << 8) | c;
        }
        codes.push_back(code);
    }
    return codes;
}

static void PrintHelpMessage() {
    std::cout << "Usage: tam [OPTIONS] FILENAME" << std::endl
              << std::endl
              << "Options:" << std::endl
              << "  -t,--trace        print the stack and allocated heap "
                 "after each instruction"
              << std::endl
              << "  -s,--step         press RETURN to advance after each "
                 "instruction"
              << std::endl
              << "                    (only if trace is also given)"
              << std::endl
              << "  -h,--help         print this help message" << std::endl;
}

static bool CpuCycle(tam::TamEmulator &emulator, bool trace, bool step) {
    const tam::TamInstruction Instr = emulator.FetchDecode();
    bool running = emulator.Execute(Instr);

    if (trace) std::cout << emulator.GetSnapshot() << std::endl;

    if (trace && step) {
        std::string buf;
        std::getline(std::cin, buf);
    }

    return running;
}

int main(int argc, const char **argv) {
    std::unique_ptr<CliArgs> args = ParseCli(argc - 1, argv + 1);
    if (!args) {
        PrintHelpMessage();
        return 1;
    }

    if (args->help) {
        PrintHelpMessage();
        return 0;
    }

    if (!std::filesystem::is_regular_file(args->filename)) {
        std::cerr << "error: io error: file '" << args->filename
                  << "' not found" << std::endl;
        return 1;
    }

    tam::TamEmulator emulator;
    try {
        std::vector<uint32_t> program = ReadProgramFromFile(args->filename);
        emulator.LoadProgram(program);
    } catch (const std::exception &e) {
        std::cerr << e.what() << std::endl;
        return 2;
    }

    bool running = true;
    do {
        try {
            running = CpuCycle(emulator, args->trace, args->step);
        } catch (const std::exception &e) {
            std::cerr << e.what() << std::endl;
            return 3;
        }
    } while (running);
    return 0;
}
