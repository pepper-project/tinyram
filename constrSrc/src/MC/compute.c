
    if ($type0 == 2 && $type1 != 2) { // non-memory transaction should be ordered after everything other operations.
        $wrong = true;
    } else if ($type1 == 2) {
        $wrong = false; // non-memory transcaction is fine.
    } else {
        bool addrMatch = (($type0 == 0) || ($type0 == 1)) && ($addr0 == $addr1);

        if ( ($addr1 < $addr0) || ( addrMatch && ($step1 < $step0) ) ) {
            $wrong = true;          // improper ordering
        } else if ( $type1 == 0 ) {
            $wrong = false;         // store is OK as long as well-ordered
        } else if ($type1 == 1) {   // is this a load? then check the previous instr for consistency
            if ( addrMatch ) {      // addresses match and previous op is a memop
                if ($data0 == $data1) { // data must match
                    $wrong = false;
                } else {
                    $wrong = true;
                }
            } else {                    // otherwise, this is the first op at this address; data must be 0
                if ($data1 == 0) {
                    $wrong = false;
                } else {
                    $wrong = true;
                }
            }
        } else {
            $wrong = true;
        }
    }

