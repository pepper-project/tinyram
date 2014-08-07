package tinyram.ver0;

import java.math.BigInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;
import tinyram.TrProgram;
import tinyram.TrInstruction;
import tinyram.TrTranscript;
import tinyram.TrMemState;
import tinyram.TrMemTranscript;
import tinyram.sim.SimExec.SimInvalidOp;
import tinyram.sim.SimExec.SimPMemOOB;
import benesNetwork.BenesRouteError;

public class SimExec extends tinyram.sim.SimExec {
    private final SimState state;
    private final TrProgram prog;
    private final int numSteps;
    private final TrTranscript transcript = new TrTranscript();
    private TrMemTranscript memTranscript = null;

    public SimExec ( TrProgram prog, int numSteps , Map<Integer,List<BigInteger>> tapes ) {
        this.prog = prog;
        this.numSteps = numSteps;
        state = new SimState(prog.wordsize,prog.numRegBits,tapes);
    }

    // transcript-getter
    public TrTranscript getTranscript() { return transcript; } 
    public TrMemTranscript getMemTranscript() { return memTranscript; }

    // run until the program answers
    public BigInteger run() throws SimPMemOOB, SimInvalidOp, BenesRouteError {
        BigInteger retval = null;
        List<TrMemState> memUnsort = new ArrayList<TrMemState>(numSteps > 0 ? numSteps : 0);
        TrMemState currMem = null;

        TrInstruction instr;
        TrState prev, next;
        next = state.getState();

        // op() will return null except when the answer instruction is executed
        // if we have an unlimited number of steps, numSteps is -1 and thus will never break the loop
        int steps = numSteps;
        while (retval == null && steps != 0) {
            prev = next;
            transcript.add(prev);
            instr = prog.getInstr(prev.getPC());
            retval = op(instr);
            if (null == instr)
                throw new SimPMemOOB(String.format("Read uninitialized progmem at %s",state.getPC()));
            next = state.getState();
            currMem = TrMemState.newTrMemState(0, prev, next, numSteps - steps, instr);
            memUnsort.add(currMem);
            --steps;
        }

        assert retval != null; // we should have exited the above loop via an ANSWER instruction

        // keep dumping states until we hit numSteps
        prev = next;
        for ( ; steps > 0 ; --steps) {
            transcript.add(prev);
            memUnsort.add(currMem.copy(numSteps - steps));
        }

        // create the memory transcript object
        memTranscript = new TrMemTranscript(memUnsort,prog.wordsize);

        return retval;
    }

    // mutate the state through the next operation
    private BigInteger op (TrInstruction instr) throws SimInvalidOp {
        switch (instr.op) {
            case 0  : return opAND(instr);
            case 1  : return opOR(instr);
            case 2  : return opXOR(instr);
            case 3  : return opNOT(instr);
            case 4  : return opADD(instr);
            case 5  : return opSUB(instr);
            case 6  : return opMULL(instr);
            case 7  : return opUMULH(instr);
            case 8  : return opSMULH(instr);
            case 9  : return opUDIV(instr);
            case 10 : return opUMOD(instr);
            case 11 : return opSHL(instr);
            case 12 : return opSHR(instr);

            case 13 : return opCMPE(instr);
            case 14 : return opCMPA(instr);
            case 15 : return opCMPAE(instr);
            case 16 : return opCMPG(instr);
            case 17 : return opCMPGE(instr);

            case 18 : return opMOV(instr);
            case 19 : return opCMOV(instr);

            case 20 : return opJMP(instr);
            case 21 : return opCJMP(instr);
            case 22 : return opCNJMP(instr);

            case 28 : return opSTORE(instr);
            case 29 : return opLOAD(instr);
            case 30 : return opREAD(instr);
            case 31 : return opANSWER(instr);

            default : throw new SimInvalidOp(String.format("Invalid opcode %d",instr.op));
        }
    }

// processor operations
// we ALWAYS assume that the immediate is canonicalized!

// bitwise operations
// flag register is set is the result is 0
    // r1 = r2 & {r3 , imm}
    // flag = r1 == 0
    private BigInteger opAND (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,state.getReg(instr.r2).and(op2));
        state.setFlag(state.getReg(instr.r1).equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = r2 | {r3 , imm}
    // flag = r1 == 0
     private BigInteger opOR (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,state.getReg(instr.r2).or(op2));
        state.setFlag(state.getReg(instr.r1).equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = r2 ^ {r3 , imm}
    // flag = r1 == 0
     private BigInteger opXOR (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,state.getReg(instr.r2).xor(op2));
        state.setFlag(state.getReg(instr.r1).equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = ! {r3 , imm}
    // flag = r1 == 0
     private BigInteger opNOT (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,op2.not());
        state.setFlag(state.getReg(instr.r1).equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

// arithmetic operations
// flag indicates overflow
    // r1 = r2 + {r3, imm}
    // flag = overflow
    private BigInteger opADD (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).add(op2);
        state.setReg(instr.r1,res);
        // if res takes more than wordsize bits to represent, we overflowed
        state.setFlag(res.bitLength() > prog.wordsize);
        state.incPC();
        return null; 
    }

    // r1 = r2 - {r3, imm}
    // flag = borrow
    private BigInteger opSUB (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).subtract(op2);
        state.setReg(instr.r1,res);
        // if res is negative, we underflowed
        state.setFlag(res.compareTo(BigInteger.ZERO) == -1);
        state.incPC();
        return null; 
    }

    // multiply two unsigned operands; return lower <wordsize> bits
    // flag is set if result >= 2^wordsize
    private BigInteger opMULL (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).multiply(op2);
        state.setReg(instr.r1,res);
        // if res takes more than wordsize bits to represent, we overflowed
        state.setFlag(res.bitLength() > prog.wordsize);
        state.incPC();
        return null; 
    }

    // multiply two unsigned operands; return upper <wordsize> bits
    // flag is set if result >= 2^wordsize
    private BigInteger opUMULH (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).multiply(op2);
        state.setReg(instr.r1,res.shiftRight(prog.wordsize));
        // if res takes more than wordsize bits to represent, we overflowed
        state.setFlag(res.bitLength() > prog.wordsize);
        state.incPC();
        return null; 
    }

