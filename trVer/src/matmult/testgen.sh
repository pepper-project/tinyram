#!/bin/bash

set -e

if [ "$2" != "" ]; then
    NITERS=$2
else
    NITERS=1
fi

set -u

BASEDIR=$(dirname $0)
OUTFILE=matmult_$1

cd ${BASEDIR}/..
for i in $(seq 1 ${NITERS}); do
    ( cat matmult.asm ; ../../trVerLocal/bin/randmats.pl -n 1 -s $1 ) > ${OUTFILE}_$i.asm
done
