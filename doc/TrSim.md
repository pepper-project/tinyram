# TrSim - simulator for TinyRAM virtual processor #

## Basic Usage ##

    ./TrSim [-n <nSteps>] [-c | -cR] [-p] [infile1 [infile2 ...]] [outdir]

TrSim reads `.jo` files produced by modes 2, 3, or 4 of TrAsm. It loads
all files specified and executes the first program found against all
tape iterations in all input files.

If the `-n` flag is present, all transcripts will have (at least)
`<nSteps>` steps of execution. If the `ANSWER` instruction is executed
before the final step, the final state is dumped repeatedly.

If the `-c` flag is present, outputs are in C declaration format for use
with the transition verification machinery. Each output file contains
the tapes against which the program was run and the transcript of
execution. `-cR` outputs only the transcript and not the tapes.

The `-p` flag specifies that the memory transcript should be output in
packed form. This uses one field element per transcript entry, Note that
the packed format is structured such that when verifying the ordering
one need only compare the numeric values of the transcript entries.

Note that unless the execution length is VERY long, it is better to use
multiple parallel Benes networks to route the individual data separately
than to unpack the data from a single network. Consider: we add a number
of constraints equal to nBits\*T when unpacking (T the length of the
memory transcript, nBits the length of each element), whereas each
additional Benes network costs about T\*2\*log(T) additional constraints.
So unless T is about 2^W, it is cheaper to build separate networks for
each piece.

If you specify a directory in any argument to TrSim, execution
transcripts for each iteration are written to out.<iternumber> in that
directory. Otherwise, the present directory is used. If an input file
is also specified (as opposed to STDIN), TrSim tries to name the
output files intelligently. If you do not want this behavior, list the
output directory last on the commandline; otherwise, list it before
the input file.

TrSim displays the return values for each iteration on STDOUT. These
return values can also be recovered by examining the final instruction
executed in each transcript, since by definition that instruction must
be `answer`.
