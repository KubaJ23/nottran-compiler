# Triangle Abstract Machine

![GitHub Release](https://img.shields.io/github/v/release/pszik/triangle-abstract-machine)
![GitHub branch status](https://img.shields.io/github/checks-status/pszik/triangle-abstract-machine/main)
![GitHub License](https://img.shields.io/github/license/pszik/triangle-abstract-machine)
![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/pszik/triangle-abstract-machine/latest/total)

This is an emulator for the Triangle Abstract Machine (TAM) defined by Watt and Brown.

It is invoked from the command line using the command `tam`. It expects a single
argument, which is the name of the binary file to load and run:

```
Usage: tam [OPTIONS] FILENAME

Options:
  -t,--trace        print the stack and allocated heap after each instruction
  -s,--step         press RETURN to advance after each instruction
                    (only if trace is also given)
  -h,--help         print this help message
```

## Expected behaviour

TAM data memory uses 16-bit words and all operations will overflow or underflow
accordingly. In particular, adding or multiplying positive numbers will overflow
into the negative and _vice versa_.

An unexpected case is that negating -32768 (the smallest signed 16-bit number)
will still result in -32768.

## Building TAM

TAM is a CMake project. To build it yourself, clone the project and then
run the following commands:

```shell
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build .
```

After this, the executable is found in `build/app/tam` on Unix, or `build\app\Release\tam`
on Windows if you build using Visual C++.
