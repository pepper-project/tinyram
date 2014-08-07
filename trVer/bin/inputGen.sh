#!/bin/sh

PREFIX=$1
INPUT1=$2
INPUT2=$3
OUTFILE=$4

cat > $OUTFILE << EOF
#include <apps_sfdl_gen/${PREFIX}_v_inp_gen.h>
#include <apps_sfdl_gen/${PREFIX}_cons.h>
#pragma pack(push)
#pragma pack(1)

${PREFIX}VerifierInpGen::${PREFIX}VerifierInpGen() {
}

//Refer to apps_sfdl_gen/${PREFIX}_cons.h for constants to use when generating input.
void ${PREFIX}VerifierInpGen::create_input(mpq_t* input_q, int num_inputs) {

EOF

cat $INPUT1 >> $OUTFILE
cat $INPUT2 >> $OUTFILE

cat >> $OUTFILE << EOF

}

#pragma pack(pop)
EOF
