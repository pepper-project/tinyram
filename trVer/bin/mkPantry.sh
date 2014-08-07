#!/bin/bash

INFILE=$(basename "$1" .jo)
PROGFILE=${INFILE}_prog.c
BASEFILE=$(dirname "$1")/${INFILE}_ver
OUTFILE=${BASEFILE}.c
TMPFILE=${BASEFILE}_tmp_$$.c

cat > "$TMPFILE" <<EOF
#include "$PROGFILE"
#include "trTransVer.h"
#include "trTransVer.c"
EOF

gcc -E -o "$OUTFILE" "$TMPFILE"
rm "$TMPFILE"
