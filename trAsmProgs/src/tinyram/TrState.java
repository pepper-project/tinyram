package tinyram;

import java.math.BigInteger;
import java.util.List;

public abstract class TrState {
    public abstract String toC();
    public abstract int toProverInput(final StringBuilder s);
    public abstract String toString();

    public abstract BigInteger getReg( int regNum );
    public abstract BigInteger getPC();

    public static TrState newTrState ( int version, BigInteger pc, int iTPtr, int aTPtr, boolean flag, List<BigInteger> regs ) {
        try {
            return (TrState) TrVersions.getClass(version,".TrState").getConstructor(BigInteger.class,Integer.TYPE,Integer.TYPE,Boolean.TYPE,List.class).newInstance(pc,iTPtr,aTPtr,flag,regs);
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }
}
