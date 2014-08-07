
//#include <stdbool.h>
#include <stdint.h>

#define $in0 input->in0
#define $in1 input->in1
#define $out0 output->out0
#define $out1 output->out1
#define $swap input->swap

#define false 0
#define true 1

struct TrMemState {
    uint64_t address;
    uint64_t data;
    uint8_t memType;
    uint16_t stepNum;
};
typedef struct TrMemState TrMemState;

struct In {
    TrMemState in0;
    TrMemState in1;
    bool swap;
};

struct Out {
    TrMemState out0;   // selected value
    TrMemState out1;
};

void compute (struct In *input, struct Out *output);

