package tinyram;

import java.math.BigInteger;

public class TrCommon {
    public final static BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
    public final static BigInteger MINUS1 = BigInteger.ZERO.subtract(BigInteger.ONE);

    public final String[] opcodes;
    public final int[] nArgs;
    public final int numOpBits;
    public final TrVerCommon vCommon;

    public TrCommon ( final int wordsize, final int nRegBits, final int version ) {
        opcodes = getOpcodes(version);
        nArgs = getNArgs(version);
        numOpBits = getNumOpBits(version);
        vCommon = TrVerCommon.newTrVerCommon(version,wordsize,nRegBits);
    }

    // version-specific
    public BigInteger canonicalize (BigInteger num, boolean isImm) { return vCommon.canonicalize(num,isImm); }
    public BigInteger signedValue (BigInteger num, boolean isImm) { return vCommon.signedValue(num,isImm); }
    public int nImmBits() { return vCommon.nImmBits(); }

    // number of bits necessary to represent a (positive!) integer
    public static int bitLength (int num) {
        return (int) Math.ceil(Math.log((double) num)/Math.log(2));
    }

    // integer to zero-padded binary
    public static String int2zpBin (int num, int len) {
        final String zeroPadding = rep('0',len);
        final String numString   = Integer.toBinaryString(num);
        return (zeroPadding.substring(numString.length()) + numString);
    }

    // bigInt to zero-padded binary
    public static String int2zpBin (BigInteger num, int len) {
        final String zeroPadding = rep('0',len);
        final String numString = num.toString(2);
        return (zeroPadding.substring(numString.length()) + numString);
    }
    
    // make a string of specified length using specified character
    public static String rep(char c, int n) {
        return new String(new char[n]).replace('\0',c);
    }

    // take a rational string like num%den and turn it into a BigInteger
    public static BigInteger rat2BigInt(final String rational) {
        final int pOff = rational.indexOf("%");
        if (0 > pOff) {
            return new BigInteger(rational);
        } else {
            final BigInteger num = new BigInteger(rational.substring(0,pOff));
            return num.divide(new BigInteger(rational.substring(pOff+1)));
        }
    }

    // tuple for 2 return values
    public static class Tuple<X,Y> {
        public X fst;
        public Y snd;
        public Tuple (final X x, final Y y) {
            fst = x;
            snd = y;
        }
    }

    public static void info(String warnStr) { msg("INFO", warnStr); }
    public static void warn(String warnStr) { msg("WARNING", warnStr); }
    public static void err(String errStr) { msg("ERROR", errStr); }
    public static void msg(String msgType, String msgStr) {
        System.err.println();
        System.err.println(">>"+msgType+": " + msgStr);
        System.err.println();
    }

// use introspection to dynamically use the correct processor version
    private static Object getX ( final int version, final String what ) {
        TrVersions.checkVersion(version);
        try {
            return Class.forName(TrVersions.versions[version]+".TrVerCommon").getField(what).get(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    public static int[] getNArgs ( final int version ) { return (int[]) getX(version,"nArgs"); }

    public static String[] getOpcodes ( final int version ) { return (String[]) getX(version,"opcodes"); }

    public static int getNumOpBits ( final int version ) { return (int) getX(version,"numOpBits"); }
}
