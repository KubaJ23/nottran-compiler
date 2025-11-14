# Nottran Language Compiler

This project contains a compiler program for a language that is based on Fortran 90 called Nottran.

## How to run

This is an Intellij project, so load this project into Intellij. There should exist run configurations for this project (top right).
There are multiple run configurations, each will compile one of the files in the src/test/examples directory.
For example the `Compiler test suite file` configuration compiles the `test-suite.n25` file. The output is saved in the `examples-output` directory as `output.tam`.
To run the output, you must run it using the Triangle abstract machine (TAM). The instructions on how to build and run it on your machine can be found [here](triangle-abstract-machine-main/README.md).
This project contains the files for TAM in the directory `triangle-abstract-machine-main`. Once built, you can run the powershell scripts to run the `output.tam` [with trace](run-nottran-ts.ps1) and [without trace](run-nottran.ps1).

## Example Nottran programs

- [Test suite program for testing all Nottran features](src/test/examples/test-suite.n25)
- [Test suite program for testing more challenging features and combinations](src/test/examples/test-suite-advanced.n25)
- [Matrix Multiplication program](src/test/examples/matrix-multiplication.n25)
- [A workspace file for quick and easy testing](src/test/examples/sandbox.n25)

### Notes:
- Only single dimensional dynamic arrays are valid (no multidimensional dynamic arrays).
- A read statement into a character sequence/string will only read one single character into the variable. You can use the concat operator ('//') to append together the inputs to get a full inputted string.
- Can only use the indexing operator ('arr(i, j)') and the field access operator ('%') only on lvalues (variables with a name) so cannot do 'x = func(arg) % field1'.
- No way to change the value of a pointer besides the allocate statement.
- Pointers are always dereferenced when used except when they are used in allocate statements, deallocate statements, and as arguments to functions/procedures.
- Write statement only writes intrinsic types, so you (Nottran programmer) must implement print statements for your arrays and derived types (like in most languages).
- Character sequences/strings uses: assignment, concatenation operator, reading user input (one char at a time), writing to output.