# Triangle Abstract Machine

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

## Building TAM

TAM is a CMake project. To build it yourself, clone the project and then
run the following commands:

```shell
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build .
```

After this, the executable is found in `build/src/tam`