    // multiply two signed operands; return upper <wordsize> bits
    // flag is set if result >= 2^wordsize
    private BigInteger opSMULH (final TrInstruction instr) {
        final BigInteger op2 = state.common.signedValue(instr.useImm ? instr.imm : state.getReg(instr.r3),instr.useImm);
        final BigInteger res = state.common.signedValue(state.getReg(instr.r2),false).multiply(op2);
        // same as IBM Power series mulhw
        // multiply signed numbers together, return MSBs of two's complement result
        // see http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp?topic=/com.ibm.aix.aixassem/doc/alangref/mulhw.htm
        // example: 0x4500 * 0x80007000 = 17664 * -2147454976 = -37932644696064
        // -37932644696064 >> 32 = -8832 = 0xFFFFDD80
        // NOTE we can canonicalize to 64 bits then shift, or shift then canonicalize to 32 bits
        // we choose the latter since the canonicalization step will always happen when we set a register
        state.setReg(instr.r1,res.shiftRight(prog.wordsize));

        // as described in TinyRAM document
        // this makes NO sense to me!
        // final BigInteger res = state.common.signedValue(state.getReg(instr.r2)).multiply(op2);
        // state.setReg(instr.r1,
        //              res.signum() == -1
        //                ? res.abs().shiftRight(prog.wordsize+1).setBit(prog.wordsize-1)
        //                : res.abs().shiftRight(prog.wordsize+1).clearBit(prog.wordsize-1));

        // if res takes more than wordsize bits to represent, we overflowed
        state.setFlag(res.bitLength() + 1 > prog.wordsize);
        state.incPC();
        return null; 
    }

    // r1 = r2 / {r3 , imm}, or 0 if {r3 , imm} == 0
    // flag is set on divide by zero
    private BigInteger opUDIV (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        if (op2.equals(BigInteger.ZERO)) { // error on divide by zero
            state.setReg(instr.r1,BigInteger.ZERO);
            state.setFlag(true);
        } else {
            state.setReg(instr.r1,state.getReg(instr.r2).divide(op2));
            state.setFlag(false);
        }
        state.incPC();
        return null; 
    }

    // r1 = r2 % {r3 , imm}, or 0 if {r3 , imm} == 0
    // flag is set on divide by zero
     private BigInteger opUMOD (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        if (op2.equals(BigInteger.ZERO)) { // error on mod by zero
            state.setReg(instr.r1,BigInteger.ZERO);
            state.setFlag(true);
        } else {
            state.setReg(instr.r1,state.getReg(instr.r2).remainder(op2));
            state.setFlag(false);
        }
        state.incPC();
        return null; 
    }

    // r1 = r2 << {r3 , imm}
    // flag = r2[wordsize-1]
    private BigInteger opSHL (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final int shift = (int) op2.floatValue();
        // NOTE: in principle the above is unsafe. However, in practice we know
        // that the number of bits in a register is FAR less than maxint
        // Therefore all we need is to be sure that when op2 is > maxint, we
        // get the right behavior (i.e., shift = maxint, not shift = -1)
        // This is the case for floatValue
        state.setFlag(state.getReg(instr.r2).testBit(prog.wordsize-1));
        state.setReg(instr.r1,state.getReg(instr.r2).shiftLeft(shift));
        state.incPC();
        return null; 
    }

