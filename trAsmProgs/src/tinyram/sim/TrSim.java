package tinyram.sim;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.IOException;
import java.io.File;

import tinyram.TrProgram;
import tinyram.TrTapeStore;
import tinyram.TrObjectReader;
import tinyram.TrCommon;
import tinyram.TrTranscript;
import tinyram.TrMemTranscript;
import benesNetwork.BenesRouteError;

public class TrSim {
    public static void main(String[] args) {
        final List<String> inFiles = new ArrayList<String>();
        boolean writeC = false;
        //boolean writeT = false;
        boolean packed = false;
        String outfileBase = "";
        int numSteps = -1;

        // go through commandline arguments
        // files get read in
        // if specified, a directory is used to store results, else present directory is used
        for (int i=0; i<args.length; ++i) {
            final File fh = new File(args[i]);
            if (args[i].equals("-p")) {
                packed = true;
            } else if (args[i].equals("-c")) {
                writeC = true;
                //writeT = true;
            } else if (args[i].equals("-cR")) {
                writeC = true;
            } else if (args.length > i+1 && args[i].equals("-n")) {
                try { numSteps = Integer.parseInt(args[++i]); }
                catch (NumberFormatException ex) {
                    TrCommon.err("Could not parse \"-n "+args[i]+"\": "+ex.toString());
                    numSteps = -1;
                }
            } else if (fh.isFile()) {
                inFiles.add(args[i]);
                if (outfileBase.lastIndexOf(File.separator) == outfileBase.length() - 1) {
                    final String fsBase = args[i].substring(args[i].lastIndexOf(File.separator)+1);
                    final int dotLoc = fsBase.lastIndexOf(".jo");
                    outfileBase += (dotLoc != -1) ? fsBase.substring(0,dotLoc) : fsBase;
                    outfileBase += "_";
                }
            }
            else if (fh.isDirectory() && outfileBase.equals("")) outfileBase = args[i]+File.separator;
            else TrCommon.warn("Ignoring extraneous argument "+args[i]);
        }
        outfileBase += "out";

        // read in the program and tapes
        final TrCommon.Tuple<TrProgram,List<TrTapeStore>> inputs = TrObjectReader.getInputs(inFiles);

        // run the program on each input tape
        if (0 == inputs.snd.size()) {
            TrCommon.warn("No tapes found. Executing program with empty tapes.");
            final String tsC;
            final Map<Integer,Map<Integer,List<BigInteger>>> emptyStore = new HashMap<Integer,Map<Integer,List<BigInteger>>>();
            final Map<Integer,List<BigInteger>> emptyTapes = new HashMap<Integer,List<BigInteger>>();
            emptyStore.put(0,emptyTapes);
            final TrTapeStore emptyTS = new TrTapeStore(emptyStore,inputs.fst.wordsize);
            runSim(inputs.fst,emptyTS.getTapeIter(0),0,outfileBase,numSteps,writeC,emptyTS,packed);
        } else {
            for ( int i = 0; i<inputs.snd.size(); ++i) {            // go through each tape store
                final TrTapeStore ts = inputs.snd.get(i);
                if (ts.wordsize != inputs.fst.wordsize) {
                    TrCommon.warn("Found input tapestore with incompatible wordsize in "+ts.filename+". Skipping.");
                } else {
                    //final Map<Integer,String> tapeStrings = writeT ? ts.toC() : null;
                    for ( Integer iter : ts.getTapeIters() ) {   // go through each iteration in this tape store
                        runSim (inputs.fst,ts.getTapeIter(iter),iter,outfileBase,numSteps,writeC,ts,packed);
                    }
                }
            }
        }
    }

    public static void runSim (TrProgram prog, Map<Integer,List<BigInteger>> tapes, int iter, String ofBase, int numSteps, boolean writeC, TrTapeStore ts, boolean packed) {
        final BigInteger retval;
        final SimExec exec;
        PrintStream ostr;

        // run the program
        try { 
            exec = SimExec.newSimExec(prog.version,prog,numSteps,tapes);
            retval = exec.run();
        } catch (SimExec.SimPMemOOB|SimExec.SimInvalidOp|BenesRouteError ex) {
            TrCommon.err("Iteration " + Integer.toString(iter) + " :: Error executing program: "+ex.toString());
            return;
        }

        // dump the results
        try { 
            ostr = new PrintStream(String.format("%s_%06x%s",ofBase,iter,writeC ? ".c" : ""));
            //ostr.print(tapeStr);

            final TrTranscript eTrans = exec.getTranscript();
            final TrMemTranscript mTrans = exec.getMemTranscript();

            if (writeC) {
                ostr.print(eTrans.toC(retval,numSteps));
                ostr.print(mTrans.toC(packed));
            } else {
                ostr.print(eTrans.toString(retval));
                ostr.print(mTrans.toString());
            }
        } catch (IOException ex) {
            TrCommon.warn("Could not open output file: "+ex.toString()+"\nUsing STDOUT");
            ostr = System.out;
        }

        System.out.println("Iter "+Integer.toString(iter)+" return value: 0x" + retval.toString(16));
        return;
    }
}
