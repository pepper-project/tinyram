package tinyram.asm;

import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.CommonTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;
import tinyram.TrCommon;
import tinyram.TrProgram;
import tinyram.TrInstruction;
import tinyram.TrTapeStore;

public class AsmWalker extends TrAsmWalk {
    final AsmState state;
    final String field5;
    final Map<String,String> opMap = new HashMap<String,String>();
    final Map<String,Integer> argMap = new HashMap<String,Integer>();
    final static BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
    final String fmtTTxt;
    final String fmt3Txt;
    final String fmt2Txt;
    final String fmt1Txt;
    final TrCommon common;
    final TrProgram prog;
    final TrTapeStore tapeStore;
    final Map<BigInteger,TrInstruction> pmem;
    final Map<Integer,Map<Integer,List<BigInteger>>> tapes;

    public AsmWalker(TreeNodeStream input, AsmState inState) {
        super(input);

        // take the state from the prewalker and reset the program counter
        super.asmState = inState;
        state = super.asmState;
        state.resetPC();
        state.asmWalk = 1;

        // make sure that we are attempting to assemble something reasonable!
        if ( 0 == state.wordsize || 0 == state.numRegBits ) {
            throw (new IllegalArgumentException("Missing or invalid TinyRAM directive"));
        }

        // common utilities
        common = new TrCommon ( state.wordsize, state.numRegBits, state.version );

        // for text formatting
        int fieldsize = state.wordsize / 3 + 2;
        fmtTTxt = String.format("tape   %%-%ds , %%-%ds , %%-%ds ; tape value"        ,fieldsize,fieldsize,fieldsize);
        fmt3Txt = String.format("%%-6s %%-%ds , %%-%ds , %%-%ds ; addr=0x%%s useIm=%%b",fieldsize,fieldsize,fieldsize);
        fmt2Txt = String.format("%%-6s %%-%ds , %%-%ds   %%-%ds ; addr=0x%%s useIm=%%b",fieldsize,fieldsize,fieldsize);
        fmt1Txt = String.format("%%-6s %%-%ds   %%-%ds   %%-%ds ; addr=0x%%s useIm=%%b",fieldsize,fieldsize,fieldsize);

        // for other formatting
        if ( 2*state.numRegBits + 6 > state.wordsize ) {
            throw (new IllegalArgumentException("Impossible wordsize/#reg combination! W >= 6 + 2*log2(K)"));
        }
        field5 = String.format(String.format("%%0%dd",state.wordsize - 6 - 2*state.numRegBits),0);

        // fill up the opcode map
        for (int i=0; i<common.opcodes.length; i++) {
            opMap.put(common.opcodes[i],TrCommon.int2zpBin(i,5));
            argMap.put(common.opcodes[i],common.nArgs[i]);
        }
        opMap.remove("");
        argMap.remove("");

        // generate a program in output formats 2 and 3
        if (state.outform == 2 || state.outform == 3) {
            pmem = new HashMap<BigInteger,TrInstruction>();
            prog = new TrProgram (state.version,state.wordsize,state.numRegBits,common.nImmBits(),pmem,state.inTapeLoc,state.auxTapeLoc);
        } else {
            pmem = null;
            prog = null;
        }

        // generate input and aux tapes in formats 2 and 4
        if (state.outform == 2 || state.outform == 4) {
            tapes = new HashMap<Integer,Map<Integer,List<BigInteger>>>();
            tapeStore = new TrTapeStore (tapes,state.wordsize);
        } else {
            tapes = null;
            tapeStore = null;
        }
    }

// ***
// N-ARG ASSEMBLER FUNCTIONS
// ***
    // assemble 3-operand instruction
    protected void asmOpR(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,r1,r2,r3,null,false); break;
            case 1 : asmOpBin(mnemonic,r1,r2,r3,null,false); break;
            case 0 :
            default: asmOpTxt(mnemonic,r1,r2,r3,null,false);
        }
        state.incPC();
    }
    protected void asmOpI(CommonTree mnemonic, CommonTree r1, CommonTree r2, BigInteger im) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,r1,r2,null,im,true); break;
            case 1 : asmOpBin(mnemonic,r1,r2,null,im,true); break;
            case 0 :
            default: asmOpTxt(mnemonic,r1,r2,null,im,true);
        }
        state.incPC();
    }

    // assemble 2-operand instruction
    protected void asmOpR(CommonTree mnemonic, CommonTree r1, CommonTree r2) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,r1,r2,null,false); break;
            case 1 : asmOpBin(mnemonic,r1,r2,null,false); break;
            case 0 :
            default: asmOpTxt(mnemonic,r1,r2,null,false);
        }
        state.incPC();
    }
    protected void asmOpI(CommonTree mnemonic, CommonTree r1, BigInteger im) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,r1,null,im,true); break;
            case 1 : asmOpBin(mnemonic,r1,null,im,true); break;
            case 0 :
            default: asmOpTxt(mnemonic,r1,null,im,true);
        }
        state.incPC();
    }

    // assemble 1-operand instruction
    protected void asmOpR(CommonTree mnemonic, CommonTree r1) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,r1,null,false); break;
            case 1 : asmOpBin(mnemonic,r1,null,false); break;
            case 0 :
            default: asmOpTxt(mnemonic,r1,null,false);
        }
        state.incPC();
    }
    protected void asmOpI(CommonTree mnemonic, BigInteger im) {
        switch (state.outform) {
            case 4 : break; // 4 does not produce instruction outputs
            case 3 :        // 3 and 2 produce java objects
            case 2 : asmOpProg(mnemonic,null,im,true); break;
            case 1 : asmOpBin(mnemonic,null,im,true); break;
            case 0 :
            default: asmOpTxt(mnemonic,null,im,true);
        }
        state.incPC();
    }

