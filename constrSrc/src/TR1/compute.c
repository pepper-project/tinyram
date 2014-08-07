
    uint16_t op = getOp($instr);
    bool uImm = getuImm($instr);
    uint16_t reg1 = getreg1($instr);
    uint16_t reg2 = getreg2($instr);
    uint64_t imm = getImm($instr);

    int i;
    bool r1ne0 = reg1 != 0;
    bool validOp = false;

#ifdef TR_USEOP_0
    bool opIs0 = op == 0;
    validOp = validOp | opIs0;
#endif
#ifdef TR_USEOP_1
    bool opIs1 = op == 1;
    validOp = validOp | opIs1;
#endif
#ifdef TR_USEOP_2
    bool opIs2 = op == 2;
    validOp = validOp | opIs2;
#endif
#ifdef TR_USEOP_3
    bool opIs3 = op == 3;
    validOp = validOp | opIs3;
#endif
#ifdef TR_USEOP_4
    bool opIs4 = op == 4;
    validOp = validOp | opIs4;
#endif
#ifdef TR_USEOP_5
    bool opIs5 = op == 5;
    validOp = validOp | opIs5;
#endif
#ifdef TR_USEOP_6
    bool opIs6 = op == 6;
    validOp = validOp | opIs6;
#endif
#ifdef TR_USEOP_7
    bool opIs7 = op == 7;
    validOp = validOp | opIs7;
#endif
#ifdef TR_USEOP_8
    bool opIs8 = op == 8;
    validOp = validOp | opIs8;
#endif
#ifdef TR_USEOP_9
    bool opIs9 = op == 9;
    validOp = validOp | opIs9;
#endif
#ifdef TR_USEOP_10
    bool opIs10 = op == 10;
    validOp = validOp | opIs10;
#endif
#ifdef TR_USEOP_11
    bool opIs11 = op == 11;
    validOp = validOp | opIs11;
#endif
#ifdef TR_USEOP_12
    bool opIs12 = op == 12;
    validOp = validOp | opIs12;
#endif
#ifdef TR_USEOP_20
    bool opIs20 = op == 20;
    validOp = validOp | opIs20;
#endif
#ifdef TR_USEOP_21
    bool opIs21 = op == 21;
    validOp = validOp | opIs21;
#endif
#ifdef TR_USEOP_22
    bool opIs22 = op == 22;
    validOp = validOp | opIs22;
#endif
#ifdef TR_USEOP_28
    bool opIs28 = op == 28;
    validOp = validOp | opIs28;
#endif
#ifdef TR_USEOP_29
    bool opIs29 = op == 29;
    validOp = validOp | opIs29;
#endif

    // we always use ANSWER
    bool opIs31 = op == 31;
    validOp = validOp | opIs31;

    uint64_t arith;
#if defined (TR_USEOP_5) || defined (TR_USEOP_12)
    uint64_t arithSubtract;
#endif
#if defined (TR_USEOP_6) || defined (TR_USEOP_7) || defined (TR_USEOP_8)
    uint64_t arithMult;
#endif
    uint64_t op1;
#if defined (TR_USEOP_12) || defined (TR_USEOP_8)
    int64_t op1s;
#endif
    uint64_t result;
    uint64_t r1prev;
    uint64_t op2;
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22) || defined (TR_USEOP_28) || defined (TR_USEOP_29) || defined (TR_USEOP_12) || defined (TR_USEOP_8)
    int64_t op2s;
#endif
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22)
    uint64_t jumpTarg;
#endif
#if defined (TR_USEOP_28) || defined (TR_USEOP_29)
    uint64_t memAddr;
#endif
    bool flagExpect = $flagprev;
    uint64_t pcNextExpect = $pc + 1;
    //uint16_t iPtrExpect = $iPtr0;
    //uint16_t aPtrExpect = $aPtr0;
#if defined (TR_USEOP_9) || defined (TR_USEOP_10) //|| defined (TR_USEOP_30)
    bool op2Is0;
#endif

    // default: correct, non-mem access
    $TrWrong = false;
    output->memAddr = 0;
    output->memData = 0;
    output->memType = 2;

    // check and fetch registers
    if ( $s0R[0] != 0 ) { $TrWrong = true; }

    for (i=0;i<TR_NREGS;++i) {
        if (reg1 == i) {
            result = $s1R[i];
            r1prev = $s0R[i];
        } else if ($s0R[i] != $s1R[i]) {
            $TrWrong = true;
        }

        if (reg2 == i) {
            op1 = $s0R[i];
        }

        if ((uint16_t) imm == i) {
            op2 = $s0R[i];
        }
    }

    // figure out signedness
#if defined (TR_USEOP_12) || defined (TR_USEOP_8)
    op1s = signedValue(op1);
#endif
    if (uImm) {
        op2 = imm;
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22) || defined (TR_USEOP_28) || defined (TR_USEOP_29) || defined (TR_USEOP_12) || defined (TR_USEOP_8)
        op2s = signedImmValue($op2);
