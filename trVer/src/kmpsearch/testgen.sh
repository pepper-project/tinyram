#!/bin/bash

set -e
set -u

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <n_len> <h_len>"
    exit -1
fi

BASEDIR=$(dirname $0)
OUTFILE=kmpsearch_$1_$2.asm

cd ${BASEDIR}/..

# worst-case input
cp kmpsearch.asm ${OUTFILE}
( 
    echo '$iter 1'
    echo "tape 0 , $1"
    echo "tape 0 , $2"

    for i in $(seq 2 $1); do
        echo "tape 0 , 0"
    done
    
    echo "tape 0 , 1"

    for i in $(seq 1 $2); do
        echo "tape 0 , 0"
    done

    echo "tape 1 , $2"
) >> ${OUTFILE}
