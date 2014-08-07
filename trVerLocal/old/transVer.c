
    uint64_t arith;
    uint64_t op2;
    uint64_t op1 = $op1expr;
    uint64_t r1prev = $r1prevexpr;
    uint64_t result = $resultexpr;
    unsigned int i;

#ifdef  TR_VECSIZE
    uint64_t arithV[TR_VECSIZE];
    uint64_t op2V[TR_VECSIZE];
    uint64_t op1V[TR_VECSIZE];
#endif  // TR_VECSIZE

    // get 2nd operand
#ifdef  TR_VECSIZE
#ifdef  TR_VEC_DIVOPS
    if ( $uImm || ($inst == 23) || ($inst == 24) || ($inst == 25) || ($inst == 26) || ($inst == 27) || ($inst == 32) || ($inst == 33) || ($inst == 34) )
#else   // TR_VEC_DIVOPS
    if ( $uImm || ($inst == 23) || ($inst == 24) || ($inst == 25) || ($inst == 26) || ($inst == 27) || ($inst == 32) )
#endif  // TR_VEC_DIVOPS
#else   // TR_VECSIZE
    if ($uImm) 
#endif  // TR_VECSIZE
        op2 = $reg3;
    else
        op2 = regFetch((uint16_t) $reg3, $s0R);

#ifdef  TR_VECSIZE
    // verify that registers are consistent
    if ( ($inst == 23) || ($inst == 24) || ($inst == 25) || ($inst == 32) ) {
    // output is 2 vectors for ADDV, UMULV, SMULV, SUBV
        for (i=0;i<TR_NREGS;++i)
        if (! ( ($reg1 == (i / TR_VECSIZE)) || ($reg1 == (1 + (i / TR_VECSIZE))) || (regFetch(i, $s0R) == regFetch(i, $s1R)) ) )
            ret.isLoad = true;
#ifdef  TR_VEC_DIVOPS
    } else if ( ($inst == 27) || ($inst == 33) || ($inst == 34) ) {
#else   // TR_VEC_DIVOPS
    } else if ($inst == 27) {
#endif  // TR_VEC_DIVOPS
    // output is just 1 vector for LOADV, DIVV, MODV
        for (i=0;i<TR_NREGS;++i)
        if (! ( ($reg1 == (i / TR_VECSIZE)) || (regFetch(i, $s0R) == regFetch(i, $s1R)) ) )
            ret.isLoad = true;
    } else {
    // all registers except reg1 should be the same
#endif  // TR_VECSIZE
        for (i=0;i<TR_NREGS;++i)
        if (! ( ($reg1 == i) || (regFetch(i, $s0R) == regFetch(i, $s1R)) ) )
            ret.isLoad = true;
#ifdef  TR_VECSIZE
    }
