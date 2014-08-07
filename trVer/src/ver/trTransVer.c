//
// NOTE: there are no headers included here
// because you need to construct the actual C file
// using C outputs from TrAsm and TrSim
// see trVer/mkVer.sh for more details
//

// NOTE
// this will ONLY work with version 1!

void compute(struct In *input, struct Out *output) {
    // set correct until we find a problem
    output->correct = true;

#ifndef VER_LOCAL
#if TR_AUXTAPELEN > 0
    // aux tape is exogenously computed from the input tape, but only if we actually have an aux tape
    uint64_t *exo0_inputs[1] = {input->tape0};
    uint16_t exo0_lens[1] = {TR_INTAPELEN};
    exo_compute(exo0_inputs,exo0_lens,output->tape1,0);
#endif // TR_AUXTAPELEN

    // execution transcript and benes settings come from
    // exogenous execution of the processor via TrExo
    uint64_t exo1_settings[8] = {1,TR_REGSIZE,TR_NREGBITS,TR_IMMSIZE,TR_INTAPE_LOC,TR_AUXTAPE_LOC,TR_NUMSTEPS,2*TR_BENES_SWITCHES};
#if TR_AUXTAPELEN > 0
    uint64_t *exo1_inputs[4] = {input->pmem,input->tape0,output->tape1,exo1_settings};
#else
    uint64_t auxTapeDummy[1] = {0};
    uint64_t *exo1_inputs[4] = {input->pmem,input->tape0,auxTapeDummy,exo1_settings};
#endif // TR_AUXTAPELEN
    uint16_t exo1_lens[4] = {TR_PROGSIZE,TR_INTAPELEN,TR_AUXTAPELEN,8};
    TrExoOutput exo1_output;
    exo_compute(exo1_inputs,exo1_lens,exo1_output,1);
#endif  // VER_LOCAL

    // memTrans comes from TR1, memTransSorted comes from the Benes network
    TrMemState memTrans[2*TR_BENES_SWITCHES];
    uint32_t i;

    for (i=0;i<TR_PROGSIZE;++i) {
        recordStore(i, input->pmem[i], memTrans, i);
    }

    for (i=0;i<TR_INTAPELEN;++i) {
        recordStore(TR_INTAPE_LOC+i, input->tape0[i], memTrans, TR_PROGSIZE+i);
    }

#if TR_AUXTAPELEN > 0
    for (i=0;i<TR_AUXTAPELEN;++i) {
        recordStore(TR_AUXTAPE_LOC+i, output->tape1[i], memTrans, TR_PROGSIZE+TR_INTAPELEN+i);
    }
#endif // TR_AUXTAPELEN

    // enforce starting state correctness
    // everything is 0 except instruction, which equals instruction 0
    // stateCompare returns true if something is WRONG
    if (stateCompare(exo1_output.transcript,input->pmem[0])) {
        output->correct = false;
#ifdef VER_LOCAL
        printf("transcript base case failed\n");
#endif
    }

    uint32_t memTransOffset = TR_PROGSIZE+TR_INTAPELEN+TR_AUXTAPELEN;
    for (i=0;i<TR_NUMSTEPS-1;++i) {
        recordLoad(&(exo1_output.transcript[i]), memTrans, 2*i+memTransOffset);

        // set step number for this transition's memop
        memTrans[1 + 2*i + memTransOffset].stepNum = 1 + 2*i + memTransOffset;
        // trVer returns true if the transition is WRONG
        if (trVer(&(exo1_output.transcript[i]),&(exo1_output.transcript[i+1]),input->pmem,&(memTrans[1 + 2*i+memTransOffset]))) {
            output->correct = false;
#ifdef VER_LOCAL
            printf("trVer failed at step %d\n",i);
#endif
        }
    }

    recordLoad(&(exo1_output.transcript[i]), memTrans, 2*i+memTransOffset);
    memTrans[1+2*i+memTransOffset].memType = 2;

    // retVer returns true if the return value is nonzero
    if (retVer( &(exo1_output.transcript[i]), &(output->retVal) )) {
        output->correct = false;
#ifdef VER_LOCAL
        printf("retVer failed\n");
#endif
    }

    // set the rest of the memory transcript to non-memops
    // since they're non-memops, we don't have to set anything else about them
    for (i = 2 + 2*i + memTransOffset; i < 2*TR_BENES_SWITCHES; i++) {
        memTrans[i].memType = 2;
    }

    // permute memTrans
    uint32_t bStep = 1;
    for (i=0;i<TR_BENES_STAGES;++i) {
        benesPermute(memTrans, exo1_output.benesRoute, i*TR_BENES_SWITCHES, bStep);
        if (i >= TR_BENES_STAGES/2) {
            bStep = bStep / 2;
        } else {
            bStep = bStep * 2;
        }
    }

    // finally, make sure that the memory transcript is consistent
    // the base case is a write of pmem[0] to address 0
    TrMemState initMemOp = {0,0,0,input->pmem[0]};
    // memStateCompare returns true if something is WRONG
    if (memStateCompare(memTrans,&initMemOp)) {
        output->correct = false;
#ifdef VER_LOCAL
        printf("mem state base case failed\n");
#endif
    }

    for (i=0;i<2*TR_BENES_SWITCHES-1;++i) {
        // memVer returns true if something is WRONG
        if (memVer(&(memTrans[i]),&(memTrans[i+1]))) {
            output->correct = false;
#ifdef VER_LOCAL
            int offset;
            printf("memVer failed at step %d\n",i);
            for (offset=0; offset<2; offset++) {
            printf("%d: %d %d %" PRIx64 " %" PRIx64 "\n",offset,memTrans[i+offset].memType,memTrans[i+offset].stepNum,memTrans[i+offset].memAddr,memTrans[i+offset].memData);
            }

            for (offset=0; offset<2; offset++) {
            printf("%ds: %d %d %" PRIx64 " %" PRIx64 "\n",offset,exo1_output.memTransSorted[i+offset].memType,exo1_output.memTransSorted[i+offset].stepNum,exo1_output.memTransSorted[i+offset].memAddr,exo1_output.memTransSorted[i+offset].memData);
            }
#endif
        }
    }
}

