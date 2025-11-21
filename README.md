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
- [Test suite program for testing IO features and combinations](src/test/examples/test-suite-IO.n25)
- [Matrix Multiplication program](src/test/examples/matrix-multiplication.n25)
- [A workspace file for quick and easy testing](src/test/examples/sandbox.n25)

### Notes:
- Can only use the indexing operator ('arr(i, j)') and the field access operator ('%') only on lvalues (variables with a name) so cannot do 'x = func(arg) % field1'.
- No way to change the value of a pointer besides the allocate statement.
- Pointers are always dereferenced when used except when they are used in allocate statements, deallocate statements, and as arguments to functions/procedures.
- Printing a derived type or array will print their address in memory.
- The TAM machine (triangle-abstract-machine-main) used to run the Nottran program (output.tam) has a bug with the '.or.' operator. For example '(.false. .or. .false.)' will return true instead of false. This is a bug in the TAM machine and not in the Nottran compiler.
- This compiler does not support Real numbers.
- To input a logical value as a user you need to enter '1' for .true and '0' for .false.