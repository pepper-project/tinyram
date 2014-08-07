package tinyram.sim;

import tinyram.TrVersions;
import java.math.BigInteger;
import tinyram.TrTranscript;
import tinyram.TrMemTranscript;
import tinyram.TrVersions;
import tinyram.TrProgram;
import java.util.List;
import java.util.Map;
import benesNetwork.BenesRouteError;

public abstract class SimExec {
    // run until the program answers
    public abstract BigInteger run() throws SimPMemOOB, SimInvalidOp, BenesRouteError;

    public abstract TrTranscript getTranscript();
    public abstract TrMemTranscript getMemTranscript();

    public static SimExec newSimExec ( final int version, TrProgram prog, int numSteps, Map<Integer,List<BigInteger>> tapes ) {
        try {
            return (SimExec) TrVersions.getClass(version,".SimExec").getConstructor(TrProgram.class,Integer.TYPE,Map.class).newInstance(prog,numSteps,tapes);
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    public static class SimPMemOOB extends Exception {
        public SimPMemOOB ( String msg ) {
            super(msg);
        }
    }

    public static class SimInvalidOp extends Exception {
        public SimInvalidOp ( String msg ) {
            super(msg);
        }
    }
}
