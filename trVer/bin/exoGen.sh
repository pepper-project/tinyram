#!/bin/sh
PREFIX=$1
AUXLEN=$2
OUTFILE=$3

cat > $OUTFILE << EOF
#include <apps_sfdl_hw/${PREFIX}_p_exo.h>
#include <apps_sfdl_gen/${PREFIX}_cons.h>
#include <common/sha1.h>
#include <storage/configurable_block_store.h>

#pragma pack(push)
#pragma pack(1)

//This file will NOT be overwritten by the code generator, if it already
//exists. make clean will also not remove this file.

${PREFIX}ProverExo::${PREFIX}ProverExo() {

  //Uncomment and fix to specify the sizes of the input and output types
  //to baseline_minimal:
  //baseline_minimal_input_size = sizeof(something);
  //baseline_minimal_output_size = sizeof(something);

}

//using namespace ${PREFIX}_cons;

void ${PREFIX}ProverExo::init_exo_inputs(
  const mpq_t* input_q, int num_inputs,
  char *folder_path, HashBlockStore *bs) {
  
}

void ${PREFIX}ProverExo::export_exo_inputs(
  const mpq_t* output_q, int num_outputs,
  char* folder_path, HashBlockStore *bs) {

}

void ${PREFIX}ProverExo::run_shuffle_phase(char *folder_path) {

}

void ${PREFIX}ProverExo::baseline_minimal(void* input, void* output){
  //Run the computation
}

void ${PREFIX}ProverExo::baseline(const mpq_t* input_q, int num_inputs, 
      mpq_t* output_recomputed, int num_outputs) {
  //struct In input;
  //struct Out output;
  // Fill code here to prepare input from input_q.
  
  // Call baseline_minimal to run the computation

  // Fill code here to dump output to output_recomputed.
}

//Refer to apps_sfdl_gen/${PREFIX}_cons.h for constants to use in this exogenous
//check.
bool ${PREFIX}ProverExo::exogenous_check(const mpz_t* input, const mpq_t* input_q,
      int num_inputs, const mpz_t* output, const mpq_t* output_q, int num_outputs, mpz_t prime) {

  bool passed_test = true;
#ifdef ENABLE_EXOGENOUS_CHECKING
  gmp_printf("%Qd %Qd [ ", output_q[1], output_q[2]);
  for(int i=0; i < ${AUXLEN}; i++) {
    gmp_printf("%Qd, ", output_q[3+i]);
  }
  gmp_printf("]\n");
  if (mpq_cmp_ui(output_q[1], 0, 1) == 0) {
    passed_test = false;
  }
#else
  gmp_printf("<Exogenous check disabled>\n");
#endif
  return passed_test;
};

#pragma pack(pop)
EOF
