// scaffolding includes
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

// real includes
#include <stdint.h>
#define MAX_SIZE 32
struct In {uint16_t input[MAX_SIZE];};
struct Out {uint16_t output[MAX_SIZE];};

void compute(struct In *input, struct Out *output);

int main(int argc, char **argv) {
    int i;
    struct In foo = {{0,}};
    struct Out bar = {{0,}};

    for (i=0; i<MAX_SIZE; i++) {
        if (i < argc-1) {
            foo.input[i] = (uint16_t) atoi(argv[i+1]);
        } else {
            foo.input[i] = 0;
        }
        printf("%d ",foo.input[i]);
    }
    printf("\n");

    compute(&foo,&bar);

    for (i=0; i<MAX_SIZE; i++) {
        printf("%d ",bar.output[i]);
    }
    printf("\n");

    return 0;
}

void compute(struct In *input, struct Out *output) {
    int bPtr, ePtr, mPtr, lPtr, rPtr;
    int span;
    int i;
    bool out2in = false;
    uint16_t *dst, *src;

    for (span = 1; span < MAX_SIZE; span *= 2) {
        // MAX_SIZE had better be a power of 2!!!

        // out2in means we're going out->in and need to copy back at the end
        if (out2in) {
            src = output->output;
            dst = input->input;
        } else {    // otherwise we're going input->output
            src = input->input;
            dst = output->output;
        }

        for (bPtr = 0; bPtr < MAX_SIZE; bPtr += 2*span) {
            lPtr = bPtr;
            mPtr = lPtr + span;
            rPtr = mPtr;
            ePtr = rPtr + span;

            for (i=lPtr; i<ePtr; i++) {
                if ( (lPtr < mPtr) && ( (rPtr >= ePtr) || (src[lPtr] < src[rPtr]) ) ) {
                    dst[i] = src[lPtr++];
                } else {
                    dst[i] = src[rPtr++];
                }
            }
        }

        out2in = ! out2in;
    }

    if (!out2in) {  // note, !out2in here because it was negated just above
        for (i=0; i<MAX_SIZE; i++) {
            output->output[i] = input->input[i];
        }
    }
}

