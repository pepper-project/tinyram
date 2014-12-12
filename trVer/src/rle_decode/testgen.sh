#!/bin/bash

set -e
set -u

if [[ $# -lt 1 ]]; then
    echo "usage: $0 <olen>"
    exit -1
fi

BASEDIR=$(dirname $0)
OUTFILE=rle_decode_$1.asm

cd ${BASEDIR}/..

# worst-case input
cp rle_decode.asm ${OUTFILE}
( 
    echo '$iter 0'
    echo "tape 0 , $1"

    for i in $(seq 1 $1); do
        echo "tape 0 , 0"
        echo "tape 0 , 0"
    done
    
    for i in $(seq 1 $1); do
        echo "tape 1 , 0"
    done
) >> ${OUTFILE}
