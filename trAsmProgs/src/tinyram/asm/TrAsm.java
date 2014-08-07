package tinyram.asm;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import tinyram.TrCommon;

public class TrAsm {
    public static void main(String[] args) throws Exception {
        CharStream input = null;
        String outfile = "";
        int trFormat = -1;
        boolean writeC = false;
        boolean writeT = false;
        boolean packed = false;
        boolean writeInputGen = false;
        int defN = -1;
        int defI = -1;
        int defA = -1;
        // -f <format>  - force the format to <format> instead of what's specified in the asm file
        // -o <outfile> - dump output to <outfile> (formats 2, 3, 4 only)
        // -c           - generate C for use with the transition verifier (formats 2, 3, 4 only)
        // -cP          - generate C for Program
        // -cT          - generate C for Tapes
        // -cP and -cT can be specified together; this is equivalent to -c
        // -p           - generate packed program
        // -D <N> <I> <A> - output program size DEFINEs
        //                N is number of program steps
        //                I is length of input tape
        //                A is length of aux tape
        //                (only effective if C output is turned on)
        // -i           - in C mode, write mpq_set_ui calls for the program and/or tapes
        // If you specify an option with an argument multiple times, the last argument wins
        for (int i=0; i<args.length; ++i) {
            if (args[i].equals("-p")) {
                packed = true;
            } else if (args[i].equals("-cP")) {
                writeC = true;
            } else if (args[i].equals("-cT")) {
                writeT = true;
            } else if (args[i].equals("-c")) {
                writeC = true;
                writeT = true;
            } else if (args.length > i+3 && args[i].equals("-D")) {
                try {
                    defN = Integer.parseInt(args[i+1]);
                    defI = Integer.parseInt(args[i+2]);
                    defA = Integer.parseInt(args[i+3]);
                } catch (NumberFormatException ex) {
                    TrCommon.err("Could not parse \"-D "+args[i+1]+" "+args[i+2]+" "+args[i+3]+"\": "+ex.toString());
                    defN = -1;
                    defI = -1;
                    defA = -1;
                }
                i += 3;
            } else if (args[i].equals("-i")) {
            		writeInputGen = true;
            } else if (args.length > i+1 && args[i].equals("-f")) {
                try { trFormat = Integer.parseInt(args[++i]); }
                catch (NumberFormatException ex) {
                    TrCommon.err("Could not parse \"-f "+args[i]+"\": "+ex.toString());
                    trFormat = -1;
                }
            } else if (args.length > i+1 && args[i].equals("-o")) {
                outfile = args[++i];
            } else if (null == input && new File(args[i]).isFile()) {
                input = new ANTLRFileStream(args[i]);
                if (outfile.equals("")) {
                    final int outloc = args[i].lastIndexOf('.');
                    outfile = (-1 == outloc) ? args[i] : args[i].substring(0,outloc);
                }
            }
        }
        if (null == input) input = new ANTLRInputStream(System.in);
        if (outfile.equals("-")) outfile = "";

        // lex and parse
        final TrAssemblerLexer lex = new TrAssemblerLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lex);
        final TrAssemblerParser par = new TrAssemblerParser(tokens);
        final RuleReturnScope r = par.asmFile();

        // prewalk
        final CommonTree tree = (CommonTree) r.getTree();
        //System.out.println(tree.toStringTree());    // show us this tree
        final CommonTreeNodeStream pnodes = new CommonTreeNodeStream(tree);
        final TrPreWalk pwalk = new TrPreWalk(pnodes);
        pwalk.asmFile();
        //pwalk.asmState.dumpState();                 // state after prewalk

        // asmwalk
        final CommonTreeNodeStream anodes = new CommonTreeNodeStream(tree);
        // if the user forced a format on the commandline, change it before the final walk
        if (trFormat >= 0 && trFormat <= 4) { pwalk.asmState.outform = trFormat; }
        final AsmWalker awalk = new AsmWalker(anodes,pwalk.asmState);
        awalk.asmFile();

