# HOWTO run a verified program on Lajos #

Note: Our implementation has a slightly different instruction set from TinyRAM and vnTinyRAM, and encodes its instructions differently. Thus, to distinguish it from the canonical (vn)TinyRAM implementations, we call ours "Lajos."

## Getting started ##

To begin, we'll assume that you have set up your machine as described in the [top-level README](../../../notes/README).

Before beginning, you will also want to learn about the assembler [mnemonics](../asm/asm.pdf) and [directives](TrAsm.html).

One other note: support for "version 0" (TinyRAM-like Harvard architecture) verification machinery is not implemented in `trVer`; therefore, you should write your programs for "version 1" (Lajos).

## Understanding the Prover's execution flow ##

To complete a verified execution, the Prover completes the following steps:

1.  Exogenously compute nondeterministic advice (the auxiliary tape), if any is required. This is done by feeding the `exo0` executable the contents of the input tape. The nondeterministic advice is filled in as part of the satisfying assignment for the verification circuit.

    (Note that in its most general form, `exo0` might want both the input tape and the program text. This requires minor modifications to the first `exo_compute()` invocation in `trVer/src/ver/trTransVer.c`.)

2.  Exogenously execute the program, providing it with the input and auxiliary tapes as well as the processor settings. The result of this execution is the execution transcript and the Benes network settings.

3.  Produce satisfying assignments for the transition verification section of the verification circuit.

4.  Fill in satisfying assignments for the sorted memory transcript using the Benes network settings along with the results of transition verification, the program text, and the input and auxiliary tapes.

5.  Produce satisfying assignments for the memory verification section of the verification circuit.

To enable this execution, we must provide three items beyond those normally required for executing a Pantry computation:

-  A program to execute.
-  The input tape(s) on which the program should operate.
-  If any auxiliary tape is required, an executable which, given the input tape on stdin, produces the auxiliary tape on stdout.

## Creating the program to execute ##

If you have read the above-referenced documentation on the assembler mnemonics and directives, this should be no problem! There are many examples in `trVer/src`---see below for more information.

(NOTE: For the sake of convenience the build pipeline here assumes that all of your input tape iterations are specified in the assembler file alongside the program.)

## Creating the `exo0` executable ##

Consider `mergesort.asm` more closely: because Lajos programs can only return a single value, this program performs a sort, then compares the sorted result against the nondeterministic advice provided via the auxiliary tape, returning `0` on success or `1` on failure. Thus, we need to create an executable to generate this advice. There are a few things to note here:

- The input tape is provided as whitespace-separated rational numbers on stdin of `exo0`. Rationals are represented as `<num>%<denom>`, e.g., `2%1`.
- The auxiliary tape is expected on stdout in a format that can be understood by the `mpq_set_str()` function of libGMP (see [Section 6.1](https://gmplib.org/manual/Initializing-Rationals.html) of the GNU MP library docs).

An example of such a program is provided in `trVer/src/mergesort`. Note the file naming scheme here! `trVer/src/mergesort.asm` corresponds to `trVer/src/mergesort/exo0.c`. The `trVer` build process automatically executes

    make -C src/mergesort exo0

In this case, the automatic Make rule for C source suffices, but in general you may need a Makefile in your equivalent directory. (Of course, nothing requires you to use C for exo0; as long as the file can be invoked via `execl()`, it will work, but you may need a dummy Makefile so that the above make command does not fail.)

## Example programs ##

Most of the example programs in src/ have corresponding test generator shell scripts.

Taking `sparse_matvec.asm` as an example, we can generate a valid set of inputs by running `src/sparse_matvec/testgen.sh <N> <K>`, for an NxN matrix with K nonzero elements. For example,

    src/sparse_matvec/testgen.sh 15 30

will create a new file, `src/sparse_matvec_15_30_1.asm`, which has a precomputed set of inputs and the corresponding output as tapes 0 and 1. You can use this file to test that your program runs correctly and/or to determine the number of steps your program takes to execute. (See immediately below).

## Determining the number of steps, intape and auxtape lengths ##

If you aren't sure how many steps your program will take (which you'll need to tell the Makefile), you can use `TrSim` to figure it out.

The Makefile has a target that handles this case, `build/%_nsteps`. Continuing the `sparse_matvec_15_30_1.asm` example from the *Example programs* section (making sure you have run `make` in the `trAsmProgs` directory first!),

    make build/sparse_matvec_15_30_1_nsteps

Somewhere in the resulting output, TrSim should report that execution took 442 steps.

Each program expects a certain length of input and auxiliary tape, and further we must know these values at compile time. For the present example, the input tape has 93 elements, and the auxiliary tape has 15 elements. You can verify this:

    grep '^tape 0' src/sparse_matvec_15_30_1.asm | wc -l
    grep '^tape 1' src/sparse_matvec_15_30_1.asm | wc -l

To review: our program runs for 442 steps, has 93 elements in the input tape, and 15 in the auxiliary tape.

## Running your program ##

(Again: make sure you've run `make` in the `trAsmProgs` directory.)

We can build the verification infrastructure for a given program as follows:

    make RUN_LOCAL=1 TR_NUMSTEPS=<numsteps> TR_INTAPELEN=<intapelen> TR_AUXTAPELEN=<auxtapelen> build/<computation_name>_ver_pantry

Concretely, you might run

    make RUN_LOCAL=1 TR_NUMSTEPS=128 TR_INTAPELEN=1 TR_AUXTAPELEN=0 build/fact_ver_pantry

or

    make RUN_LOCAL=1 TR_NUMSTEPS=980 TR_INTAPELEN=9 TR_AUXTAPELEN=8 build/mergesort_ver_pantry

or

    make RUN_LOCAL=1 TR_NUMSTEPS=442 TR_INTAPELEN=93 TR_AUXTAPELEN=15 build/sparse_matvec_15_30_1_ver_pantry

Note that the first line above has a zero-length aux tape, and thus does not require `exo0` (and there is no corresponding `src/fact/` directory), while the second and third have aux tapes and thus require `exo0`.

If you do not supply the `RUN_LOCAL=1` flag, the program will be compiled but not executed. You might want to do this, for example, so that you can edit the verifier's input generator subroutine (by default, whatever was in tape 0 is fixed as the input to the computation, but perhaps you will want to generate a random set of inputs for each run). After compiling as above, the input generator lives in `build/zcc/apps_sfdl_gen/<computation_name>_ver_v_inp_gen.cpp`. Note that the "input" includes both the program itself as well as the contents of the input tape, in that order. Be careful that you don't disturb the values that encode the program text (i.e., you should only change the last `TR_INTAPELEN` values in the aforementioned file).

After you've compiled the program as above, you can (re)run the computations manually by copying the contents of `build/zcc/bin`, `build/zcc/apps_sfdl_hw`, and `build/zcc/apps_sfdl_gen` to the corresponding directories in `../../pepper` (you may need to create the `bin` and `apps_sfdl_gen` subdirectories). Then `cd ../../pepper; ./run/run_pepper.sh <computation_name>_ver`.
