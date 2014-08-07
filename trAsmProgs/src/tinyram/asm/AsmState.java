package tinyram.asm;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.math.BigInteger;
import org.antlr.runtime.tree.CommonTree;
import tinyram.TrCommon;

public class AsmState { 
    final Map<String,BigInteger> labelMap = new HashMap<String,BigInteger>();
    BigInteger pc = BigInteger.ZERO;
    int tapeIter = 0;
    int version = 0;
    int outform = 0;
    int wordsize = 0;
    int numRegBits = 0;
    int asmWalk = 0;
    BigInteger inTapeLoc = BigInteger.ZERO;
    BigInteger auxTapeLoc = BigInteger.ZERO;

    // dump the state
    protected void dumpState() {
        System.err.println(String.format("pc : %d",pc));
        System.err.println(String.format("ver: %d",version));
        System.err.println(String.format("ws : %d",wordsize));
        System.err.println(String.format("nr : %d",numRegBits));
        System.err.println("Labels:");
        for (String k : labelMap.keySet()) {
            System.err.println(String.format("    %s = %s",k,labelMap.get(k).toString()));
        }
    }

// ***
// PROGRAM COUNTER
// ***
    // increment the program counter
    protected void incPC() { pc = pc.add(BigInteger.ONE); }

    // reset program counter (used after prewalk)
    protected void resetPC() { pc = BigInteger.ZERO; }

// ***
// LABELS
// ***
    // add a label at present address
    protected void addLabel(final String labName) { labelMap.put(labName,pc); }

    // add a label with the given value
    protected void addLabel(final String labName, final BigInteger labVal) {
        labelMap.put(labName,labVal);
    }

    // get a label value, enhanced error reporting
    protected BigInteger getLabel(final CommonTree label) {
        BigInteger retval = labelMap.get(label.getText());
        if (null == retval) {
            TrCommon.warn(String.format("Attempt to resolve undefined label %s at %d:%d",
                               label.getText(),label.getLine(),label.getCharPositionInLine()));
            return BigInteger.ZERO;
        } else {
            return retval;
        }
    }


// ***
// IMMEDIATES
// ***
    // BigInteger support for immediates and the program counter
    protected BigInteger getImmed(final CommonTree immed) { return getImmed(immed,BigInteger.ZERO); }
    protected BigInteger getImmed(final CommonTree immed, final BigInteger dflt) {
        BigInteger imm = dflt;
        TrCommon.Tuple<Integer,String> imBS = getImmedBaseString(immed.getText());
        try { imm = new BigInteger(imBS.snd, imBS.fst); }
        catch (NumberFormatException ex) {
            TrCommon.warn(String.format("Malformed immediate %s at %d:%d: %s",
                               immed.getText(),immed.getLine(),immed.getCharPositionInLine(),ex.toString()));
        }
        return imm;
    }

    // parse the string to figure out its base etc
    protected TrCommon.Tuple<Integer,String> getImmedBaseString(final String im) {
        int base = 10;
        String immed = im;
        boolean isneg = false;
        if (immed.length() > 0 && immed.substring(0,1).equals("-")) {
            isneg = true;
            immed = immed.substring(1);
        }
        if (immed.length() > 1)
            switch (immed.substring(0,2).toLowerCase()) {
                case "0x" : base = 16;
                            immed = (isneg ? "-" : "") + immed.substring(2);
                            break;
                case "0b" : base = 2;
                            immed = (isneg ? "-" : "") + immed.substring(2);
                            break;
                case "0o" : base = 8;
                            immed = (isneg ? "-" : "") + immed.substring(2);
                            break;
                default   : immed = (isneg ? "-" : "") + immed;
            }
        else immed = (isneg ? "-" : "") + immed;
        return new TrCommon.Tuple<Integer,String>(base,immed);
    }

// ***
// OPERATORS
// ***
    // arithmetic operation
    protected BigInteger expOp(final String op, final BigInteger i1, final BigInteger i2) {
        switch (op) {
            case "-" : return i1.subtract(i2);
            case "/" : return i1.divide(i2);
            case "*" : return i1.multiply(i2);
            case "%" : return i1.remainder(i2);
            case "&" : return i1.and(i2);
            case "|" : return i1.or(i2);
            case "^" : return i1.xor(i2);
            case ">>" : return i1.shiftRight(i2.intValue());
            case "<<" : return i1.shiftLeft(i2.intValue());
            case "+" :
            default  : return i1.add(i2);
        }
    }

    protected BigInteger expOp(String op, BigInteger i1) {
        switch (op) {
            case "~" : return i1.negate();
            case ">" : return i1.subtract(pc);
            case "!" :
            default  : return i1.not();
        }
    }

// ***
// ASSEMBLER DIRECTIVES
// ***
    // handle assembler directives
    protected void asmDirective (CommonTree dirToken, List<BigInteger> dirArgs) {
        switch (dirToken.getText().toLowerCase()) {
            case "tinyram" :
                if (dirArgs.size() < 4) { 
                    TrCommon.warn("TinyRAM directive takes 4 args: version, outform, wordsize, #regs\ninvalid directive ignored");
                } else if ( asmWalk==0 && wordsize == 0 && numRegBits == 0 ) {  // only the first TinyRAM directive is respected
                    version  = dirArgs.get(0).intValue();
                    outform  = dirArgs.get(1).intValue();
                    wordsize = dirArgs.get(2).intValue();
                    numRegBits  = dirArgs.get(3).intValue();
                    if (numRegBits > 16) TrCommon.warn("Suspiciously large number of register bits requested.");
                } else if ( asmWalk==1 ) { // emit TinyRAM tag in output stream in AsmWalk, but only the first time
                    switch (outform) {
                        case 4 :
                        case 3 :
                        case 2 : break ;
                        case 1 :
                        case 0 :
                        default: System.out.println(String.format("$ TinyRAM %d %d %d %d",version,outform,wordsize,numRegBits));
                    }
                }
                break;
            case "org" :
                if (dirArgs.size() < 1) {
                    TrCommon.warn("org directive takes 1 arg\ninvalid directive ignored");
                } else { 
                    pc = dirArgs.get(0);
                    if ( asmWalk > 0 && outform == 0) { // emit org directives in output stream when in text mode
                        System.out.println(String.format("$ org 0x%s",pc.toString(16)));
                    }
                }
                break;
            case "iter" :
                if (dirArgs.size() < 1) {
                    TrCommon.warn("iter directive takes 1 arg\ninvalid directive ignored");
                } else {
                    tapeIter = (int) dirArgs.get(0).floatValue(); // (int) floatValue clips rather than wrapping
                }
                break;
            case  "intape" :
                if (dirArgs.size() < 1) {
                    TrCommon.warn("inTape directive takes 1 arg\ninvalid directive ignored");
                } else {
                    inTapeLoc = dirArgs.get(0);
                    addLabel("_inTape",inTapeLoc);
                }
                break;
            case  "auxtape" :
                if (dirArgs.size() < 1) {
                    TrCommon.warn("auxTape directive takes 1 arg\ninvalid directive ignored");
                } else {
                    auxTapeLoc = dirArgs.get(0);
                    addLabel("_auxTape",auxTapeLoc);
                }
                break;
            default : 
                TrCommon.warn(String.format("unknown assembler directive %s at %d:%d"
                                   ,dirToken.getText(),dirToken.getLine(),dirToken.getCharPositionInLine()));
        }
    }
}