// ***
// TAPE ASSEMBLING FUNCTIONS
// ***
    // 2-arg tape instruction uses the state's tapeIter value
    // this is useful because we can set the iter with the $ tapeIter directive
    protected void tapeOp(final CommonTree mnemonic, final BigInteger tapeNum, final BigInteger value) {
        // yes, (int) floatValue is intentional. This clips the value rather than wrapping
        tapeOp(mnemonic, state.tapeIter, (int) tapeNum.floatValue(), value);
    }

    // process 3-arg tape instruction from the syntax
    protected void tapeOp(final CommonTree mnemonic, final BigInteger iter, final BigInteger tapeNum, final BigInteger value) {
        // yes, (int) floatValue is intentional. This clips the value rather than wrapping
        tapeOp(mnemonic, (int) iter.floatValue(), (int) tapeNum.floatValue(), value);
    }

    // handle tape stuff
    protected void tapeOp(final CommonTree mnemonic, final int tapeIter, final int tapeNum, final BigInteger value) {
        if (! mnemonic.getText().toLowerCase().equals("tape")) {
            TrCommon.warn(String.format("Invalid opcode %s or syntax error at %d:%d.\nOnly tape opcode takes multiple immediates.",
                                     mnemonic.getText(),mnemonic.getLine(),mnemonic.getCharPositionInLine()));
            return;
        }

        switch (state.outform) {
            case 3 : break; // 3 does not produce any tape outputs
            case 4 :        // 4 and 2 produce java object tapes
            case 2 : tapeOpProg(tapeIter, tapeNum, value); break;
            case 1 : tapeOpBin(tapeIter, tapeNum, value); break;
            case 0 :
            default: tapeOpTxt(tapeIter, tapeNum, value); break;
        }
    }

// ***
// TRPROGRAM EMITTING FUNCTIONS
// ***
    protected void asmOpProg(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3, BigInteger im, boolean useIm) {
        asmOpProg(mnemonic,r1,r2,r3,im,useIm,3);
    }
    protected void asmOpProg(CommonTree mnemonic, CommonTree r1, CommonTree r2, BigInteger im, boolean useIm) {
        asmOpProg(mnemonic,r1,r1,r2,im,useIm,2);
    }
    protected void asmOpProg(CommonTree mnemonic, CommonTree r1, BigInteger im, boolean useIm) {
        asmOpProg(mnemonic,null,null,r1,im,useIm,1);
    }
    protected void asmOpProg(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3, BigInteger im, boolean  useIm, int nargs) {
        final String opcode = opMap.get(mnemonic.getText().toLowerCase());
        if (null == opcode || argMap.get(mnemonic.getText().toLowerCase()) != nargs) {
            TrCommon.warn(String.format("Invalid opcode %s or incorrect #args at %d:%d",
                                     mnemonic.getText(),mnemonic.getLine(),mnemonic.getCharPositionInLine()));
        } else {
            final TrInstruction instr = new TrInstruction(Integer.parseInt(opcode,2),
                                                          r1 == null ? 0 : reg2int(r1),
                                                          r2 == null ? 0 : reg2int(r2),
                                                          r3 == null ? 0 : reg2int(r3),
                                                          useIm ? common.canonicalize(im,true) : null,
                                                          useIm);
            pmem.put(state.pc,instr);
        }
    }

    protected int reg2int(CommonTree reg) {
        int regint = Integer.parseInt(reg.getText().substring(1));
        if (regint >= (int) Math.pow(2,state.numRegBits)) {
            TrCommon.warn(String.format("Impossible register number %s for numRegBits %d at %d:%d",
                        reg.getText(),state.numRegBits,reg.getLine(),reg.getCharPositionInLine()));
            regint = 0;
        }
        return regint;
    }

    protected void tapeOpProg (final int tapeIter, final int tapeNum, final BigInteger value) {
        List<BigInteger> tape;

        Map<Integer,List<BigInteger>> iter = tapes.get(tapeIter);
        if (null == iter) { // new iteration
            iter = new HashMap<Integer,List<BigInteger>>();
            tapes.put(tapeIter,iter);
        }

        tape = iter.get(tapeNum);
        if (null == tape) { // new tape
            tape = new ArrayList<BigInteger>();
            iter.put(tapeNum,tape);
        }

        tape.add(value);
    }

