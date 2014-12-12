#!/bin/bash

if [[ $# -lt 2 ]]; then
    echo "usage: $0 n k"
    exit -1
fi

set -e

if [ "$3" != "" ]; then
    NITERS=$3
else
    NITERS=1
fi

set -u

BASEDIR=$(dirname $0)
OUTFILE=sparse_matvec_$1_$2

cd ${BASEDIR}/..
for i in $(seq 1 ${NITERS}); do
    ( cat sparse_matvec.asm ; sparse_matvec/test $1 $2 ) > ${OUTFILE}_$i.asm
done
