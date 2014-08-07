# TrExo - TinyRAM simulation interface for exo_compute #

## Usage ##

When verifying a TinyRAM execution, the `exo_compute()` pseudo-constraint
is used to nondeterminmistically generate the TinyRAM transcript and
Benes network settings. TrExo provides an interface to the TinyRAM
execution pipeline that is compatible with the implementation in
`compute_exo_compute()` from `computation_p.cpp`.

TrExo takes one argument, the number of output values expected, which it
checks against the number actually generated. All other inputs to TrExo
come from stdin, as

    [ program ] [ intape ] [ auxtape ] [ config ]