// ***
// CONSTRAINT VARIABLE EMITTING FUNCTIONS
// ***
    protected void asmOpBin(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3, BigInteger im, boolean useIm) {
        asmOpBin(mnemonic,r1,r2,r3,im,useIm,3);
    }
    protected void asmOpBin(CommonTree mnemonic, CommonTree r1, CommonTree r2, BigInteger im, boolean useIm) {
        asmOpBin(mnemonic,r1,r1,r2,im,useIm,2);
    }
    protected void asmOpBin(CommonTree mnemonic, CommonTree r1, BigInteger im, boolean useIm) {
        asmOpBin(mnemonic,null,null,r1,im,useIm,1);
    }
    protected void asmOpBin(CommonTree mT, CommonTree r1, CommonTree r2, CommonTree r3, BigInteger im, boolean useIm, int nargs) {
        final String mnemonic = mT.getText();
        final String opcode = opMap.get(mnemonic.toLowerCase());
        if (null == opcode || argMap.get(mnemonic.toLowerCase()) != nargs) {
            TrCommon.warn(String.format("Invalid opcode %s or incorrect #args at %d:%d",
                                     mnemonic,mT.getLine(),mT.getCharPositionInLine()));
        } else {
            final String word0String = opcode + 
                                       (useIm ? "1" : "0") + 
                                       (nargs >= 2 ? reg2zpBin(r1,state.numRegBits) : TrCommon.int2zpBin(0,state.numRegBits)) +
                                       (nargs >= 2 ? reg2zpBin(r2,state.numRegBits) : TrCommon.int2zpBin(0,state.numRegBits)) +
                                       field5;
            final BigInteger word0 = new BigInteger(word0String,2);
            final BigInteger word1 = useIm ? common.canonicalize(im,true) : new BigInteger(Integer.toString(reg2int(r3)));

            System.out.print(String.format("0x%x",state.pc)+" 0x");
            System.out.print(word0.toString(16)+" 0x");
            System.out.println(word1.toString(16));
        }
    }

    // emit tape value
    protected void tapeOpBin(final int tapeIter, final int tapeNum, final BigInteger value) {
        System.out.println(String.format("TAPE 0x%x 0x%x 0x%s",tapeIter,tapeNum,value.toString(16)));
    }

    // register name (e.g., "r32") to zero-padded binary string
    protected String reg2zpBin(CommonTree reg, int len) { return TrCommon.int2zpBin(reg2int(reg),len); }

// ***
// TEXT EMITTING FUNCTIONS
// ***
    protected void asmOpTxt(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3, BigInteger im, boolean useIm) {
        asmOpTxt(mnemonic,r1,r2,r3,im,useIm,3);
    }
    protected void asmOpTxt(CommonTree mnemonic, CommonTree r1, CommonTree r2, BigInteger im, boolean useIm) {
        asmOpTxt(mnemonic,r1,r2,null,im,useIm,2);
    }
    protected void asmOpTxt(CommonTree mnemonic, CommonTree r1, BigInteger im, boolean useIm) {
        asmOpTxt(mnemonic,r1,null,null,im,useIm,1);
    }
    protected void asmOpTxt(CommonTree mT, CommonTree r1T, CommonTree r2T, CommonTree r3T, BigInteger im, boolean useIm, int nargs) {
        final String mnemonic = mT.getText();
        final String r1 = r1T == null ? null : r1T.getText();
        final String r2 = r2T == null ? null : r2T.getText();
        final String r3 = r3T == null ? null : r3T.getText();
        switch (nargs) {
            case 3 :
                System.out.println(String.format(fmt3Txt,mnemonic,r1,r2,useIm ? "0x"+im.toString(16) : r3,state.pc.toString(16),useIm));
                break;
            case 2 :
                System.out.println(String.format(fmt2Txt,mnemonic,r1,"",useIm ? "0x"+im.toString(16) : r2,state.pc.toString(16),useIm));
                break;
            case 1 :
            default:
                System.out.println(String.format(fmt1Txt,mnemonic,"","",useIm ? "0x"+im.toString(16) : r1,state.pc.toString(16),useIm));
        }
    }

    // emit tape text
    protected void tapeOpTxt (final int tapeIter, final int tapeNum, final BigInteger value) {
        System.out.println(String.format(fmtTTxt,tapeIter,tapeNum,value.toString()));
    }
}
