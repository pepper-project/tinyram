# TrAsm - assembler for TinyRAM virtual processor #

## Basic Usage ##

    ./TrAsm [-o <outbin>] [-f <outformat>] [-c | -cP | -cT] [-p] [-D <n> <i> <a>] [infile]

The `-f` flag allows you to override the output format specified in the
TinyRAM directive (see below).

The `-o` flag lets you specify an output file for binary data.
Otherwise, it is the same as the input file with the suffix `.jo`,
or STDOUT. If the argument to `-o` is `-`, output is to STDOUT.

The `-c` flag specifies that when in mode 2, 3, or 4 TrAsm should output
c struct definitions for the program and tape files. If writing to a
file, this happens in addition to the `.jo` file; if writing to STDOUT,
the C output takes the place of the binary output. `-cP` and `-cT`
indicate that when in mode 2, the C definitions are restricted to the
(P)rogram or (T)apes, respectively.

The `-p` flag enables packed program output. In this mode, the program
is packed into a single value and must be unpacked in the constraint
machinery. Note that this is generally more efficient than fetching
individual components of the instruction, since each fetch costs as many
constraints as there are words in the program memory.

The `-D` flag causes TrAsm to output program and tape size `DEFINE`s
along with the C-formatted program output. `<n>` is the number of
steps, `<i>` is the size of the input tape, and `<a>` is the size of
the auxiliary tape.

An input file may be specified, or the input can be piped into STDIN.
TrAsm ignores nonexistent input files specified on the commandline.

## Assembler Syntax ##

### Comments ###

Comments begin with `;` or `#` and continue until the end of the line.
They are ignored.

`#` is treated as a comment because this lets you use the c preprocessor
to generate asm files. This lets you use macros without my having to
reimplement them.

### Immediates ###

An immediate value is an integer constant matching the regexp 
`/^-?(0[XxBbOo])?[0-9a-fA-F]+$/`

`0x` indicates hexadecimal, `0b` indicates binary, and `0o` indicates
octal. Of course, please only use digits that are appropriate for
your chosen base or you'll get a runtime conversion error.

Note that immediates are represented internally as `BigInteger`s and can
thus be any size. Of course, you'll get a warning if you try to assemble
something with an immediate `>=2^wordsize` (see the TinyRAM directive,
below), as this is the size of the program counter, registers, and
immediate operand field.

### Labels ###

A label is a string that matches `/^_[0-9a-zA-Z_]+$/`. (Yes, all labels
begin with `_`.) Labels may be defined in one of two ways:

    _MY1LABEL :

`_MY1LABEL` equals the value of the program counter where it appears in
the input file.


    _MY2LABEL = 0x5F

`_MY2LABEL` equals `0x5F`. The RHS of a label assignment is a label
expression (see below). Labels may be used to define other labels, but
forward references are not allowed in label assignments because these
are resolved during the first pass.

### Assembler mnemonics ###

Assembler mnemonics are detailed in [TinyRAM and Lajos instructions](../asm/asm.pdf).

Mnemonics are case insensitive.

Note, however, that the order of operands for the `STORE` instruction is
reversed compared to the reference document (this keeps the syntax
completely regular). In other words, the `A` operand is *always* last.

#### tape mnemonic ####

In addition to the above, the `tape` mnemonic is used to specify tape
values for the TinyRAM processor. There are two variants:

    tape <tapeNum> <value>
    tape <tapeIter> <tapeNum> <value>

The first invocation will use the present tape iteration value, set with
the `iter` directive (see below). The tape iteration defaults to 0.

When a program will be run many times with different inputs, all inputs
can be specified in the same asm file by defining a sequence of tape
iterations. This tells the simulator that the program is to be run
against each set of tape values.

### Operands ###

Each instruction takes 1, 2, or 3 operands. All but the last operand
must be a register. The final operand can be either a register or a
label expression.

#### Registers ####

A register matches `/^[rR][0-9]+$/` and in addition must specify a
register number between `0` and `(2^numRegBits)-1` (see the TinyRAM
directive, below).

#### Label expressions ####

A label expression is an immediate, a label, or a bracket-enclosed
arithmetic or logical expression of immediates and labels. Each
operation must be enclosed in its own square brackets, which while
somewhat inconvenient obviates operator precedence (and I am lazy).

Note that unlike in assembler directives and label assignments, label
expressions in instruction operands *can* have forward references
because operands are not resolved until the second pass.

The supported arithmetic operators are `+`, `-` (subtract), `~` (unary
negation), `*`, `/`, `%`, and `>`. `[>_FOO]` computes the difference between
the current location and `_FOO` for relative addressing as in Lajos.

NOTE: The `-` operator MUST be followed by a space if its right operand
is an immediate.

The supported logical operators are `&`, `|`, `^` (xor), `!` (unary
not), `<<` (left shift), `>>` (right shift). `>>` for negative numbers
gives a negative result.

Thus, the following are possible:

    _BAR = 0xFF
    add r1,r2,[37%5]
    add r2,r0,[>0]      ; in Lajos, value of PC is now in r2
    jmp _FOO
    _FOO:
    not r2,[[[0x100 - 1] & _BAR] ^ [0xde % 0xad]]

### Assembler directives ###

An assembler directive gives information to the assembler. It is
specified as:

    $ directiveName arg1 [arg2 ...]

`directiveName` is case insensitive. Arguments are label expressions. No
forward references are allowed in assembler directives because they are
resolved in the first pass.

There are two supported directives:

#### TinyRAM directive ####

    $ TinyRAM <version> <outputformat> <wordsize> <numRegBits>

For example:

    $ TinyRAM 0 0 32 5

This directive is used to indicate to the assembler the expected TinyRAM
processor version, assembler output format, wordsize in bits, and number
of registers in the TinyRAM processor. This must be specified so the
assembler knows the correct format to generate.

NOTE: unlike all other immediates, arguments to the TinyRAM directive
are cast to Java `int`s. Their legal range is thus `-2^31 <= x < 2^31`

Supported output formats:

 * 0 - text

    When fed the output of mode 0, TrAsm should be idempotent (otherwise
    something is wrong!)

 * 1 - debug pseudo-binary

    The output in this mode is a list of hex addresses and ASCII-binary
    program words

 * 2 - java object, program and tapes

    This is the input format suitable for TrSim. It is a serialized
    object that TrSim reads using ObjectInputStream

 * 3 - java object, program only

    As above, but only the program is assembled. Tapes values are ignored.

 * 4 - java object, tapes only

    As above, but only the tape values are produced.

NOTE: output formats 2, 3, and 4 produce a binary output file. See the
description of the `-o` flag, above.

#### Origin directive ####

    $ org <location>

For example:

    $ org 0xFF

    $ org _TABLE

This directive tells the assembler to set the PC to `<location>` and
continue assembling. It is useful, e.g., for creating jumptables.

#### Tape iteration directive ####

    $ iter <tapeIter>

For example:

    $ iter _ITERNUM

This directive tells the assembler to set the present iteration number
to `<tapeIter>` for the two-argument `tape` mnemonic (see above).

#### Tape Location Directives ####

    $ inTape 1024
    $ auxTape _AUXBASE

For Lajos (version 1) only, sets the location in memory where the input
and auxiliary tapes are written. If you do not set this, probably your
program will not work, since by default the tapes are located at zero!

In addition, the `_inTape` and `_auxTape` labels are set by these
commands, respectively.

Note that since mnemonics are resolved in the first pass, forward
references are not allowed here.
