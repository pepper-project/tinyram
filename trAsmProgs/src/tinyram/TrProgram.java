package tinyram;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedSet;
import java.math.BigInteger;
import java.io.Serializable;

public class TrProgram implements Serializable {
    public final int version;                           // processor version
    public final int wordsize;
    public final int numRegBits;
    public final int numImmBits;
    public String filename = "";
    public final BigInteger inTapeLoc;
    public final BigInteger auxTapeLoc;
    // not writeable, so we only expose get()
    private final Map<BigInteger,TrInstruction> pmem;   // program counter is wordsize, so
                                                        // addresses are BigInteger and thus
                                                        // List is insufficient
    private final int numOpBits;

    public TrProgram (final int version, final int wordsize, final int numRegBits, final int numImmBits,
                      final Map<BigInteger,TrInstruction> pmem, final BigInteger inTapeLoc, final BigInteger auxTapeLoc) {
        this.version = version;
        this.wordsize = wordsize;
        this.numRegBits = numRegBits;
        this.numOpBits = TrCommon.getNumOpBits(version);
        this.numImmBits = numImmBits;
        this.pmem = pmem;
        this.inTapeLoc = inTapeLoc;
        this.auxTapeLoc = auxTapeLoc;
    }

    private String instrDefs () {
        final int[] defs = new int[(int) Math.pow(2,TrCommon.getNumOpBits(version))];
        final StringBuilder s = new StringBuilder();

        for (Map.Entry<BigInteger,TrInstruction> ent : pmem.entrySet()) {
            final int op = ent.getValue().op;
            
            if (defs[op] == 0) {
                defs[op] = 1;
                s.append("#define TR_USEOP_").append(op).append("\n");
            }
        }

        return s.toString();
    }

    public TrInstruction getInstr ( BigInteger pc ) { return pmem.get(pc); }

    public BigInteger getPackedInstr ( BigInteger pc ) {
        final TrInstruction instr = pmem.get(pc);
        if (null == instr) {
            return BigInteger.ZERO;
        } else {
            return instr.pack(numOpBits,numRegBits,numImmBits);
        }
    }

    public SortedSet<BigInteger> getAddrs () { return new TreeSet<BigInteger>(pmem.keySet()); }

    public String toC() { return toC(false); }
    public String toC( boolean packed ) {
        final BigInteger maxAddr = getAddrs().last();
        final StringBuilder s = new StringBuilder();
        s.append("#ifndef TR_TRANS_VER_H\n");
        s.append("#ifndef TR_NREGBITS\n");
        s.append("#define TR_NREGBITS ").append(numRegBits).append("\n");
        s.append("#define TR_NREGS    ").append((int) Math.pow(2,numRegBits)).append("\n");
        s.append("#define TR_REGSIZE  ").append(wordsize).append("\n");
        s.append("#define TR_REGMASK  ((uint64_t) 0x").append(TrCommon.TWO.pow(wordsize).subtract(BigInteger.ONE).toString(16)).append(")\n");
        s.append("#define TR_SREGMASK ((uint64_t) 0x").append(TrCommon.TWO.pow(wordsize-1).toString(16)).append(")\n");
        s.append("#define TR_IMMSIZE  ").append(numImmBits).append("\n");
        s.append("#define TR_IMMMASK  ((uint64_t) 0x").append(TrCommon.TWO.pow(numImmBits).subtract(BigInteger.ONE).toString(16)).append(")\n");
        s.append("#define TR_SIMMMASK ((uint64_t) 0x").append(TrCommon.TWO.pow(numImmBits-1).toString(16)).append(")\n");
        s.append("#define TR_VERSION ").append(version).append("\n");
        s.append("#define TR_VERSION_").append(version).append("\n");
        s.append(instrDefs());
        if (packed) {
            s.append("#define TR_OP_INSTRMASK ").append("0x").append(Integer.toString((int) Math.pow(2,numOpBits) - 1,16)).append("\n");
            s.append("#define TR_OP_REGMASK   ").append("0x").append(Integer.toString((int) Math.pow(2,numRegBits) - 1,16)).append("\n");
            s.append("#define TR_PROGRAM_PACKED 1\n");
        } else { s.append("#undef TR_PROGRAM_PACKED\n"); }
        s.append("#define TR_INTAPE_LOC 0x").append(inTapeLoc.toString(16)).append("\n");
        s.append("#define TR_AUXTAPE_LOC 0x").append(auxTapeLoc.toString(16)).append("\n");
        s.append("#ifndef TR_PROGSIZE\n");
        s.append("#define TR_PROGSIZE 0x").append(maxAddr.add(BigInteger.ONE).toString(16)).append("\n");
        s.append("#endif  // TR_PROGSIZE\n");
        s.append("#endif  // TR_NREGBITS\n");
        s.append("#else   // TR_TRANS_VER_H\n");
        s.append("#ifndef TR_PROG_DECL\n");
        s.append("#define TR_PROG_DECL 1\n");

        final String connector = packed ? "" : "\n        ";
        s.append("    .pmem=").append(connector).append("{");
        // we don't use an iterator because we have to emit empty instruction placeholders
        // yes, I am *fully* aware how ugly this is.
        // the alternative would be an integer loop counter and then pmem.get(new BigInteger(Integer.toString(i)))
        // which to me is even uglier
        BigInteger i;
        boolean isFirst = true;
        for (i = BigInteger.ZERO; i.compareTo(maxAddr) < 1; i=i.add(BigInteger.ONE)) {  // stop before the last instruction
            final TrInstruction thisInstr;
            if (isFirst)
                isFirst = false;
            else
                s.append(connector).append(",");
            thisInstr = pmem.get(i);
            if (null == thisInstr) s.append("{}");
            else s.append(thisInstr.toC(numOpBits,numRegBits,numImmBits,packed));
        }

        s.append(connector).append("},\n");
        s.append("#endif  // TR_PROG_DECL\n");
        s.append("#endif  // TR_TRANS_VER_H\n");
        return s.toString();
    }

    public int toInputGen(int startIndex, StringBuilder s) {
    		final BigInteger maxAddr = getAddrs().last();
        BigInteger i;
    	 	for (i = BigInteger.ZERO; i.compareTo(maxAddr) < 1; i=i.add(BigInteger.ONE)) {  // stop before the last instruction
    	 			final TrInstruction thisInstr;
    	 			thisInstr = pmem.get(i);
    	 			if (null != thisInstr) 
    	 					startIndex = thisInstr.toInputGen(startIndex, s, numOpBits,numRegBits,numImmBits);
    	 	}
      	return startIndex;
    }
}
