#!/bin/bash

set -e
set -u

BASEDIR=$(dirname $0)
OUTFILE=ptrchase_$1.asm

cd ${BASEDIR}/..
( cat ptrchase.asm ; ptrchase/chasegen.pl $1 ) > ${OUTFILE}
