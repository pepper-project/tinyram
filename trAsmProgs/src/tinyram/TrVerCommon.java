package tinyram;

import java.math.BigInteger;

public abstract class TrVerCommon {
    public abstract BigInteger canonicalize (BigInteger num, boolean isImm);

    public abstract BigInteger signedValue (BigInteger num, boolean isImm);

    public abstract int nImmBits ();

    public static TrVerCommon newTrVerCommon ( int version, int wordsize, int nRegBits ) {
        try {
            return (TrVerCommon) TrVersions.getClass(version,".TrVerCommon").getConstructor(Integer.TYPE,Integer.TYPE).newInstance(wordsize,nRegBits);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex.toString());
        }
    }
}
