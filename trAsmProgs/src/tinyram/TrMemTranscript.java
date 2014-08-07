package tinyram;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.math.BigInteger;
import benesNetwork.BenesRoute;
import benesNetwork.BenesRouteError;

public class TrMemTranscript {
    private final List<TrMemState> memUnsort;
    private final List<TrMemState> memSort;
    private BigInteger memTransOffset = BigInteger.ZERO;
    private final List<List<Boolean>> memRoute;
    private final int nStepBits;
    private final int wordsize;

    public TrMemTranscript ( List<TrMemState> memUnsort , int wordsize ) throws BenesRouteError {
        int i;
        nStepBits = TrCommon.bitLength(memUnsort.size());
        final int memStepBase = (int) Math.pow(2,nStepBits);
        this.wordsize = wordsize;

        memSort = new ArrayList<TrMemState>(memUnsort);
        final MemCompare memComp = new MemCompare();
        Collections.sort(memSort,memComp);

        this.memUnsort = memUnsort;
        memRoute = BenesRoute.benesRoute(memUnsort,memSort);
    }

    class MemCompare implements Comparator<TrMemState> {
        public int compare(TrMemState lhs, TrMemState rhs) {
            if (lhs.isMem ^ rhs.isMem) {
                // memory instructions always come earlier in the sort
                if (lhs.isMem) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                final int addrComp = lhs.addr.compareTo(rhs.addr);
                if (addrComp != 0) {
                    // sort on address
                    return addrComp;
                } else {
                    // break ties with step number
                    final int lhsStep = lhs.stepNum;
                    final int rhsStep = rhs.stepNum;
                    return Integer.compare(lhsStep,rhsStep);
                }
            }
        }
    }

    // in ver1, the verifier provides predefined inputs to the Benes network
    // this offset is the spot at which the transition constraints should start
    // inserting their purported memory transactions
    public void setOffset ( final BigInteger offset ) { memTransOffset = offset; }

    public String toString() {
        final StringBuilder s = new StringBuilder();

        for(TrMemState mSt : memSort)
            s.append(mSt.toString()).append("\n");

        return s.toString();
    }

    public int toProverInput(final StringBuilder s) {
        int numWritten = 0;

        for (List<Boolean> layer : memRoute) {
            numWritten += layer.size();
            for (Boolean set : layer) {
                s.append(set ? " 1" : " 0");
            }
        }

        return numWritten;
    }

    public String toC( boolean packed ) {
        final StringBuilder s = new StringBuilder();
        boolean isFirst = true;

        s.append("#ifndef TR_SUPPRESS_TRANSCRIPT\n");
        s.append("#ifndef TR_TRANS_VER_H\n");
        s.append("#define TR_MEMTRANS_OFFSET 0x").append(memTransOffset.toString(16)).append("\n");
        if (packed)
            s.append("#define TR_MEMTRANS_PACKED 1\n");
        else
            s.append("#undef TR_MEMTRANS_PACKED\n");
        s.append("#define TR_BENES_STAGES ").append(memRoute.size()).append("\n");
        s.append("#define TR_BENES_SWITCHES ").append(memRoute.get(0).size()).append("\n");
        s.append("#else   // TR_TRANS_VER_H\n");

        final String connector = packed ? "" : "\n        ";
        s.append("    .memTransSorted=").append(connector).append("{");
        for (TrMemState mSt : memSort) {
            if (isFirst)
                isFirst = false;
            else
                s.append(connector).append(",");
            s.append(mSt.toC(nStepBits,wordsize,packed));
        }
        s.append(connector).append("},\n");

        isFirst = true;
        s.append("    .memTrans=").append(connector).append("{");
        for (TrMemState mSt : memUnsort) {
            if (isFirst)
                isFirst = false;
            else
                s.append(connector).append(",");
            s.append(mSt.toC(nStepBits,wordsize,packed));
        }
        s.append(connector).append("},\n");

        isFirst = true;
        s.append("    .benesRoute={");
        for (List<Boolean> layer : memRoute) {
            for (Boolean set : layer) {
                if (isFirst)
                    isFirst = false;
                else
                    s.append(",");
                s.append(set ? "1" : "0");
            }
        }
        s.append("},\n");

        s.append("#endif  // TR_TRANS_VER_H\n");
        s.append("#endif  // TR_SUPPRESS_TRANSCRIPT\n");

        return s.toString();
    }
}
