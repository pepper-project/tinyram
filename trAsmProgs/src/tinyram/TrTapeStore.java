package tinyram;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.math.BigInteger;
import java.io.Serializable;

public class TrTapeStore implements Serializable {
    public final static List<Integer> minTapeSet = new ArrayList<Integer>(Arrays.asList(0));
    public String filename = "";
    public final int wordsize;

    // not writeable, so we expose a getter
    private final Map<Integer,Map<Integer,List<BigInteger>>> tapes;

    public TrTapeStore (final Map<Integer,Map<Integer,List<BigInteger>>> tapes, int wordsize) {
        this.tapes = tapes;
        this.wordsize = wordsize;
    }

    public TrTapeStore (final List<BigInteger> inTape, final List<BigInteger> auxTape, final int wordsize) {
        // allocate a tapestore and a tape map
        this.tapes = new HashMap<Integer,Map<Integer,List<BigInteger>>>();
        final Map<Integer,List<BigInteger>> tmpMap = new HashMap<Integer,List<BigInteger>>();
        // insert the one tape at position 0, iter 0
        tmpMap.put(0,inTape);
        tmpMap.put(1,auxTape);
        this.tapes.put(0,tmpMap);
        this.wordsize = wordsize;
    }

    public Map<Integer,List<BigInteger>> getTapeIter ( final int iter ) {
        return tapes.get(iter);
    }

    public Set<Integer> getTapeIters () { return new HashSet<Integer>(tapes.keySet()); }

    public Map<Integer,String> toC() {
        final Map<Integer,String> cTapes = new HashMap<Integer,String>();
        final StringBuilder s = new StringBuilder();
        for (Integer iterNum : getTapeIters()) {
            s.setLength(0);
            s.append("#ifdef  TR_TRANS_VER_H\n");
            s.append("#ifndef TR_TAPES_DECL\n");
            s.append("#define TR_TAPES_DECL 1\n");
            // get all the tapes we have to process
            final Set<Integer> tapeSet = new HashSet<Integer>(tapes.get(iterNum).keySet());
            tapeSet.addAll(minTapeSet); // make sure we hit at least the minimum tapes

            for (Integer tapeNum : tapeSet) {
                final List<BigInteger> thisTape = tapes.get(iterNum).get(tapeNum);
                s.append("    .tape").append(tapeNum).append("={");
                for (BigInteger tData : thisTape)
                    s.append("0x").append(tData.toString(16)).append(",");
                s.append("},\n");
            }
            s.append("#endif  // TR_TAPES_DECL\n");
            s.append("#endif  // TR_TRANS_VER_H\n");
            cTapes.put(iterNum,s.toString());
        }
        return cTapes;
    }

    public int toInputGen(final int startIndex, final Map<Integer,String> gTapes) {
        int gEndIndex = -1;
        final StringBuilder s = new StringBuilder();
        for (Integer iterNum : getTapeIters()) {
            int endIndex = startIndex;
            s.setLength(0);

            final Set<Integer> tapeSet = new HashSet<Integer>(tapes.get(iterNum).keySet());
            tapeSet.addAll(minTapeSet); // make sure we hit at least the minimum tapes

            for (Integer tapeNum : tapeSet) {
                final List<BigInteger> thisTape = tapes.get(iterNum).get(tapeNum);

                for (BigInteger i : thisTape) {
                    s.append(String.format("mpq_set_ui(input_q[%d], %sL, 1L);\n", endIndex++, i.toString()));
                }
            }
            gTapes.put(iterNum,s.toString());

            if (gEndIndex < 0) {
                gEndIndex = endIndex;
            } else if (gEndIndex != endIndex) {
                throw new IllegalArgumentException("Multiple tapes to the same program have different lengths.");
            }
        }

        return gEndIndex;
    }
}