        // get rid of useless arguments in restricted write modes
        if (awalk.state.outform == 4) writeC = false;
        if (awalk.state.outform == 3) writeT = false;
        // serialize the results depending on output format
        if (awalk.state.outform >= 2 && awalk.state.outform <= 4) {
            final ObjectOutputStream oout;

            try {
                if (! outfile.equals(""))
                    oout = new ObjectOutputStream(new FileOutputStream(outfile+".jo"));
                else if (! (writeC || writeT))
                    oout = new ObjectOutputStream(System.out);
                else
                    oout = null;

                // write the program (binary and/or C versions)
                int genOffset = 0;
                if (awalk.state.outform != 4) {
                    if (null != oout) {
                        oout.writeObject(awalk.prog);
                    }

                    if (writeC) {
                        if (! outfile.equals("")) {
                            final PrintStream cout = new PrintStream(outfile+"_prog.c");
                            cout.print(awalk.prog.toC(packed));
                            if ( (defN > 0) && (defI > -1) && (defA > -1) ) {
                                // also emit DEFINEs for trVer
                                cout.print(genDefines(defN,defI,defA));
                            }
                            cout.flush();
                            cout.close();
                        } else {
                            System.out.print(awalk.prog.toC(packed));
                        }

                        if (writeInputGen) {
                            final PrintStream inputGenStr = new PrintStream(outfile + "_prog_input");
                            final StringBuilder s = new StringBuilder();
                            genOffset = awalk.prog.toInputGen(0, s);
                            inputGenStr.print(s.toString());
                            inputGenStr.close();
                        }
                    }
                }

                // write the tapes (binary and/or C versions)
                if (awalk.state.outform != 3) {
                    if (null != oout) {
                        oout.writeObject(awalk.tapeStore);
                    }


                    if (writeT) {
                        final Map<Integer,String> ctapes = awalk.tapeStore.toC();
                        final Map<Integer,String> gtapes;

                        if (writeInputGen) {
                            gtapes = new HashMap<Integer,String>();
                            genOffset += awalk.tapeStore.toInputGen(genOffset,gtapes);
                        } else {
                            gtapes = null;
                        }

                        for (Integer iter : ctapes.keySet()) {
                            final String nameBase = String.format("%s_tape_%06x",outfile,iter);
                            final PrintStream cout;

                            if (outfile.equals("")) {
                                cout = System.out;
                                cout.println("// TAPE ITERATION # "+Integer.toString(iter));
                            } else {
                                cout = new PrintStream(nameBase+".c");
                            }

                            cout.print(ctapes.get(iter));
                            cout.flush();

                            if (! outfile.equals("")) cout.close();

                            if (writeInputGen) {
                                final PrintStream genOut = new PrintStream(nameBase+"_input");

                                genOut.print(gtapes.get(iter));
                                genOut.flush();
                                genOut.close();
                            }
                        }
                    }
                }

                // close the object stream if open
                if (null != oout) {
                    oout.flush();
                    oout.close();
                }
            } catch (Exception ex) {
                System.out.println("Error writing output file(s): "+ex.toString());
            }
        }
    }

    private static String genDefines(int N, int I, int A) {
        final StringBuilder s = new StringBuilder();
        final int logNSwitches = TrCommon.bitLength(2*N+I+A);
        final int nSwitches = (int) Math.pow(2,logNSwitches-1);
        final int nStages = 2*logNSwitches - 1;
        s.append(  "#ifndef TR_TRANS_VER_H");
        s.append("\n#define TR_NUMSTEPS ").append(N);
        s.append("\n#define TR_INTAPELEN ").append(I);
        s.append("\n#define TR_AUXTAPELEN ").append(A);
        s.append("\n#define TR_BENES_SWITCHES ").append(nSwitches);
        s.append("\n#define TR_BENES_STAGES ").append(nStages);
        s.append("\n#endif  // TR_TRANS_VER_H\n");

        return s.toString();
    }
}
