
//#include <stdbool.h>
#include <stdint.h>

#define false 0
#define true 1

#define $addr0 input->memAddr0
#define $data0 input->memData0
#define $type0 input->memType0
#define $step0 input->memStep0

#define $addr1 input->memAddr1
#define $data1 input->memData1
#define $type1 input->memType1
#define $step1 input->memStep1

#define $wrong output->wrong

struct In {
    uint8_t memType0;
    uint64_t memAddr0;
    uint64_t memData0;
    uint16_t memStep0;

    uint8_t memType1;
    uint64_t memAddr1;
    uint64_t memData1;
    uint16_t memStep1;
};

struct Out {
    bool wrong;
};

void compute (struct In *input, struct Out *output);

