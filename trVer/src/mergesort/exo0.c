#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>
#include <string.h>
#include <stdint.h>

#define BUFFER_SIZE 1024
#define BUFFER_CHUNK 10*BUFFER_SIZE

void compute(int *input, int *output, int length);

int main(int argc, char **argv) {
    int i = 0;
    int nRead;
    char buf[BUFFER_SIZE];
    int inStrSize = BUFFER_CHUNK;
    char *inString = (char *) malloc(inStrSize*sizeof(char));
    char *tok, *stok;
    char *saveptr1, *saveptr2, *str1, *str2;

    int length = 0;
    int *dst;
    int *input;
    int *output;

    if ((argc == 0) || (argv == 0)) { exit(-1); }

    while (0 < (nRead = read(STDIN_FILENO, buf, BUFFER_CHUNK))) {
        if (i + nRead > inStrSize) { // assumes that BUFFER_CHUNK 
            inStrSize += BUFFER_CHUNK;
            if (NULL == (inString = (char *) realloc(inString, inStrSize))) {
                perror("failed to realloc inString");
                exit(-1);
            }
        }
        memcpy(inString + i, buf, nRead);
        i += nRead;
    }
    inString[i] = '\0';  // null terminate

    // tokenize on spaces, braces
    for (i=-1, str1 = inString ; ; i++, str1 = NULL) {
        tok = strtok_r(str1, " []", &saveptr1);

        if (i == length || NULL == tok) { break; }

        if (i < 0) {
            dst = &length;
        } else {
            dst = &(input[i]);
        }

        // tokenize on rational notation, e.g., 5%2
        // note that we turn these into integers!
        for (str2 = tok; ; str2 = NULL) {
            stok = strtok_r(str2, " %", &saveptr2);

            if (NULL == stok) { break; }

            if (str2 != NULL) {
                *dst = (int) atoi(stok);
            } else {
                *dst /= (int) atoi(stok);
            }
        }

        if (i < 0) {
            input = (int *) calloc(length, sizeof(int));
            output = (int *) calloc(length, sizeof(int));
        }
    }

    free(inString);

    compute(input, output, length);

    fprintf(stderr, "\n");
    for (i=0; i<length; i++) {
        printf("%u ", output[i]);
        fprintf(stderr, "%u ", output[i]);
    }
    fprintf(stderr, "\n");

    free(input);
    free(output);

    return 0;
}

void compute(int *input, int *output, int length) {
    int bPtr, ePtr, mPtr, lPtr, rPtr;
    int span;
    int i;
    bool out2in = false;
    int *dst, *src;

    for (span = 1; span < length; span *= 2) {
        // length had better be a power of 2!!!

        // out2in means we're going out->in and need to copy back at the end
        if (out2in) {
            src = output;
            dst = input;
        } else {    // otherwise we're going input->output
            src = input;
            dst = output;
        }

        for (bPtr = 0; bPtr < length; bPtr += 2*span) {
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
        for (i=0; i<length; i++) {
            output[i] = input[i];
        }
    }
}
