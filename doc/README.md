# HOWTO run a verified program on Lajos #

## Getting started ##

To begin, we'll assume that you have set up your machine as described in the [top-level README](../../../notes/README).

Before beginning, you will also want to learn about the assembler [mnemonics](../asm/asm.pdf) and [directives](TrAsm.html).

One other note: support for "version 0" (TinyRAM) verification machinery is not implemented in `trVer`; therefore, you should write your programs for "version 1" (Lajos).

## Understanding the Prover's execution flow ##

To complete a verified execution, the Prover completes the following steps:

1.  Exogenously compute nondeterministic advice (the auxiliary tape), if any is required. This is done by feeding the `exo0` executable the contents of the input tape. The nondeterministic advice is filled in as part of the satisfying assignment for the verification circuit.

    (Note that in its most general form, `exo0` might want both the input tape and the program text. This requires only minor modifications to the first `exo_compute()` invocation in `trVer/src/ver/trTransVer.c`.)

2.  Exogenously execute the program, providing it with the input and auxiliary tapes as well as the TinyRAM processor settings. The result of this execution is the execution transcript and the Benes network settings.

3.  Produce satisfying assignments for the transition verification section of the verification circuit.

4.  Fill in satisfying assignments for the sorted memory transcript using the Benes network settings along with the results of transition verification, the program text, and the input and auxiliary tapes.

5.  Produce satisfying assignments for the memory verification section of the verification circuit.

To enable this execution, we must provide three items beyond those normally required for executing a Pantry computation:

-  A program to execute.
-  The input tape(s) on which the program should operate.
-  If any auxiliary tape is required, an executable which, given the input tape on stdin, produces the auxiliary tape on stdout.

## Creating the program to execute ##

If you have read the above-referenced documentation on the assembler mnemonics and directives, this should be no problem! There are examples in `trVerLocal/`, e.g., `factv1.asm`, `matmult_v1.asm`, and `mergesort_v1.asm`.

(For the sake of convenience the build pipeline here assumes that all of your input tape iterations are specified in the assembler file alongside the program.)

## Creating the `exo0` executable ##

Consider `mergesort_v1.asm` more closely: because Lajos programs can only return a single value, this program performs a sort, then compares the sorted result against the nondeterministic advice provided via the auxiliary tape, returning `0` on success or `1` on failure. Thus, we need to create an executable to generate this advice. There are a few things to note here:

- The input tape is provided as whitespace-separated rational numbers on stdin of `exo0`. Rationals are represented as `<num>%<denom>`, e.g., `2%1`.
- The auxiliary tape is expected on stdout in a format that can be understood by the `mpq_set_str()` function of libGMP (see [Section 6.1](https://gmplib.org/manual/Initializing-Rationals.html) of the GNU MP library docs).

An example of such a program is provided in `trVer/src/mergesort`. Note the file naming scheme here! `trVer/src/mergesort.asm` corresponds to `trVer/src/mergesort/exo0.c`. The `trVer` build process automatically executes

    make -C src/mergesort exo0

In this case, the automatic Make rule for C source suffices, but in general you may need a Makefile in your equivalent directory. (Of course, nothing requires you to use C for exo0; as long as the file can be invoked via `execl()`, it will work, but you may need a dummy Makefile so that the above make command does not fail.)

## Running your program ##

From the `tinyram` base directory, first run

    make trAsmProgs

This builds the executables associated with assembling and executing Lajos programs.

Next,

    cd trVer
    make TR_NUMSTEPS=<numsteps> TR_INTAPELEN=<intapelen> TR_AUXTAPELEN=<auxtapelen> build/<program>_ver_pantry

Concretely, you might run

    make TR_NUMSTEPS=128 TR_INTAPELEN=1 TR_AUXTAPELEN=0 build/fact_ver_pantry

or

    make TR_NUMSTEPS=980 TR_INTAPELEN=9 TR_AUXTAPELEN=8 build/mergesort_ver_pantry

Note that the first line above has a zero-length aux tape, and thus does not require `exo0` (and there is no corresponding `src/fact/` directory), while the second has an aux tape and thus requires `exo0`.