#endif  // TR_VECSIZE

    // 20, 21, 22 are jumps; PC is checked below
    // 31 is ANSWER, which halts the machine
    // NOTE we expect to have many ANSWER states in a row in a computation
    // since it can end sooner than MAXITER
    if ( ($inst != 20) && ($inst != 21) && ($inst != 22) && ($inst != 31) && (s1.pc != (s0.pc + 1)) )
        ret.isLoad = true;

    // all instructions except READ must leave the tape pointers untouched
    if ( ($inst != 30) && ( (s0.iTPtr != s1.iTPtr) || (s0.aTPtr != s1.aTPtr) ) )
        ret.isLoad = true;

    if ($inst == 0) {               // AND
        if ( ($result != ($op1 & $op2)) || ($flag != ($result == 0)) )
            ret.isLoad = true;
    } else if ($inst == 1) {        // OR
        if ( ($result != ($op1 | $op2)) || ($flag != ($result == 0)) )
            ret.isLoad = true;
    } else if ($inst == 2) {        // XOR
        if ( ($result != ($op1 ^ $op2)) || ($flag != ($result == 0)) )
            ret.isLoad = true;
    } else if ($inst == 3) {        // NOT
        if ( ($result != (~$op2 & TR_REGMASK)) || ($flag != ($result == 0)) )
            ret.isLoad = true;
    } else if ($inst == 4) {        // ADD
        arith = $op1 + $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            ret.isLoad = true;
    } else if ($inst == 5) {        // SUB
        arith = $op1 - $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            ret.isLoad = true;
    } else if ($inst == 6) {        // MULL
        arith = $op1 * $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            ret.isLoad = true;
    } else if ($inst == 7) {        // UMULH
        arith = ( ($op1 * $op2) >> TR_REGSIZE ) & TR_REGMASK;
        if ( ($result != arith) || ($flag != ($result != 0)) )
            ret.isLoad = true;
    } else if ($inst == 8) {        // SMULH
        arith = ( (signedValue($op1) * signedValue($op2)) >> TR_REGSIZE ) & TR_REGMASK;
        if ( ($result != arith) || ($flag != ( ($result != 0) && ($result != TR_REGMASK) )) )
            ret.isLoad = true;
    } else if ($inst == 9) {        // UDIV
        if ($op2 == 0) {
            if ( ($flag != true) || ($result != 0) )
                ret.isLoad = true;
        } else {
            arith = $op1 / $op2;
            if ( ($flag != false) || ($result != arith) )
                ret.isLoad = true;
        }
    } else if ($inst == 10) {       // UMOD
        if ($op2 == 0) {
            if ( ($flag != true) || ($result != 0) )
                ret.isLoad = true;
        } else {
            arith = $op1 % $op2;
            if ( ($flag != false) || ($result != arith) )
                ret.isLoad = true;
        }
    } else if ($inst == 11) {       // SHL
        arith = bitShiftL( $op1, (uint16_t) $op2 );
        if ( ($result != arith) || ((($op1 & TR_SREGMASK) != 0) != $flag) )
            ret.isLoad = true;
    } else if ($inst == 12) {       // SHR
        arith = bitShiftR( $op1, (uint16_t) $op2 );
        if ( ($result != arith) || ((($op1 & 1) != 0) != $flag) )
            ret.isLoad = true;
    } else if ($inst == 13) {       // CMPE
        if ( ($result != $r1prev) || ($flag != ($op1 == $op2)) )
            ret.isLoad = true;
    } else if ($inst == 14) {       // CMPA (unsigned >)
        if ( ($result != $r1prev) || ($flag != ($op1 > $op2)) )
            ret.isLoad = true;
    } else if ($inst == 15) {       // CMPAE (unsigned >=)
        if ( ($result != $r1prev) || ($flag != ($op1 >= $op2)) )
            ret.isLoad = true;
    } else if ($inst == 16) {       // CMPG (signed >)
        if ( ($result != $r1prev) || ($flag != (signedValue($op1) > signedValue($op2))) )
            ret.isLoad = true;
    } else if ($inst == 17) {       // CMPGE (signed >=)
        if ( ($result != $r1prev) || ($flag != (signedValue($op1) >= signedValue($op2))) )
            ret.isLoad = true;
    } else if ($inst == 18) {       // MOV
        if ( ($result != $op2) || ($flag != $flagprev) )
            ret.isLoad = true;
    } else if ($inst == 19) {       // CMOV
        if ( $flagprev == true ) {
            if ( ($result != $op2) || ($flag == false) )
                ret.isLoad = true;
        } else {
            if ( ($result != $r1prev) || ($flag == true) )
                ret.isLoad = true;
        }
    } else if ($inst == 20) {       // JMP
        if ( (s1.pc != $op2) || ($result != $r1prev) || ($flag != $flagprev) )
            ret.isLoad = true;
    } else if ($inst == 21) {       // CJMP
        if ($result != $r1prev)
            ret.isLoad = true;
        if ( $flagprev == true ) {
            if ( (s1.pc != $op2) || ($flag == false) )
                ret.isLoad = true;
        } else {
            if ( (s1.pc != (s0.pc + 1)) || ($flag == true) )
                ret.isLoad = true;
        }
    } else if ($inst == 22) {       // CNJMP
        if ($result != $r1prev)
            ret.isLoad = true;
        if ( $flagprev == true ) {
            if ( (s1.pc != (s0.pc + 1)) || ($flag == false) )
                ret.isLoad = true;
        } else {
            if ( (s1.pc != $op2) || ($flag == true) )
                ret.isLoad = true;
        }
#ifdef  TR_VECSIZE
    } else if ($inst == 23) {       // ADDV - vector addition
        if ($uImm) {
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = op1V[i] + $reg3;
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = op1V[i] + op2V[i];
        }

        regFetchV($reg1, $s1R, op2V);
        regFetchV($reg1+1, $s1R, op1V);
        for (i=0;i<TR_VECSIZE;++i)
        if ( (op2V[i] != (arithV[i] & TR_REGMASK)) || (op1V[i] != ((arithV[i] >> TR_REGSIZE) & TR_REGMASK)) )
            ret.isLoad = true;

        op2V[0] = false;
        for (i=0;i<TR_VECSIZE;++i)
            op2V[0] = ( op2V[0] || (op1V[i] != 0) );

        if ( $flag != op2V[0] )
            ret.isLoad = true;
    } else if ($inst == 24) {       // UMULV - unsigned vector multiply
        if ($uImm) {
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = op1V[i] * $reg3;
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = op1V[i] * op2V[i];
        }

        regFetchV($reg1, $s1R, op2V);
        regFetchV($reg1+1, $s1R, op1V);
        for (i=0;i<TR_VECSIZE;++i)
        if ( (op2V[i] != (arithV[i] & TR_REGMASK)) || (op1V[i] != ((arithV[i] >> TR_REGSIZE) & TR_REGMASK)) )
            ret.isLoad = true;

        op2V[0] = false;
        for (i=0;i<TR_VECSIZE;++i)
            op2V[0] = ( op2V[0] || (op1V[i] != 0) );

        if ( $flag != op2V[0] )
            ret.isLoad = true;
    } else if ($inst == 25) {       // SMULV - signed vector multiply
        if ($uImm) {
            op2V[0] = signedValue($reg3);
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = signedValue(op1V[i]) * op2V[0];
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i)
                arithV[i] = signedValue(op1V[i]) * signedValue(op2V[i]);
        }

        regFetchV($reg1, $s1R, op2V);
        regFetchV($reg1+1, $s1R, op1V);
        for (i=0;i<TR_VECSIZE;++i)
        if ( (op2V[i] != (arithV[i] & TR_REGMASK)) || (op1V[i] != ((arithV[i] >> TR_REGSIZE) & TR_REGMASK)) )
            ret.isLoad = true;

        op2V[0] = false;
        for (i=0;i<TR_VECSIZE;++i)
            op2V[0] = ( op2V[0] || ( (op1V[i] != 0) && (op1V[i] != TR_REGMASK) ) );

        if ( $flag != op2V[0] )
            ret.isLoad = true;
    } else if ($inst == 32) {       // SUBV - vector subtract
        op2 = 0;
        if ($uImm) {
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i) {
                arithV[i] = op1V[i] - $reg3;
                op2 = op2 || (op1V[i] < $reg3);
            }
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i) {
                arithV[i] = op1V[i] - op2V[i];
                op2 = op2 || (op1V[i] < op2V[i]);
            }
        }

        regFetchV($reg1, $s1R, op2V);
        regFetchV($reg1+1, $s1R, op1V);
        for (i=0;i<TR_VECSIZE;++i)
        if ( (op2V[i] != ( arithV[i] & TR_REGMASK )) || (op1V[i] != ((arithV[i] >> TR_REGSIZE) & TR_REGMASK)) )
            ret.isLoad = true;

        if ( $flag != op2 )
            ret.isLoad = true;
