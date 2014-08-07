
    uint64_t instr = instrFetch( $pc, $P );
    uint16_t op = getOp(instr);
    bool uImm = getuImm(instr);
    uint16_t reg1 = getreg1(instr);
    uint16_t reg2 = getreg2(instr);
    uint64_t imm = getImm(instr);

    int i;
    bool op00 = op == 0;
    bool op01 = op == 1;
    bool op02 = op == 2;
    bool op03 = op == 3;
    bool op18 = op == 18;
    bool op19 = op == 19;
    bool op20 = op == 20;
    bool op21 = op == 21;
    bool op22 = op == 22;
    bool op28 = op == 28;
    bool op29 = op == 29;
    bool op30 = op == 30;
    bool op31 = op == 31;

    uint64_t arith;
    uint64_t op1;
    uint64_t result;
    uint64_t r1prev;
    uint64_t op2;

    // default: correct, non-mem access
    output->wrong = false;
    output->memAddr = 0;
    output->memData = 0;
    output->memType = 2;

    // check and fetch registers
    for (i=0;i<TR_NREGS;++i) {
        if (reg1 == i) {
            result = $s1R[i];
            r1prev = $s0R[i];
        } else if ($s0R[i] != $s1R[i]) {
            output->wrong = true;
        }

        if (reg2 == i) {
            op1 = $s0R[i];
        }

        if ((uint16_t) imm == i) {
            op2 = $s0R[i];
        }
    }

    if (uImm)
        op2 = imm;

    if (! ( (op30) || ( ($iPtr0 == $iPtr1) && ($aPtr0 == $aPtr1) ) ) ) {
        output->wrong = true;
    }

    if (! ( op20 || op21 || op22 || op31 || ($pcNext == ($pc + 1)) ) ) {
        output->wrong = true;
    }

    if (op00 || op01 || op02 || op03) {
        if ($flag != ($result == 0)) { output->wrong = true; }
    }

    if (op18 || op19 || op20 || op21 || op22 || op28 || op29 || op31) {
        if ($flag != $flagprev) { output->wrong = true; }
    }

    if (op00) {               // AND
        if ($result != ($op1 & $op2)) { output->wrong = true; }
    } else if (op01) {        // OR
        if ($result != ($op1 | $op2)) { output->wrong = true; }
    } else if (op02) {        // XOR
        if ($result != ($op1 ^ $op2)) { output->wrong = true; }
    } else if (op03) {        // NOT
        if ($result != (~$op2 & TR_REGMASK)) { output->wrong = true; }
    } else if (op == 4) {        // ADD
        arith = $op1 + $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            output->wrong = true;
    } else if (op == 5) {        // SUB
        arith = $op1 - $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            output->wrong = true;
    } else if (op == 6) {        // MULL
        arith = $op1 * $op2;
        if ( ($result != (arith & TR_REGMASK)) || ($flag != ($result != arith)) )
            output->wrong = true;
    } else if (op == 7) {        // UMULH
        arith = ( ($op1 * $op2) >> TR_REGSIZE ) & TR_REGMASK;
        if ( ($result != arith) || ($flag != ($result != 0)) )
            output->wrong = true;
    } else if (op == 8) {        // SMULH
        arith = signedValue($op1) * signedValue($op2);
        if ( ($result != ((arith >> TR_REGSIZE) & TR_REGMASK)) || 
             ($flag == ( ( ($result == 0) && (0 == (arith & TR_SREGMASK)) ) || 
                         ( ($result == TR_REGMASK) && (0 != (arith & TR_SREGMASK)) ))))
            output->wrong = true;
    } else if (op == 9) {        // UDIV
        if ($op2 == 0) {
            if ( ($flag != true) || ($result != 0) )
                output->wrong = true;
        } else {
            arith = $op1 / $op2;
            if ( ($flag != false) || ($result != arith) )
                output->wrong = true;
        }
    } else if (op == 10) {       // UMOD
        if ($op2 == 0) {
            if ( ($flag != true) || ($result != 0) )
                output->wrong = true;
        } else {
            arith = $op1 % $op2;
            if ( ($flag != false) || ($result != arith) )
                output->wrong = true;
        }
    } else if (op == 11) {       // SHL
        arith = bitShiftL( $op1, (uint16_t) $op2 );
        if ( ($result != arith) || ((($op1 & TR_SREGMASK) != 0) != $flag) )
            output->wrong = true;
    } else if (op == 12) {       // SHR
        arith = bitShiftR( $op1, (uint16_t) $op2 );
        if ( ($result != arith) || ((($op1 & 1) != 0) != $flag) )
            output->wrong = true;
    } else if (op == 13) {       // CMPE
        if ( ($result != $r1prev) || ($flag != ($op1 == $op2)) )
            output->wrong = true;
    } else if (op == 14) {       // CMPA (unsigned >)
        if ( ($result != $r1prev) || ($flag != ($op1 > $op2)) )
            output->wrong = true;
    } else if (op == 15) {       // CMPAE (unsigned >=)
        if ( ($result != $r1prev) || ($flag != ($op1 >= $op2)) )
            output->wrong = true;
    } else if (op == 16) {       // CMPG (signed >)
        if ( ($result != $r1prev) || ($flag != (signedValue($op1) > signedValue($op2))) )
            output->wrong = true;
    } else if (op == 17) {       // CMPGE (signed >=)
        if ( ($result != $r1prev) || ($flag != (signedValue($op1) >= signedValue($op2))) )
            output->wrong = true;
    } else if (op18) {       // MOV
        if ($result != $op2) { output->wrong = true; }
    } else if (op19) {       // CMOV
        if ( $flagprev == true ) {
            if ($result != $op2)
                output->wrong = true;
        } else {
            if ($result != $r1prev)
                output->wrong = true;
        }
    } else if (op20) {       // JMP
        if ( ($pcNext != $op2) || ($result != $r1prev) )
            output->wrong = true;
    } else if (op21) {       // CJMP
        if ($result != $r1prev)
            output->wrong = true;
        if ( $flagprev == true ) {
            if ($pcNext != $op2)
                output->wrong = true;
        } else {
            if ($pcNext != ($pc + 1))
                output->wrong = true;
        }
    } else if (op22) {       // CNJMP
        if ($result != $r1prev)
            output->wrong = true;
        if ( $flagprev == true ) {
            if ($pcNext != ($pc + 1))
                output->wrong = true;
        } else {
            if ($pcNext != $op2)
                output->wrong = true;
        }
    } else if (op28) {       // STORE
        if ($result != $r1prev) {
            output->wrong = true;
        } else {
            output->memType = 0;
            output->memAddr = $op2;
            output->memData = $op1;
        }
    } else if (op29) {       // LOAD
        output->memType = 1;
        output->memAddr = $op2;
        output->memData = $result;
    } else if (op30) {       // READ
        if ($op2 == 0) {        // read from inTape
            if ( ($iPtr1 != ($iPtr0 + 1)) || ($aPtr1 != $aPtr0) )
                output->wrong = true;
            if ($iPtr0 < $iLength) { // successful read
                if ( ($result != tapeFetch($iPtr0, $tapeI)) || ($flag == true) )
                    output->wrong = true;
            } else {                    // unsuccessful read
                if ( ($result != 0 ) || ($flag == false) )
                    output->wrong = true;
            }
        } else if ($op2 == 1) { // read from auxTape
            if ( ($iPtr1 != $iPtr0) || ($aPtr1 != ($aPtr0 + 1)) )
                output->wrong = true;
            if ($aPtr0 < $aLength) { // successful read
                if ( ($result != tapeFetch($aPtr0, $tapeA)) || ($flag == true) )
                    output->wrong = true;
            } else {                    // unsuccessful read
                if ( ($result != 0 ) || ($flag == false) )
                    output->wrong = true;
            }
        } else {
            if ( ($flag == false) || ($result != 0) || ($iPtr0 != $iPtr1) || ($aPtr0 != $aPtr1) )
                output->wrong = true;
        }
    } else if (op31) {       // ANSWER - halts the machine, so the next state had better be identical!
        if ( ($pc != $pcNext) || ($result != $r1prev) )
            output->wrong = true;
    } else  {                       // any other instruction is invalid
        output->wrong = true;
    }

