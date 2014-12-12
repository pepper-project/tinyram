#!/bin/bash

if [[ $# -lt 2 || $1 -gt 13 || $1 -lt 2 ]]; then
    echo "usage: $0 seq_len n_perms [n_tests]"
    echo "2 <= seq_len < 14"
    exit -1;
fi

set -e

if [ "$3" != "" ]; then
    NITERS=$3
else
    NITERS=1
fi

set -u

BASEDIR=$(dirname $0)
OUTFILE=fannkuch_$1_$2

cd ${BASEDIR}
make
cd ..
OFi=${OUTFILE}.asm
cp fannkuch.asm ${OFi}
if [[ $1 -eq 13 ]]; then
    ( echo -e "tape 0 , 13\ntape 0 , "$2
    for i in 13 10 8 12 11 1 7 6 9 4 3 2 5; do
        echo "tape 0 , $i"
    done ) >> ${OFi}
else
    fannkuch/maxrun $1 $2 >> ${OFi}
fi
echo -n "tape 1 , " >> ${OFi}
grep 'tape 0' ${OFi} | cut -d , -f 2 | tr '\n' ' ' | fannkuch/exo0 >> ${OFi}
