package tinyram;

import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

// a transcript is just a sequence of "TrState"s.
// in addition, it can output itself as C or as a terse value report
public class TrTranscript {
    private final List<TrState> transcript = new ArrayList<TrState>();

    public TrTranscript () { }

    public void add (TrState st) {
        transcript.add(st);
    }

    public TrState get (int idx) {
        return transcript.get(idx);
    }

    public int size () {
        return transcript.size();
    }

    public String toC( ) { return toC(null,-1); }
    public String toC( BigInteger retVal ) { return toC(retVal,-1); }
    public String toC( BigInteger retVal, int numSteps ) {
        final StringBuilder s = new StringBuilder();
        boolean isFirst = true;

        s.append("#ifdef  TR_TRANS_VER_H\n");
        s.append("#ifndef TR_SUPPRESS_TRANSCRIPT\n");
        s.append("    .transcript=\n        {");

        for (TrState st : transcript) {
            if (isFirst)
                isFirst = false;
            else
                s.append("\n        ,");
            s.append(st.toC());
        }

        s.append("\n        },\n");

        if (null != retVal)
            s.append("    .returnValue=0x").append(retVal.toString(16)).append(",\n");

        s.append("#endif  // TR_SUPPRESS_TRANSCRIPT\n");
        s.append("#endif  // TR_TRANS_VER_H\n");

        if (-1 != numSteps) {
            s.append("#ifndef TR_TRANS_VER_H\n");
            s.append("#ifndef TR_NUMSTEPS\n");
            s.append("#define TR_NUMSTEPS ").append(numSteps).append("\n");
            s.append("#endif  // TR_NUMSTEPS\n");
            s.append("#endif  // TR_TRANS_VER_H\n");
        }

        return s.toString();
    }

    public int toProverInput(final StringBuilder s) {
        int numWritten = 0;

        for (TrState st : transcript) {
            numWritten += st.toProverInput(s);
        }

        return numWritten;
    }

    public String toString( ) { return toString(null); }
    public String toString( BigInteger retVal ) {
        final StringBuilder s = new StringBuilder();

        for (TrState st : transcript)
            s.append(st.toString()).append("\n");

        if (null != retVal)
            s.append("VALUE 0x").append(retVal.toString(16));

        return s.toString();
    }
}
