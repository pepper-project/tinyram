
//#include <stdbool.h>
#include <stdint.h>

#define TR_PROGSIZE 128
#define TR_TAPELEN_MAX 10
#define TR_OP_INSTRMASK 0x1F
#define TR_OP_REGMASK 0x1F
#define TR_NREGBITS 5
#define TR_NREGS 32
#define TR_REGSIZE 32
#define TR_SREGMASK ((uint64_t) 0x80000000)
#define TR_REGMASK ((uint64_t) 0xFFFFFFFF)

#define getOp(X)   (X >> (TR_REGSIZE + 2*TR_NREGBITS + 1)) & TR_OP_INSTRMASK
#define getuImm(X) (X >> (TR_REGSIZE + 2*TR_NREGBITS)) & 0x1
#define getreg1(X) (X >> (TR_REGSIZE + TR_NREGBITS)) & TR_OP_REGMASK
#define getreg2(X) (X >> TR_REGSIZE) & TR_OP_REGMASK
#define getImm(X)  X & TR_REGMASK

#define $P input->P
#define $result result
#define $op1 op1
#define $op2 op2
#define $r1prev r1prev
#define $flag input->flag1
#define $flagprev input->flag0
#define $pc input->pc0
#define $pcNext input->pc1
#define $iPtr0 input->iTPtr0
#define $aPtr0 input->aTPtr0
#define $iPtr1 input->iTPtr1
#define $aPtr1 input->aTPtr1
#define $iLength input->tapeILen
#define $aLength input->tapeALen
#define $tapeI input->tapeI
#define $tapeA input->tapeA
#define $s1R input->regs1
#define $s0R input->regs0

#define false 0
#define true 1

struct In {
    // take in the contents of two successive states plus the program and tapes
    uint64_t P[TR_PROGSIZE];    // program is a bunch of packed integers
    uint64_t tapeI[TR_TAPELEN_MAX];
    uint16_t tapeILen;
    uint64_t tapeA[TR_TAPELEN_MAX];
    uint16_t tapeALen;

    uint16_t stepNum;

    uint64_t pc0;               // from-state
    bool flag0;
    uint64_t regs0[TR_NREGS];
    uint16_t iTPtr0;
    uint16_t aTPtr0;

    uint64_t pc1;               // to-state
    bool flag1;
    uint64_t regs1[TR_NREGS];
    uint16_t iTPtr1;
    uint16_t aTPtr1;
};

struct Out {
    bool wrong;
    uint64_t memAddr;           // address read/written
    uint64_t memData;           // data read/written
    uint8_t memType;            // type of operation (0=store, 1=load, 2=neither)
};

void compute (struct In *input, struct Out *output);
uint64_t instrFetch ( uint64_t pc, uint64_t *P );
uint64_t tapeFetch ( uint16_t tPtr, uint64_t *tape );
int64_t signedValue ( uint64_t value );
uint64_t bitShiftL ( uint64_t value, uint16_t shift );
uint64_t bitShiftR ( uint64_t value, uint16_t shift );

