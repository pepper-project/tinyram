#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <unistd.h>
#include <vector>

#define BUFFER_SIZE 1024

void realloc_buffer(int size, char *buffer);

int main(void) {
    int i = 0;
    int nRead;
    char buf[BUFFER_SIZE];
    int inStrSize = 2 * BUFFER_SIZE;
    char *inString = (char *) malloc(inStrSize*sizeof(char));

    char *tok, *stok;
    char *saveptr1, *saveptr2, *str1, *str2;
    int *dst;
    int length = 0;
    int numflips = 0;
    std::vector<int> input;
    int output = 0;

    // slurp input
    while (0 < (nRead = read(STDIN_FILENO, buf, BUFFER_SIZE))) {
        if (i + nRead > inStrSize) {    // reallocate to make more room
            inStrSize *= 2;
            realloc_buffer(inStrSize, inString);
        }
        memcpy(inString + i, buf, nRead);
        i += nRead;
    }
    
    // null terminate
    if (i + 1 > inStrSize) {
        inStrSize += 1;
        realloc_buffer(inStrSize, inString);
    }
    inString[i] = '\0';

    for (i = -2, str1 = inString ; ; i++, str1 = NULL) {
        tok = strtok_r(str1, " []", &saveptr1);

        if (i == length || NULL == tok) { break; }

        int tmp;
        if (i == -2) {
            dst = &length;
        } else if (i == -1) {
            dst = &numflips;
        } else {
            dst = &tmp;
        }

        // tokenize rational, e.g., 5%2
        for (str2 = tok ; ; str2 = NULL) {
            stok = strtok_r(str2, " %", &saveptr2);

            if (NULL == stok) { break; }

            if (str2 != NULL) {
                *dst = (int) atoi(stok);
            } else {
                *dst /= (int) atoi(stok);
            }
        }

        if (i < 0) {
            input.reserve(length);
        } else {
            input.push_back(tmp);
            if (tmp < 0 || tmp > length) {
                printf("elements must be in [1, %d] for length %d\n", length, length);
                exit(-1);
            }
        }
    }

    // done with the input string
    free(inString);

    for (i = 0; i < numflips; i++) {
        std::vector<int> stack(input);
        int nflips = 0;

        while (stack[0] != 1) {
            std::reverse(stack.begin(), stack.begin() + stack[0]);
            nflips++;
        }

        output = nflips > output ? nflips : output;

        std::next_permutation(input.begin(), input.end());
    }

    printf("%d\n", output);
    fprintf(stderr, "\n\n** %d **\n\n", output);
    return 0;
}

void realloc_buffer(int size, char *buffer) {
    if (NULL == (buffer = (char *) realloc(buffer, size))) {
        perror("failed to realloc buffer");
        exit(-1);
    }
}