void benesPermute (TrMemState *list, bool *route, int offset, int bSteps) {
    int i, j;
    TrMemState temp;
    int swPerStep = TR_BENES_SWITCHES/bSteps;

    for (i=0;i<bSteps;++i) {
        for (j=0;j<swPerStep;++j) {
            if (route[offset+j+i*swPerStep]) { // swap indicated
                temp = list[2*i*swPerStep+j];
                list[2*i*swPerStep+j] = list[(2*i+1)*swPerStep+j];
                list[(2*i+1)*swPerStep+j] = temp;
            }
        }
    }
}

bool memStateCompare(TrMemState *in1, TrMemState *in2) {
    bool flag = false;
    if ( (in1->memType != in2->memType) ||
         (in1->stepNum != in2->stepNum) ||
         (in1->memAddr != in2->memAddr) ||
         (in1->memData != in2->memData) ) {
        flag = true;
    }
    return flag;
}

bool stateCompare( TrState *in, uint64_t instr) {
    int i;
    bool flag = false;

    // check PC, instruction, and flag
    if ( (in->pc != 0) || (in->instr != instr) || (in->flag != false) ) {
        flag = true;
    }

    for (i=0;i<TR_NREGS;++i) {
        if (in->regs[i] != 0) { flag = true; }
    }

    return flag;
}

bool memVer(TrMemState *prev, TrMemState *curr) {
    bool $wrong = false;
#include "MC.c"
    return $wrong;
}

bool retVer ( TrState *s0 , uint64_t *retVal ) {
    uint16_t op = getOp($instr);
    bool uImm = getuImm($instr);
    uint64_t imm = getImm($instr);
    bool flag;

    // set the return value
    *retVal = uImm ? imm : regFetch(imm, $s0R);
    // did the program ANSWER?
    if ( op == 31 )
        flag = false;
    else
        flag = true;

    return flag;
}

bool trVer (TrState *s0, TrState *s1, uint64_t *P, TrMemState *output) {
    bool TrWrong;
#include "TR1.c"
    return TrWrong;
}

void recordLoad(TrState *s0, TrMemState *output, uint16_t step) {
    // record the memory load
    output[step].memType = 1;
    output[step].stepNum = step;
    output[step].memAddr = $pc;
    output[step].memData = $instr;
}

void recordStore(uint64_t addr, uint64_t data, TrMemState *output, uint16_t step) {
    // record memory store
    output[step].memType = 0;
    output[step].stepNum = step;
    output[step].memAddr = addr;
    output[step].memData = data;
}

#ifndef VER_LOCAL
#include "trTransVerUtil.c"
#include "TR1Util.c"
#endif  // VER_LOCAL

