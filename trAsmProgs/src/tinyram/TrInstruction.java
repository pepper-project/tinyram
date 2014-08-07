package tinyram;

import java.math.BigInteger;
import java.io.Serializable;

public class TrInstruction implements Serializable {
    public final int op;
    public final int r1;
    public final int r2;
    public final int r3;
    public final BigInteger imm;
    public final boolean useImm;

    public TrInstruction (final int op, final int r1, final int r2, final int r3,
                          final BigInteger imm, final boolean useImm) {
        this.op = op;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.imm = imm;
        this.useImm = useImm;
    }

    public TrInstruction ( final BigInteger packedInstr, int numOpBits, int numRegBits, int numImmBits ) {
        // unpack the instruction into a zero-padded string
        final int len = numImmBits + 2*numRegBits + numOpBits + 1;
        final String instrBits = TrCommon.int2zpBin(packedInstr,len);
        assert len == instrBits.length() : "int2zpBin failed to return a string of the correct length.";

        this.op = Integer.parseInt(instrBits.substring(0,numOpBits),2);
        this.useImm = instrBits.substring(numOpBits,numOpBits+1).equals("1");
        this.r1 = Integer.parseInt(instrBits.substring(numOpBits+1,numOpBits+1+numRegBits),2);
        this.r2 = Integer.parseInt(instrBits.substring(numOpBits+1+numRegBits,numOpBits+1+2*numRegBits),2);
        this.r3 = Integer.parseInt(instrBits.substring(len-numRegBits),2);
        this.imm = new BigInteger(instrBits.substring(len-numImmBits),2);
    }

    private String packStr( int numOpBits, int numRegBits, int numImmBits ) {
        final StringBuilder s = new StringBuilder();

        s.append(TrCommon.int2zpBin(op,numOpBits));
        s.append(useImm ? "1" : "0");
        s.append(TrCommon.int2zpBin(r1,numRegBits));
        s.append(TrCommon.int2zpBin(r2,numRegBits));
        if (useImm) s.append(TrCommon.int2zpBin(imm,numImmBits));
        else        s.append(TrCommon.int2zpBin(r3,numImmBits));

        return s.toString();
    }

    BigInteger pack( int numOpBits, int numRegBits, int numImmBits ) {
        return new BigInteger(packStr(numOpBits,numRegBits,numImmBits),2);
    }

    String toC(int numOpBits, int numRegBits, int numImmBits) {
        return toC(numOpBits,numRegBits,numImmBits,false);
    }

    String toC(int numOpBits, int numRegBits, int numImmBits, boolean packed) {
        if (packed) {
            return "0b"+packStr(numOpBits,numRegBits,numImmBits);
        } else {
            final StringBuilder s = new StringBuilder();
            s.append("{.op=0x").append(Integer.toString(op,16)).append(",");
            s.append(".uImm=").append(useImm ? "1" : "0").append(",");
            s.append(".reg1=0x").append(Integer.toString(r1,16)).append(",");
            s.append(".reg2=0x").append(Integer.toString(r2,16)).append(",");
            s.append(".reg3=0x");
            if (useImm) s.append(imm.toString(16));
            else        s.append(Integer.toString(r3,16));
            s.append("}");
            return s.toString();
        }
    }

    public int toInputGen(int startIndex, StringBuilder s, int numOpBits,
                          int numRegBits, int numImmBits) {
        BigInteger inst = new BigInteger(packStr(numOpBits,numRegBits,numImmBits), 2);
        s.append(String.format("mpq_set_ui(input_q[%d], %sL, 1L);\n", startIndex++, inst.toString()));
        return startIndex;
    }
}