#endif
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22)
        jumpTarg = ($pc + $op2s) & TR_REGMASK;
#endif
#if defined (TR_USEOP_28) || defined (TR_USEOP_29)
        memAddr = ($op1 + $op2s) & TR_REGMASK;
#endif
    } else {
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22) || defined (TR_USEOP_28) || defined (TR_USEOP_29) || defined (TR_USEOP_12) || defined (TR_USEOP_8)
        op2s = signedValue($op2);
#endif
#if defined (TR_USEOP_20) || defined (TR_USEOP_21) || defined (TR_USEOP_22)
        jumpTarg = $op2;
#endif
#if defined (TR_USEOP_28) || defined (TR_USEOP_29)
        memAddr = ($op1 + $op2) & TR_REGMASK;
#endif
    }

    arith = $r1prev;
#if defined (TR_USEOP_5) || defined (TR_USEOP_12)
    arithSubtract = $op1 - $op2;
#endif
#if defined (TR_USEOP_6) || defined (TR_USEOP_7)
    arithMult = $op1 * $op2;
#endif
#if defined (TR_USEOP_9) || defined (TR_USEOP_10) //|| defined (TR_USEOP_30)
    op2Is0 = $op2 == 0;
#endif

    // compute the arith value
    if (opIs31) { 
        pcNextExpect = $pc;
    }
#ifdef TR_USEOP_0
    if (opIs0) { 
        arith = $op1 & $op2;
        flagExpect = arith == 0;
    }
#endif
#ifdef TR_USEOP_1
    if (opIs1) {
        arith = $op1 | $op2;
        flagExpect = arith == 0;
    }
#endif
#ifdef TR_USEOP_2
    if (opIs2) {
        arith = $op1 ^ $op2;
        flagExpect = arith == 0;
    }
#endif
#ifdef TR_USEOP_3
    if (opIs3) {
        arith =      ~ $op2;
        flagExpect = arith == 0;
    }
#endif
#ifdef TR_USEOP_4
    if (opIs4) {
        arith = $op1 + $op2;
        flagExpect = arith > TR_REGMASK;
    }
#endif
#ifdef TR_USEOP_5
    if (opIs5) {
        arith = arithSubtract;
        flagExpect = $op1 < $op2;
    }
#endif
#ifdef TR_USEOP_12
    if (opIs12) {
        arith = arithSubtract;
        flagExpect = $op1s < $op2s;
    }
#endif
#ifdef TR_USEOP_6
    if (opIs6) {
        arith = arithMult;
        flagExpect = arith > TR_REGMASK;
    }
#endif
#ifdef TR_USEOP_7
    if (opIs7) {
        arith = ( arithMult >> TR_REGSIZE ) & TR_REGMASK;
        flagExpect = arith != 0;
    }
#endif
#ifdef TR_USEOP_8
    if (opIs8) {
        arithMult = $op1s * $op2s;
        arith = ( arithMult >> TR_REGSIZE ) & TR_REGMASK;
        flagExpect = !( ( (arith == 0) && (0 == (arithMult & TR_SREGMASK)) ) || 
                        ( (arith == TR_REGMASK) && (0 != (arithMult & TR_SREGMASK)) ) );
    }
#endif
#ifdef TR_USEOP_9
    if (opIs9) {
        if (op2Is0) {
            arith = 0;
            flagExpect = true;
        } else {
            arith = $op1 / $op2;
            flagExpect = false;
        }
    }
#endif
#ifdef TR_USEOP_10
    if (opIs10) {
        if (op2Is0) {
            arith = 0;
            flagExpect = true;
        } else {
            arith = $op1 % op2;
            flagExpect = false;
        }
    }
#endif
#ifdef TR_USEOP_11
    if (opIs11) {
        arith = bitShiftL( $op2, (uint8_t) $op1 );
        flagExpect = ($op2 & TR_SREGMASK) != 0;
    }
#endif
#ifdef TR_USEOP_20
    if (opIs20) {
        pcNextExpect = jumpTarg;
    }
#endif
#ifdef TR_USEOP_21
    if (opIs21) {
        if ($flagprev == true) {
            pcNextExpect = jumpTarg;
        }
    }
#endif
#ifdef TR_USEOP_22
    if (opIs22) {
        if ($flagprev == false) {
            pcNextExpect = jumpTarg;
        }
    }
#endif
#ifdef TR_USEOP_28
    if (opIs28) {
        output->memType = 0;
        output->memAddr = memAddr;
        output->memData = $r1prev;
    }
#endif
#ifdef TR_USEOP_29
    if (opIs29) {
        output->memType = 1;
        output->memAddr = memAddr;
        output->memData = $result;
        arith = $result;    // we assume result is correct; validity is checked by mem transcript
    }
#endif

    if ( (!validOp) ||
         ($flag != flagExpect) ||
         ($pcNext != pcNextExpect) ||
         (r1ne0 && ($result != (arith & TR_REGMASK))) ) {
        $TrWrong = true;
    }

