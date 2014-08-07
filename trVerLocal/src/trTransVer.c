//
// NOTE: there are no headers included here
// because you need to construct the actual C file
// using C outputs from TrAsm and TrSim
// see trVer/mkVer.sh for more details
//

void compute(struct In *input, struct Out *output) {
    uint32_t i;
    uint32_t bStep = 1;

    output->correct = true;

#ifdef TR_VERSION_1
    for (i=0;i<TR_PROGSIZE;++i) {
        recordStore(i,input->pmem[i],&($memTrans[i]));
    }

    for (i=0;i<input->tape0.size;++i) {
        recordStore(TR_INTAPE_LOC+i,input->tape0.data[i],&($memTrans[TR_PROGSIZE+i]));
    }

    for (i=0;i<input->tape1.size;++i) {
        recordStore(TR_AUXTAPE_LOC+i,input->tape1.data[i],&($memTrans[TR_PROGSIZE+input->tape0.size+i]));
    }
#endif // TR_VERSION_1

    // check the transitions, write the memory transcript results
#if defined TR_VERSION_0
    if (! stateCompare(input->transcript,0) )
#elif defined TR_VERSION_1
    if (! stateCompare(input->transcript,input->pmem[0]) )
#endif // TR VERSIONS
    {
        output->correct = false;
        output->failStep = 0;
    }
    for (i=0;i<TR_NUMSTEPS-1;++i) {
#if defined TR_VERSION_0
        trVer(&(input->transcript[i]),&(input->transcript[i+1]),&(input->tape0),&(input->tape1),input->pmem,&($memTrans[i+TR_MEMTRANS_OFFSET]));
        if ($memTrans[i+TR_MEMTRANS_OFFSET].wrong)
#elif defined TR_VERSION_1
        recordLoad(&(input->transcript[i]), &($memTrans[2*i+TR_MEMTRANS_OFFSET]));

        trVer(&(input->transcript[i]),&(input->transcript[i+1]),input->pmem,&($memTrans[1 + 2*i+TR_MEMTRANS_OFFSET]));
        if ($memTrans[1+2*i+TR_MEMTRANS_OFFSET].wrong)
#endif // TR VERSIONS
        {
            output->correct = false;
            output->failStep = i+1;
        }
    }

#if defined TR_VERSION_0
    $memTrans[i+TR_MEMTRANS_OFFSET].memType=2;
#elif defined TR_VERSION_1
    $memTrans[2*i+TR_MEMTRANS_OFFSET].memType=1;
    $memTrans[2*i+TR_MEMTRANS_OFFSET].memAddr=input->transcript[i].pc;
    $memTrans[2*i+TR_MEMTRANS_OFFSET].memData=input->transcript[i].instr;
    $memTrans[1+2*i+TR_MEMTRANS_OFFSET].memType=2;
#endif // TR VERSIONS

    // check the return value
#if defined TR_VERSION_0
    if (! retVer( input->returnValue , &(input->transcript[i]), instrFetch(input->transcript[i].pc,input->pmem)))
#elif defined TR_VERSION_1
    if (! retVer( input->returnValue , &(input->transcript[i]), input->transcript[i].instr))
#endif // TR VERSIONS
    {
        output->correct = false;
        output->failStep = i+1;
    }


    // set the step numbers
    for (i=0;i<2*TR_BENES_SWITCHES;++i) {
        $memTrans[i].stepNum = i;
    }

    // permute $memTrans
    for (i=0;i<TR_BENES_STAGES;++i) {
        benesPermute($memTrans, input->benesRoute, i*TR_BENES_SWITCHES, bStep);
        if (i >= TR_BENES_STAGES/2) {
            bStep = bStep / 2;
        } else {
            bStep = bStep * 2;
        }
    }

#ifndef ZAATAR_MODE
    // compare that transcript to the purported memory transcript
    // strictly speaking this is not necessary, but it's a good check of
    // the code that's used to generate nondeterministic input for the Prover
    for (i=0;i<2*TR_BENES_SWITCHES;++i) {
        if (! memStateCompare(&($memTrans[i]),&($memTransS[i]))) {
            output->correct = false;
            output->failStep = TR_NUMSTEPS + i;
        }
    }
#endif // ZAATAR_MODE

    // finally, make sure that the memory transcript is consistent
    // the base case is a write of 0 to address 0
    TrMemState initMemOp = {0,};
    if (! memVer(&initMemOp,$memTrans)) {
        output->correct = false;
        output->failStep = 2*TR_BENES_SWITCHES + TR_NUMSTEPS;
    } else {
        for (i=0;i<2*TR_BENES_SWITCHES-1;++i) {
            if (! memVer(&($memTrans[i]),&($memTrans[i+1]))) {
                output->correct = false;
                output->failStep = 2*TR_BENES_SWITCHES + TR_NUMSTEPS + i + 1;
            }
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
    bool flag = true;
    if ( (in1->wrong   != in2->wrong  ) ||
         (in1->memType != in2->memType) ||
         (in1->stepNum != in2->stepNum) ||
         (in1->memAddr != in2->memAddr) ||
         (in1->memData != in2->memData) ) {
        flag = false;
    }
    return flag;
}

bool stateCompare( TrState *in, uint64_t instr) {
    int i;
    bool flag = true;
    if ( (in->pc != 0) ||
#ifdef TR_VERSION_0
         (in->iTPtr != 0) ||
         (in->aTPtr != 0) ||
#endif // TR_VERSION_0
         (in->instr != instr) ||
         (in->flag != false) ) {
        flag = false;
    } else {
        for (i=0;i<TR_NREGS;++i) {
            if (in->regs[i] != 0) { flag = false; }
        }
    }

    return flag;
}

bool memVer(TrMemState *prev, TrMemState *curr) {
    bool $wrong = false;
#include "MC.c"
    return (!$wrong);
}

bool retVer ( uint64_t retVal, TrState *s0, uint64_t instr ) {
    uint16_t op = getOp(instr);
    bool uImm = getuImm(instr);
    uint64_t imm = getImm(instr);
    uint64_t op2 = uImm ? imm : regFetch(imm, $s0R);
    bool flag = true;

    if ( ( op != 31 ) || ( op2 != retVal) )
        flag = false;

    return flag;
}

#if defined TR_VERSION_0
void trVer (TrState *s0, TrState *s1, TrTape *t0, TrTape *t1, uint64_t *P, TrMemState *output) {
#include "TR0.c" 
#elif defined TR_VERSION_1
void trVer (TrState *s0, TrState *s1, uint64_t *P, TrMemState *output) {
#include "TR1.c"
#endif // TR VERSIONS
}

#if defined TR_VERSION_0
#include "TR0Util.c" 
#elif defined TR_VERSION_1
#include "TR1Util.c"
void recordLoad(TrState *s0, TrMemState *output) {
    // record the memory load
    output->wrong = 0;
    output->memType = 1;
    output->memAddr = $pc;
    output->memData = $instr;
}

void recordStore(uint64_t addr, uint64_t data, TrMemState *output) {
    output->wrong = 0;
    output->memType = 0;
    output->memAddr = addr;
    output->memData = data;
}
#endif // TR VERSIONS

