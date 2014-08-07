
/*#include <stdbool.h>*/
#include <stdint.h>

#define TR_TAPELEN_MAX 10
#define TR_OP_INSTRMASK 0x1F
#define TR_OP_REGMASK 0x1F
#define TR_NREGBITS 5
#define TR_NREGS 32
#define TR_REGSIZE 32
#define TR_SREGMASK ((uint64_t) 0x80000000)
#define TR_REGMASK  ((uint64_t) 0xFFFFFFFF)
#define TR_IMMSIZE 16
#define TR_SIMMMASK ((uint64_t) 0x00008000)
#define TR_IMMMASK  ((uint64_t) 0x0000FFFF)

#define TR_USEOP_0
#define TR_USEOP_1
#define TR_USEOP_2
#define TR_USEOP_3
#define TR_USEOP_4
#define TR_USEOP_5
#define TR_USEOP_6
#define TR_USEOP_7
#define TR_USEOP_8
#define TR_USEOP_9
#define TR_USEOP_10
#define TR_USEOP_11
#define TR_USEOP_12
#define TR_USEOP_20
#define TR_USEOP_21
#define TR_USEOP_22
#define TR_USEOP_28
#define TR_USEOP_29
#define TR_USEOP_31

#define getOp(X)   (X >> (TR_IMMSIZE + 2*TR_NREGBITS + 1)) & TR_OP_INSTRMASK
#define getuImm(X) (X >> (TR_IMMSIZE + 2*TR_NREGBITS)) & 0x1
#define getreg1(X) (X >> (TR_IMMSIZE + TR_NREGBITS)) & TR_OP_REGMASK
#define getreg2(X) (X >> TR_IMMSIZE) & TR_OP_REGMASK
#define getImm(X)  X & TR_IMMMASK

#define $instr input->instr
#define $result result
#define $op1 op1
#define $op1s op1s
#define $op2 op2
#define $op2s op2s
#define $r1prev r1prev
#define $flag input->flag1
#define $flagprev input->flag0
#define $pc input->pc0
#define $pcNext input->pc1
#define $s1R input->regs1
#define $s0R input->regs0
#define $TrWrong output->wrong

#define false 0
#define true 1

struct In {
    // take in the contents of two successive states plus the tapes
    /*
    uint64_t tapeI[TR_TAPELEN_MAX];
    uint16_t tapeILen;
    uint64_t tapeA[TR_TAPELEN_MAX];
    uint16_t tapeALen;
    */

    uint64_t instr;

    uint64_t pc0;               // from-state
    bool flag0;
    uint64_t regs0[TR_NREGS];
    /*
    uint16_t iTPtr0;
    uint16_t aTPtr0;
    */

    uint64_t pc1;               // to-state
    bool flag1;
    uint64_t regs1[TR_NREGS];
    /*
    uint16_t iTPtr1;
    uint16_t aTPtr1;
    */
};

struct Out {
    bool wrong;
    uint64_t memAddr;   // address read/written
    uint64_t memData;   // data read/written
    uint8_t memType;    // type of operation (0=store, 1=load, 2=neither)
};

void compute (struct In *input, struct Out *output);
//uint64_t tapeFetch ( uint16_t tPtr, uint64_t *tape );
int64_t signedValue ( uint64_t value );
int64_t signedImmValue (uint64_t value );
#ifdef TR_USEOP_11
uint64_t bitShiftL ( uint64_t value, uint8_t shift );
#endif

