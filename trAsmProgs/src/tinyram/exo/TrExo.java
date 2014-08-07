package tinyram.exo;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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
import tinyram.TrInstruction;
import tinyram.TrTapeStore;
import tinyram.TrObjectReader;
import tinyram.TrCommon;
import tinyram.TrTranscript;
import tinyram.TrMemTranscript;
import tinyram.sim.SimExec;
import benesNetwork.BenesRouteError;

public class TrExo {
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("TrExo requires 1 argument, received zero.");
        }

        // total number of outputs expected. Check that this is comparable to what we expect
        final int numOutputs = Integer.parseInt(args[0]);

        // read in from stdin
        final Scanner inS = new Scanner(System.in);
        final List<String> inList = new ArrayList<String>();
        while (inS.hasNext()) {
            final String s = inS.next();
            inList.add(s);
        }

        final Iterator<String> in = inList.iterator();
        final String assertFailMsg = "Invalid input format. Expected: [ program ] [ intape ] [ auxtape ] [ config ]";
        // hooray for slightly broken generics implementations. This is how we get arrays of containers!
        final List<BigInteger>[] inLists = (List<BigInteger>[]) new List<?>[4];

        String tok;
        for (int i=0; i<4; i++) {
            tok = in.next();
            assert tok.equals("[") : assertFailMsg;

            inLists[i] = new ArrayList<BigInteger>();

            for (tok = in.next() ; ! "]".equals(tok) ; tok = in.next()) {
                inLists[i].add(TrCommon.rat2BigInt(tok));
            }
        }
        assert ! in.hasNext() : assertFailMsg;

        assert inLists[3].size() == 8 :
            "Invalid input. Last list must be [ ver wordsize #regBits #immBits inTapeLoc auxTapeLoc #steps #memOps ]." ;

        final int version = inLists[3].get(0).intValue();
        final int wordsize = inLists[3].get(1).intValue();
        final int numRegBits = inLists[3].get(2).intValue();
        final int numImmBits = inLists[3].get(3).intValue();
        final BigInteger inTapeLoc = inLists[3].get(4);
        final BigInteger auxTapeLoc = inLists[3].get(5);
        final int numSteps = inLists[3].get(6).intValue();
        final int numMemOps = inLists[3].get(7).intValue();

        // create the instruction memory. Our program memory is a map
        // because in principle we can have an arbitrary wordsize and
        // so memory addresses can be larger than an Integer
        final Map<BigInteger,TrInstruction> pmem = new HashMap<BigInteger,TrInstruction>(2*inLists[0].size());
        for (int i=0; i<inLists[0].size(); i++) {
            pmem.put(new BigInteger(Integer.toString(i)),new TrInstruction(inLists[0].get(i),TrCommon.getNumOpBits(version),numRegBits,numImmBits));
        }

        // create the program
        final TrProgram prog = new TrProgram(version,wordsize,numRegBits,numImmBits,pmem,inTapeLoc,auxTapeLoc);
        // create the tapestore
        final TrTapeStore pTapes = new TrTapeStore(inLists[1],inLists[2],wordsize);

        final int expectedMemOps = (int) Math.pow(2,TrCommon.bitLength(inLists[0].size() + inLists[1].size() + inLists[2].size() + 2*numSteps));
        assert numMemOps == expectedMemOps :
            "Invalid input. Expected " + Integer.toString(expectedMemOps) + " but was told to provide " + Integer.toString(numMemOps);

        final BigInteger retval;
        final SimExec exec;

        try {
            exec = SimExec.newSimExec(version,prog,numSteps,pTapes.getTapeIter(0));
            retval = exec.run();
        } catch (SimExec.SimPMemOOB|SimExec.SimInvalidOp|BenesRouteError ex) {
            TrCommon.err("Error executing program: "+ex.toString());
            return;
        }

        final TrTranscript eTrans = exec.getTranscript();
        final TrMemTranscript mTrans = exec.getMemTranscript();

        final StringBuilder s = new StringBuilder();
        int numActualOutputs = eTrans.toProverInput(s);
        numActualOutputs += mTrans.toProverInput(s);

        assert numOutputs == numActualOutputs :
            "Was told to provide " + Integer.toString(numOutputs) + " but actually providing " + Integer.toString(numActualOutputs);

        System.out.println(s.toString());
    }
}