    // r1 = r2 >> {r3 , imm}
    // flag = r2[0]
     private BigInteger opSHR (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final int shift = (int) op2.floatValue();
        // ^^^ see comment in opSHL
        state.setFlag(state.getReg(instr.r2).testBit(0));
        state.setReg(instr.r1,state.getReg(instr.r2).shiftRight(shift));
        state.incPC();
        return null; 
    }

// comparison instructions
// what a waste. Just make r0 a constant-zero register
// and you get these operations for free
    // flag = r2 == {r3 , imm}
    private BigInteger opCMPE (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setFlag(state.getReg(instr.r2).equals(op2));
        state.incPC();
        return null; 
    }

    // flag = r2 > {r3 , imm} (unsigned)
    private BigInteger opCMPA (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setFlag(state.getReg(instr.r2).compareTo(op2) > 0);
        state.incPC();
        return null; 
    }

    // flag = r2 >= {r3 , imm} (unsigned)
    private BigInteger opCMPAE (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setFlag(state.getReg(instr.r2).compareTo(op2) >= 0);
        state.incPC();
        return null; 
    }

    // flag = r2 > {r3 , imm} (signed)
    private BigInteger opCMPG (final TrInstruction instr) {
        final BigInteger op2 = state.common.signedValue(instr.useImm ? instr.imm : state.getReg(instr.r3),instr.useImm);
        state.setFlag(state.common.signedValue(state.getReg(instr.r2),false).compareTo(op2) > 0);
        state.incPC();
        return null; 
    }

    // flag = r2 >= {r3 , imm} (signed)
    private BigInteger opCMPGE (final TrInstruction instr) {
        final BigInteger op2 = state.common.signedValue(instr.useImm ? instr.imm : state.getReg(instr.r3),instr.useImm);
        state.setFlag(state.common.signedValue(state.getReg(instr.r2),false).compareTo(op2) >= 0);
        state.incPC();
        return null; 
    }

// move instructions
    // r1 = {r3 , imm}
    // flag unchanged
    private BigInteger opMOV (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,op2);
        state.incPC();
        return null; 
    }

    // if (flag) { r1 = {r3 , imm} }
    // flag unchanged
     private BigInteger opCMOV (final TrInstruction instr) {
        if (state.getFlag()) {
            final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
            state.setReg(instr.r1,op2);
        }
        state.incPC();
        return null; 
    }

// jump instructions
    // PC = {r3 , imm}
    // flag unchanged
    private BigInteger opJMP (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setPC(op2);
        return null; 
    }

    // if (flag) { PC = {r3 , imm} }
    // else       { PC++ }
    // flag unchanged
     private BigInteger opCJMP (final TrInstruction instr) {
        if (state.getFlag()) {
            final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
            state.setPC(op2);
        } else { state.incPC(); }
        return null; 
    }

    // if (!flag) { PC = {r3 , imm} }
    // else       { PC++ }
    // flag unchanged
     private BigInteger opCNJMP (final TrInstruction instr) {
        if (!state.getFlag()) {
            final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
            state.setPC(op2);
        } else { state.incPC(); }
        return null; 
    }

// memory-related instructions
    // mem[{r3 , imm}] = r2
    // flag unchanged
    private BigInteger opSTORE (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setMem(op2,state.getReg(instr.r2));
        state.incPC();
        return null; 
    }

    // r1 = mem[{r3 , imm}]
    // flag unchanged
     private BigInteger opLOAD (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        state.setReg(instr.r1,state.getMem(op2));
        state.incPC();
        return null; 
    }

    // r1 = readTape[{r3 , imm}]
    // flag is 1 if tape is exhausted, 0 otherwise
    private BigInteger opREAD (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final int op2Int = (int) op2.floatValue();
        final BigInteger readValue;
        // should only be 0 or 1, so as with the shift instructions it's OK to cast this way
        switch (op2Int) {
            case 0 :
                readValue = state.readInTape();
                break;
            case 1 :
                readValue = state.readAuxTape();
                break;
            default:
                readValue = null;
        }
        if (readValue == null) {
            state.setReg(instr.r1,BigInteger.ZERO);
            state.setFlag(true);
        } else {
            state.setReg(instr.r1,readValue);
            state.setFlag(false);
        }
        state.incPC();
        return null; 
    }

    // answer with return value {r3 , imm}
    private BigInteger opANSWER (final TrInstruction instr) {
        return instr.useImm ? instr.imm : state.getReg(instr.r3);
    }
}
