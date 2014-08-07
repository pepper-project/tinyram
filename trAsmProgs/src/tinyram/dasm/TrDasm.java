package tinyram.dasm;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import tinyram.TrProgram;
import tinyram.TrInstruction;
import tinyram.TrTapeStore;
import tinyram.TrObjectReader;
import tinyram.TrCommon;

public class TrDasm {
    public static void main(final String[] args) {
        int trFormat = 2;
        final List<String> inFiles = new ArrayList<String>();

        // -f switch specifies the "format" argument to TinyRAM
        for (int i = 0; i<args.length; ++i) {
            if (args[i].equals("-f") && args.length > i+1) {
                try { trFormat = Integer.parseInt(args[++i]); }
                catch (NumberFormatException ex) {
                    TrCommon.err("Could not parse \"-f "+args[i]+"\": "+ex.toString());
                    trFormat = 2;
                }
            } else {
                inFiles.add(args[i]);
            }
        }

        TrCommon.Tuple<TrProgram,List<TrTapeStore>> inputs = TrObjectReader.getInputs(inFiles);

        if (null != inputs.fst) { trProgDasm(inputs.fst,trFormat); }

        if (null != inputs.snd && 0 < inputs.snd.size()) {
            final int fieldsize;
            if (null == inputs.fst) {
                System.out.println(String.format("$ TinyRam 0 %d %d 0",trFormat,inputs.snd.get(0).wordsize));
                fieldsize = inputs.snd.get(0).wordsize / 3 + 2;
            } else {
                fieldsize = inputs.fst.wordsize / 3 + 2;
            }
            final String fmtTTxt = String.format("tape   %%-%ds , %%-%ds , %%-%ds ; tape value",fieldsize,fieldsize,fieldsize);
            trTapeDasm(inputs.snd,fmtTTxt);
        }
    }

    public static void trProgDasm(final TrProgram prog, final int trFormat) {
        final Set<BigInteger> addrs = prog.getAddrs();

        System.out.println(String.format("$ TinyRAM %d %d %d %d",prog.version,trFormat,prog.wordsize,prog.numRegBits));

        int fieldsize = prog.wordsize / 3 + 2;
        String fmt3Txt = String.format("%%-6s r%%-%ds , r%%-%ds , %%-%ds ; addr=0x%%s useIm=%%b",fieldsize-1,fieldsize-1,fieldsize);
        String fmt2Txt = String.format("%%-6s r%%-%ds , %%-%ds   %%-%ds ; addr=0x%%s useIm=%%b",fieldsize-1,fieldsize,fieldsize);
        String fmt1Txt = String.format("%%-6s %%-%ds   %%-%ds   %%-%ds ; addr=0x%%s useIm=%%b",fieldsize,fieldsize,fieldsize);

        final String[] opcodes = TrCommon.getOpcodes(prog.version);
        final int[] nArgs = TrCommon.getNArgs(prog.version);

        BigInteger prevAddr = BigInteger.ZERO.subtract(BigInteger.ONE);
        for (BigInteger addr : addrs) {
            if(! prevAddr.add(BigInteger.ONE).equals(addr)) {
                System.out.println("$ org 0x"+addr.toString(16));
            }
            prevAddr = addr;

            final TrInstruction instr = prog.getInstr(addr);

            switch (nArgs[instr.op]) {
                case 3 :
                    System.out.println(String.format(fmt3Txt,opcodes[instr.op],Integer.toString(instr.r1),Integer.toString(instr.r2),
                                                     instr.useImm?"0x"+instr.imm.toString(16):"r"+Integer.toString(instr.r3),
                                                     addr.toString(16),instr.useImm));
                    break;
                case 2 : 
                    System.out.println(String.format(fmt2Txt,opcodes[instr.op],Integer.toString(instr.r1),"",
                                                     instr.useImm?"0x"+instr.imm.toString(16):"r"+Integer.toString(instr.r3),
                                                     addr.toString(16),instr.useImm));
                    break;
                case 1 :
                    System.out.println(String.format(fmt1Txt,opcodes[instr.op],"","",
                                                     instr.useImm?"0x"+instr.imm.toString(16):"r"+Integer.toString(instr.r3),
                                                     addr.toString(16),instr.useImm));
                    break;
                default:
                    TrCommon.warn("Unknown instruction at address 0x"+addr.toString(16));
            }
        }
    }

    public static void trTapeDasm(final List<TrTapeStore> stores, final String fmt) {
        int numIters = 0;
        for (int i = 0; i<stores.size(); ++i) {
            final TrTapeStore ts = stores.get(i);
            for ( Integer iter: ts.getTapeIters() )
                showTape(ts.getTapeIter(iter),numIters++,fmt);
        }
    }

    public static void showTape(final Map<Integer,List<BigInteger>> tapeIter, final int iter, final String fmt) {
        for (Integer tape : tapeIter.keySet())
        for (BigInteger tEnt : tapeIter.get(tape)) {
            System.out.println(String.format(fmt,iter,tape,tEnt.toString()));
        }
    }
}
