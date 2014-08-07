uint64_t regFetch ( uint16_t rPtr, uint64_t *regs ) {
    uint16_t i;
    uint64_t regVal;

    for (i=0;i<TR_NREGS;++i)
        if (rPtr == i)
            regVal = regs[i];

    return regVal;
}
