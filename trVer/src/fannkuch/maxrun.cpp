#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <iostream>
#include <unistd.h>
#include <vector>

#define BUFFER_SIZE 1024

void realloc_buffer(int size, char *buffer);
bool vec_eq(std::vector<int> &a, std::vector<int> &b);
int do_flips(const std::vector<int> &input);

int main(int argc, char **argv) {
    std::vector<int> input;
    int seqlen;
    int numflips;
    if (argc < 3) {
        printf("Usage: %s <sequence_length> <num_flips>\n", argv[0]);
        exit(-1);
    }

    seqlen = atoi(argv[1]);
    numflips = atoi(argv[2]);

    if (seqlen < 1 || numflips < 1) {
        printf("Invalid sequence length or #flips.\n");
        exit(-1);
    } else {
        printf("Sequence length: %d\n", seqlen);
    }

    input.reserve(seqlen);
    for(int i=1; i<=seqlen; i++) {
        input.push_back(i);
    }

    std::vector<int> orig(input);
    std::vector<int> max;
    std::deque<int> lastN(numflips,0);
    int maxtot = 0;
    int currtot = 0;
    do {
        std::vector<int> stack(input);
        int nflips = do_flips(input);

        int oldfront = lastN[0];
        lastN.pop_front();
        lastN.push_back(nflips);
        currtot -= oldfront;
        currtot += nflips;
        if (currtot > maxtot) {
            maxtot = currtot;
            max = input;
        }

        std::next_permutation(input.begin(), input.end());
    } while (! vec_eq(orig, input));

    printf("Total flips in %d permutations: %d\n", numflips, maxtot);
    for(int j=1; j<numflips; j++) {
        std::prev_permutation(max.begin(), max.end());
    }
    printf("[ ");
    for(unsigned i=0; i<max.size(); i++) {
        printf("%d ", max[i]);
    }
    printf("]\n");
    return 0;
}

void realloc_buffer(int size, char *buffer) {
    if (NULL == (buffer = (char *) realloc(buffer, size))) {
        perror("failed to realloc buffer");
        exit(-1);
    }
}

bool vec_eq(std::vector<int> &a, std::vector<int> &b) {
    if (a.size() != b.size()) { return false; }
    for(unsigned i=0; i<a.size(); i++) {
        if (a[i] != b[i]) { return false; }
    }
    return true;
}

int do_flips(const std::vector<int> &input) {
    std::vector<int> stack(input);
    int nflips = 0;

    while (stack[0] != 1) {
        std::reverse(stack.begin(), stack.begin() + stack[0]);
        nflips++;
    }

    return nflips;
}
