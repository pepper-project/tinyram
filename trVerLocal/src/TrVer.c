#include <stdio.h>
#include <dlfcn.h>
#include <inttypes.h>
#include <stdbool.h>

// output structure so that we can access the result of execution
#include "computeStructOut.h"

// verify each execution by loading the dynamic library and then running the verifier

int main (const int argc, const char **argv) {
    char *err;
    int i;
    void *input;
    struct Out *output;
    void (*compute) (void *, struct Out *);
    void *dlptr;
    
    for (i=1;i<argc;i++)  {
        // build the input structure
        dlptr = dlopen(argv[i],RTLD_LAZY);
        if ((err = dlerror()) != NULL) {
            printf("Error in iteration %d: %s\n",i,err);
            continue;
        }

        input = dlsym(dlptr,"input");
        output = (struct Out *) dlsym(dlptr,"output");
        compute = dlsym(dlptr,"compute");
        if ((err = dlerror()) != NULL) {
            printf("Error in iteration %d: %s\n",i,err);
            dlclose(dlptr);
            continue;
        }

        (*compute)(input, output);

        if (output->correct) {
            printf("Execution %s verified\n",argv[i]);
        } else {
#ifdef COMPUTE_STRUCT_OUT_FAILSTEP
            printf("Execution %s failed verification at step %d\n",argv[i],output->failStep);
#else
            printf("Execution %s failed verification\n",argv[i]);
#endif // COMPUTE_STRUCT_OUT_FAILSTEP
        }

        dlclose(dlptr);
    }

    return 0;
}
