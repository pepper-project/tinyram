/*
uint64_t tapeFetch ( uint16_t tPtr, uint64_t *tape ) {
    uint16_t i;
    uint64_t tapeVal;

    for (i=0;i<TR_TAPELEN_MAX;++i)
        if (tPtr == i)
            tapeVal = tape[i];

    return tapeVal;
}
*/

int64_t signedValue (uint64_t value) {
    int64_t retVal;

    if (value >= TR_SREGMASK)
        retVal = (int64_t) value - (1 + (int64_t) TR_REGMASK);
    else
        retVal = (int64_t) value;

    return retVal;
}

int64_t signedImmValue (uint64_t value) {
    int64_t retVal;

    if (value >= TR_SIMMMASK)
        retVal = (int64_t) value - (1 + (int64_t) TR_IMMMASK);
    else
        retVal = (int64_t) value;

    return retVal;
}

#ifdef TR_USEOP_11
uint64_t bitShiftL(uint64_t value, uint8_t shift) {
    uint64_t retVal = 0;
    uint16_t i;

    for (i=0;i<TR_REGSIZE;++i)
        if (i == shift)
            retVal = (value << i) & TR_REGMASK;

    return retVal;
}
#endif