#ifdef  TR_VEC_DIVOPS
    } else if ($inst == 33) {       // DIVV - vector divide
        if ($uImm) {
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i) {
                if ($reg3 == 0) {
                    arithV[i] = 0;
                    op2 = true;
                } else {
                    arithV[i] = op1V[i] / $reg3;
                }
            }
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i) {
                if (op2V[i] == 0) {
                    arithV[i] = 0;
                    op2 = true;
                } else {
                    arithV[i] = op1V[i] / op2V[i];
                }
            }
        }

        regFetchV($reg1, $s1R, op2V);
        for (i=0;i<TR_VECSIZE;i++)
        if ( op2V[i] != arithV[i] )
            ret.isLoad = true;

        if ( $flag != op2 )
            ret.isLoad = true;
    } else if ($inst == 34) {       // MODV - vector modulo
        if ($uImm) {
            regFetchV($reg2, $s0R, op1V);
            for (i=0;i<TR_VECSIZE;++i) {
                if ($reg3 == 0) {
                    arithV[i] = 0;
                    op2 = true;
                } else {
                    arithV[i] = op1V[i] % $reg3;
                }
            }
        } else {
            regFetchV($reg2, $s0R, op1V);
            regFetchV($reg3, $s0R, op2V);
            for (i=0;i<TR_VECSIZE;++i) {
                if (op2V[i] == 0) {
                    arithV[i] = 0;
                    op2 = true;
                } else {
                    arithV[i] = op1V[i] % op2V[i];
                }
            }
        }

        regFetchV($reg1, $s1R, op2V);
        for (i=0;i<TR_VECSIZE;i++)
        if ( op2V[i] != arithV[i] )
            ret.isLoad = true;

        if ( $flag != op2 )
            ret.isLoad = true;
    } else if ($inst == 26) {       // STORV - vector store - MEMORY CHECKS UNIMPLEMENTED FOR VECTOR OPS
        if ( ($result != $r1prev) || ($flag != $flagprev) )
            ret.isLoad = true;
    } else if ($inst == 27) {       // LOADV - vector load  - MEMORY CHECKS UNIMPLEMENTED FOR VECTOR OPS
        if ( $flag != $flagprev )
            ret.isLoad = true;
#endif  // TR_VEC_DIVOPS
#endif  // TR_VECSIZE
    } else if ($inst == 28) {       // STORE
        if ( ($result != $r1prev) || ($flag != $flagprev) ) {
            ret.isLoad = true;
        } else {
            ret.isMem = true;
            ret.addr = $op2;
            ret.data = $op1;
        }
    } else if ($inst == 29) {       // LOAD
        ret.isLoad = true;
        if ( $flag == $flagprev ) { // correct from an execution standpoint, now just make the memcheck struct
            ret.isMem = true;
            ret.addr = $op2;
            ret.data = $result;
        }
    } else if ($inst == 30) {       // READ
        if ($op2 == 0) {        // read from inTape
            if ( (s1.iTPtr != (s0.iTPtr + 1)) || (s1.aTPtr != s0.aTPtr) )
                ret.isLoad = true;
            if (s0.iTPtr < t0.size) { // successful read
                if ( ($result != tapeFetch(s0.iTPtr, t0.data)) || ($flag == true) )
                    ret.isLoad = true;
            } else {                    // unsuccessful read
                if ( ($result != 0 ) || ($flag == false) )
                    ret.isLoad = true;
            }
        } else if ($op2 == 1) { // read from auxTape
            if ( (s1.iTPtr != s0.iTPtr) || (s1.aTPtr != (s0.aTPtr + 1)) )
                ret.isLoad = true;
            if (s0.aTPtr < t1.size) { // successful read
                if ( ($result != tapeFetch(s0.aTPtr, t1.data)) || ($flag == true) )
                    ret.isLoad = true;
            } else {                    // unsuccessful read
                if ( ($result != 0 ) || ($flag == false) )
                    ret.isLoad = true;
            }
        } else {
            if ( ($flag == false) || ($result != 0) || (s0.iTPtr != s1.iTPtr) || (s0.aTPtr != s1.aTPtr) )
                ret.isLoad = true;
        }
    } else if ($inst == 31) {       // ANSWER - halts the machine, so the next state had better be identical!
        if ( (s0.pc != s1.pc) || ($result != $r1prev) || ($flag != $flagprev) )
            ret.isLoad = true;
    } else  {                       // any other instruction is invalid
        ret.isLoad = true;
    }

    // if we made it here, this transition must be correct

