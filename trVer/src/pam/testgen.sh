#!/bin/bash

if [[ $# -lt 4 ]]; then
    echo "usage: $0 n_dim n_points n_medoids n_pam_iters [n_tests]"
    exit -1;
fi

set -e

if [ "$5" != "" ]; then
    NITERS=$5
else
    NITERS=1
fi

set -u

BASEDIR=$(dirname $0)
OUTFILE=pam_$1_$2_$3_$4

cd ${BASEDIR}/..
for i in $(seq 1 ${NITERS}); do
    OFi=${OUTFILE}_$i.asm
    ( cat pam.asm ; pam/pamgen.pl $1 $2 $3 $4 ) > ${OFi}
    grep 'tape 0' ${OFi} | cut -d , -f 2 | tr '\n' ' ' | pam/exo0 | tr ' ' '\n' | awk '{print("tape 1 ,",$0);}' >> ${OFi}
done
