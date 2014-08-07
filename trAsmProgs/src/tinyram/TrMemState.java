package tinyram;

import java.math.BigInteger;

public abstract class TrMemState {
    protected boolean isMem;
    protected boolean isLoad;
    protected BigInteger addr;
    protected int stepNum;
    protected BigInteger data;

    public static TrMemState newTrMemState (int version, TrState st, TrState stNext, int stepNum, TrInstruction instr) {
	      try {
	          return (TrMemState) TrVersions.getClass(version,".TrMemState").getConstructor(TrState.class,TrState.class, Integer.TYPE,TrInstruction.class,Integer.TYPE).newInstance(st, stNext, stepNum, instr, version);
	      } catch (Exception ex) {
	          throw new RuntimeException(ex.toString());
	      }
    }
    
    public static TrMemState newTrMemState (int version, BigInteger addr, BigInteger data, int stepNum, boolean isLoad) {
      try {
        	return (TrMemState) TrVersions.getClass(version,".TrMemState").getConstructor(BigInteger.class,BigInteger.class, Integer.TYPE,Boolean.TYPE).newInstance(addr, data, stepNum, isLoad);
      } catch (Exception ex) {
        	throw new RuntimeException(ex.toString());
      }
    }

    public TrMemState copy () { return copy(stepNum); }
    public abstract TrMemState copy ( int stepNum );

    public String toString () {
        if (! isMem) {
            return "NON-MEMOP ("+Integer.toString(stepNum)+")";
        }

        final StringBuilder s = new StringBuilder();

        s.append(isLoad ? "LOAD" : "STORE");
        s.append(" ADDR=0x").append(addr.toString(16));
        s.append(" STEP=0x").append(Integer.toString(stepNum,16));
        s.append(" DATA=0x").append(data.toString(16));

        return s.toString();
    }

    public String toC ( int nStepBits, int wordsize, boolean packed ) {
        final StringBuilder s = new StringBuilder();

        if (packed) {
            s.append("0b");
            s.append(TrCommon.int2zpBin(addr,wordsize));
            s.append(isMem ? "1" : "0");
            s.append(TrCommon.int2zpBin(stepNum,nStepBits));
            s.append(isLoad ? "1" : "0");
            s.append(TrCommon.int2zpBin(data,wordsize));
        } else {
            s.append("{.memType=").append(isMem ? (isLoad ? "1" : "0") : "2");
            s.append(",.memAddr=0x").append(addr.toString(16));
            s.append(",.stepNum=0x").append(Integer.toString(stepNum,16));
            s.append(",.memData=0x").append(data.toString(16));
            s.append("}");
        }

        return s.toString();
    }
}
