// include TrAsm/TrSim outputs before TR_TRANS_VER_H to get the #defines

#ifndef TR_TRANS_VER_H
#define TR_TRANS_VER_H 1

#include <stdint.h>
#include <stdbool.h>

// reduce verbosity of the transition verifiction function a bit
// wow this makes it look like perl.
#define $P P
#define $result result
#define $op1 op1
#define $op1s op1s
#define $op2 op2
#define $op2s op2s
#define $r1prev r1prev
#define $flag s1->flag
#define $flagprev s0->flag
#define $pc s0->pc
#define $instr s0->instr
#define $pcNext s1->pc
#define $iPtr0 s0->iTPtr
#define $aPtr0 s0->aTPtr
#define $iPtr1 s1->iTPtr
#define $aPtr1 s1->aTPtr
#define $iLength t0->size
#define $aLength t1->size
#define $tapeI t0->data
#define $tapeA t1->data
#define $s1R s1->regs
#define $s0R s0->regs
#define $TrWrong output->wrong

#define $addr0 prev->memAddr
#define $data0 prev->memData
#define $type0 prev->memType
#define $step0 prev->stepNum
#define $addr1 curr->memAddr
#define $data1 curr->memData
#define $type1 curr->memType
#define $step1 curr->stepNum
#define $wrong isWrong

#define instrFetch(X,Y) (Y[(int) X])
#define tapeFetch(X,Y) (Y[(int) X])
#define regFetch(X,Y) (Y[(int) X])
#define signedValue(X) (((X>=TR_SREGMASK) ? ((int64_t)X-(1+(int64_t)TR_REGMASK)) : ((int64_t)X)))
#define signedImmValue(X) (((X>=TR_SIMMMASK) ? ((int64_t)X-(1+(int64_t)TR_IMMMASK)) : ((int64_t)X)))
#define bitShiftL(X,Y) ((X << Y) & TR_REGMASK)
#define bitShiftR(X,Y) (X >> Y)
#define getOp(X)   (X >> (TR_IMMSIZE + 2*TR_NREGBITS + 1)) & TR_OP_INSTRMASK
#define getuImm(X) (X >> (TR_IMMSIZE + 2*TR_NREGBITS)) & 0x1
#define getreg1(X) (X >> (TR_IMMSIZE + TR_NREGBITS)) & TR_OP_REGMASK
#define getreg2(X) (X >> TR_IMMSIZE) & TR_OP_REGMASK
#define getImm(X)  X & TR_IMMMASK

#define $memTrans input->memTrans
#define $memTransS input->memTransSorted

struct TrState {
    uint64_t pc;
#ifdef TR_VERSION_0
    uint16_t iTPtr;
    uint16_t aTPtr;
#endif // TR_VERSION_0
    uint64_t instr;
    bool flag;
    uint64_t regs[TR_NREGS];
};
typedef struct TrState TrState;

struct TrMemState {
    bool wrong;
    uint8_t memType;
    uint32_t stepNum;
    uint64_t memAddr;
    uint64_t memData;
};
typedef struct TrMemState TrMemState;

struct TrTape {
    uint16_t size;
    uint64_t data[TR_TAPELEN_MAX];
};
typedef struct TrTape TrTape;

struct In {
    uint64_t pmem[TR_PROGSIZE];
    TrTape tape0;
    TrTape tape1;
    TrState transcript[TR_NUMSTEPS];
    uint64_t returnValue;
    TrMemState memTrans[2*TR_BENES_SWITCHES];
    TrMemState memTransSorted[2*TR_BENES_SWITCHES];
    bool benesRoute[TR_BENES_STAGES * TR_BENES_SWITCHES];
};

#include "computeStructOut.h"
// functions

void compute(struct In *input, struct Out *output);
void benesPermute(TrMemState *list, bool *route, int offset, int bSteps);
bool memStateCompare(TrMemState *in1, TrMemState *in2);
bool stateCompare(TrState *in, uint64_t instr);
bool memVer(TrMemState *prev, TrMemState *curr);
bool retVer(uint64_t retVal, TrState *s0, uint64_t P);

#if defined TR_VERSION_0
void trVer(TrState *s0, TrState *s1, TrTape *t0, TrTape *t1, uint64_t *P, TrMemState *output);
#elif defined TR_VERSION_1
void trVer(TrState *s0, TrState *s1, uint64_t *P, TrMemState *output);
void recordLoad(TrState *s0, TrMemState *output);
void recordStore(uint64_t addr, uint64_t data, TrMemState *output);
#endif

#endif  // TR_TRANS_VER_H

// include TrAsm/TrSim outputs after TR_TRANS_VER_H to get the declarations
