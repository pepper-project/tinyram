# TrDasm - disassembler for TinyRam virtual processor #

## Basic Usage ##

    ./TrDasm [-f <fNum>] [infile1 [infile2 ...]]

TrDasm reads in `.jo` files created by TrAsm and outputs assembler
mnemonics (TrAsm format 0).

The `-f` flag specifies the output format argument that is inserted into
the TinyRAM directive in the resulting disassembled output file.

TrDasm loads data from all input files and disassembles the first
program found, if any. It then disassembles all tape iterations present
in all input files, if any.

