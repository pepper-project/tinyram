#!/bin/bash

INFILE=$(basename $1)
PROGFILE=$(echo $INFILE | sed 's/out_.*\.c/prog.c/');
OUTFILE="$(echo $1 | sed 's/_out_/_ver_/')";
if [ ! -f $1 ] || [ $1 == $OUTFILE ]; then
    exit 1;
fi

# we intentionally include INFILE and PROGFILE twice
# they are set up so that when included before trTransVer.h they
# only provide the necessary DEFINES for trTransVer.h; afterwards,
# they actually provide the necessary variable declarations

cat > $OUTFILE <<EOF
#include "$INFILE"
#include "$PROGFILE"
#include "trTransVer.h"
struct In input = {
#include "$INFILE"
#include "$PROGFILE"
};
struct Out output;
#include "trTransVer.c"
EOF
