# TrVer - run execution verifier locally #

## Basic usage ##

    ./trVerLocal/bin/TrVer [exec0.so [exec1.so [...]]]

TrVer will dynamically load the libraries listed on the commandline and
execute the compute() function on the included data. See the Makefile in
trVerLocal for details on how the .sofiles are made.

