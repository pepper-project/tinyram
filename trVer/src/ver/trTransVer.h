// include TrAsm/TrSim outputs before TR_TRANS_VER_H to get the #defines

#ifndef TR_TRANS_VER_H
#define TR_TRANS_VER_H 1

#include <stdint.h>

// we don't need _Bool with zcc, just true and false
#ifdef VER_LOCAL
#include <stdbool.h>
#else
#define true 1
#define false 0
#endif // VER_LOCAL


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
#define $s1R s1->regs
#define $s0R s0->regs
#define $TrWrong TrWrong

#define $addr0 prev->memAddr
#define $data0 prev->memData
#define $type0 prev->memType
#define $step0 prev->stepNum
#define $addr1 curr->memAddr
#define $data1 curr->memData
#define $type1 curr->memType
#define $step1 curr->stepNum
#define $wrong isWrong

// TODO may need to tweak this to work with ZCC.
#define getOp(X)   (X >> (TR_IMMSIZE + 2*TR_NREGBITS + 1)) & TR_OP_INSTRMASK
#define getuImm(X) (X >> (TR_IMMSIZE + 2*TR_NREGBITS)) & 0x1
#define getreg1(X) (X >> (TR_IMMSIZE + TR_NREGBITS)) & TR_OP_REGMASK
#define getreg2(X) (X >> TR_IMMSIZE) & TR_OP_REGMASK
#define getImm(X)  X & TR_IMMMASK

struct TrState {
    uint64_t pc;
    uint64_t instr;
    bool flag;
    uint64_t regs[TR_NREGS];
};
typedef struct TrState TrState;

struct TrMemState {
    uint8_t memType;
    uint16_t stepNum;
    uint64_t memAddr;
    uint64_t memData;
};
typedef struct TrMemState TrMemState;

struct In {
    uint64_t pmem[TR_PROGSIZE];
    uint64_t tape0[TR_INTAPELEN];
};

#include "computeStructOut.h"

// functions
void compute(struct In *input, struct Out *output);
void benesPermute(TrMemState *list, bool *route, int offset, int bSteps);
bool memStateCompare(TrMemState *in1, TrMemState *in2);
bool stateCompare(TrState *in, uint64_t instr);
bool memVer(TrMemState *prev, TrMemState *curr);
bool retVer(TrState *s0, uint64_t *retVal);

bool trVer(TrState *s0, TrState *s1, uint64_t *P, TrMemState *output);

void recordLoad(TrState *s0, TrMemState *output, uint16_t step);
void recordStore(uint64_t addr, uint64_t data, TrMemState *output, uint16_t step);

#ifdef VER_LOCAL

struct Exo {
    TrState transcript[TR_NUMSTEPS];
    uint32_t returnValue;
    TrMemState memTrans[2*TR_BENES_SWITCHES];
    TrMemState memTransSorted[2*TR_BENES_SWITCHES];
    bool benesRoute[TR_BENES_STAGES * TR_BENES_SWITCHES];
};

#define regFetch(X,Y) (Y[(int) X])
#define signedValue(X) (((X>=TR_SREGMASK) ? ((int64_t)X-(1+(int64_t)TR_REGMASK)) : ((int64_t)X)))
#define signedImmValue(X) (((X>=TR_SIMMMASK) ? ((int64_t)X-(1+(int64_t)TR_IMMMASK)) : ((int64_t)X)))
#ifdef TR_USEOP_11
#define bitShiftL(X,Y) ((X << Y) & TR_REGMASK)
#endif // TR_USEOP_11
// debugging
#include <stdio.h>
#include <inttypes.h>

#else  // VER_LOCAL

struct TrExoOutput {
    TrState transcript[TR_NUMSTEPS];
    bool benesRoute[TR_BENES_STAGES * TR_BENES_SWITCHES];
};
typedef struct TrExoOutput TrExoOutput;

uint64_t regFetch( uint16_t rPtr, uint64_t *regs );
int64_t signedValue ( uint64_t value );
int64_t signedImmValue (uint64_t value );
#ifdef TR_USEOP_11
uint64_t bitShiftL ( uint64_t value, uint8_t shift );
#endif // TR_USEOP_11
#endif // VER_LOCAL

#endif  // TR_TRANS_VER_H
// include TrAsm/TrSim outputs after TR_TRANS_VER_H to get the declarations
