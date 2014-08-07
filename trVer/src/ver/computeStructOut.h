#ifndef COMPUTE_STRUCT_OUT
#define COMPUTE_STRUCT_OUT 1

struct Out {
    bool correct;
    uint64_t retVal;
#if TR_AUXTAPELEN > 0
    uint64_t tape1[TR_AUXTAPELEN];
#endif // TR_AUXTAPELEN
};

#endif // COMPUTE_STRUCT_OUT
