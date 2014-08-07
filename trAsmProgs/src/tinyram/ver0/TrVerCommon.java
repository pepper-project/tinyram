package tinyram.ver0;

import tinyram.TrCommon;
import java.math.BigInteger;


public class TrVerCommon extends tinyram.TrVerCommon {
    public final static String[] opcodes =    
    {   "and"  ,"or"   ,"xor"  ,"not"  ,"add"  ,"sub"  ,"mull" ,"umulh"
    ,   "smulh","udiv" ,"umod" ,"shl"  ,"shr"  ,"cmpe" ,"cmpa" ,"cmpae"
    ,   "cmpg" ,"cmpge","mov"  ,"cmov" ,"jmp"  ,"cjmp" ,"cnjmp",""     
    ,   ""     ,""     ,""     ,""     ,"store","load" ,"read" ,"answer"
    };

    public final static int[] nArgs =
    {   3, 3, 3, 2, 3, 3, 3, 3
    ,   3, 3, 3, 3, 3, 2, 2, 2
    ,   2, 2, 2, 2, 1, 1, 1, 0
    ,   0, 0, 0, 0, 2, 2, 2, 1
    };

    public final static int numOpBits = 5;
    public final int wordsize;
    public final int nRegBits;
    public final BigInteger WORDMAX;
    public final BigInteger SWRDMAX;

    public TrVerCommon ( int wordsize, int nRegBits ) {
        this.wordsize = wordsize;
        this.nRegBits = nRegBits;
        WORDMAX = TrCommon.TWO.pow(wordsize);
        SWRDMAX = TrCommon.TWO.pow(wordsize-1);
    }

    // canonicalize a BigInteger value
    // to the chosen word size
    // result is a non-negative BigInteger smaller than WORDSIZE
    // BigInteger's implementation of mod is correct for negative numbers,
    // so this can be pretty simple. If "mod" is really remainder, it becomes
    // more complex.
    // ver0 doesn't care whether it's a register or an immediate
    public BigInteger canonicalize (BigInteger num, boolean isImm) { return num.mod(WORDMAX); }

    // given a >canonicalized< BigInteger value, interpret it as a signed value
    public BigInteger signedValue (BigInteger num, boolean isImm) {
        if (num.compareTo(SWRDMAX) < 0) return num;
        else return num.subtract(WORDMAX);
    }

    public int nImmBits () { return wordsize; }
}
