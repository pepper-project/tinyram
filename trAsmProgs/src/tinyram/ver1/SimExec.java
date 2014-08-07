package tinyram.ver1;

import java.math.BigInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;
import tinyram.TrCommon;
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
        state = new SimState(prog,tapes);
    }

    // transcript-getter
    public TrTranscript getTranscript() { return transcript; } 
    public TrMemTranscript getMemTranscript() { return memTranscript; }

    // run until the program answers or we hit maxSteps
    public BigInteger run() throws SimPMemOOB, SimInvalidOp, BenesRouteError {
        if (transcript.size() > 0) // we never run more than once
            return null;

        List<TrMemState> memUnsort = new ArrayList<TrMemState>();

        // read in program memory
        final BigInteger maxAddr = prog.getAddrs().last();
        int i=0;

        /*
        // we must _always_ first write 0 to 0 on the first cycle to be sure
        // the very first entry in the Benes network output is constrained
        memUnsort.add(TrMemState.newTrMemState(1,BigInteger.ZERO,BigInteger.ZERO,i++,false));
        */
        // The above isn't true in ver1 because the Verifier must always write the program
        // into memory starting from location 0. This means that location 0 will be written
        // on the first step of the memory transcript, which wins all ties and sets the base
        // case for the memory verification

        // now read in program memory starting from address 0
        // YES this is ugly, but would stringifying each time be better?
        // note that we decide implicitly here that all programs will be written into memory starting from
        // address 0 to their end. Don't make your programs sparsely laid out or this will be a bitch!
        for (BigInteger progAddr = BigInteger.ZERO; progAddr.compareTo(maxAddr) < 1; progAddr=progAddr.add(BigInteger.ONE)) {
            // get instruction in proper format
            final BigInteger data = prog.getPackedInstr(progAddr);

            // canonicalize should not be necessary but belt & suspenders
            assert data.equals(state.common.canonicalize(data,false)) :
                "data != canonicalize(data) in run()";

            state.setMemInstr(progAddr,data,prog.getInstr(progAddr));
            memUnsort.add(TrMemState.newTrMemState(1,progAddr,data,i++,false));
        }

        // read input tape into memory
        if (null != state.inTape) {
            BigInteger tapePosn = prog.inTapeLoc;
            for (BigInteger tapeElm : state.inTape) {
                state.setMem(tapePosn,tapeElm);                                             // set memory
                memUnsort.add(TrMemState.newTrMemState(1,tapePosn,tapeElm,i++,false));      // add to memory transcript
                tapePosn = tapePosn.add(BigInteger.ONE);                                    // increment
            }
        }

        // read aux tape into memory
        if (null != state.auxTape) {
            BigInteger tapePosn = prog.auxTapeLoc;
            for (BigInteger tapeElm : state.auxTape) {
                state.setMem(tapePosn,tapeElm);                                             // set memory
                memUnsort.add(TrMemState.newTrMemState(1,tapePosn,tapeElm,i++,false));      // add to transcript
                tapePosn = tapePosn.add(BigInteger.ONE);                                    // increment
            }
        }
        final BigInteger memOffset = new BigInteger(Integer.toString(i));


        // set up "next" to be the initial state
        // all registers are clear except INSTR
        // INSTR contains the first instruction to be executed
        //    what's really happening here is that INSTR was fetched - we add a load to the memory transcript -
        //    but the transition verifier doesn't see that happen, and we *must* start from a known good state
        //    to be sure that we have properly constrained the prover. Thus, the value of the INSTR register
        //    in the starting state must be set in the constraints to be equal to the word at address 0
        //    in memory
        //    We don't actually have to put our program at 0 in memory, but the
        //    entry point for execution is always there. For example, address 0 could be left 0,
        //    which is effectively a no-op, and then address 1 could be an unconditional jump
        //    to somewhere else in memory.
        TrState prev, next;
        state.setInstr(state.getMem(BigInteger.ZERO));
        next = state.getState();
        TrMemState currMem = null;
        TrInstruction instr = null;
        BigInteger retval = null;
        // op() will return null except when the answer instruction is executed
        // if we have an unlimited number of steps, steps is -1 and thus will never break the loop
        int steps = numSteps;
        while (retval == null && steps != 0) {
            prev = next;
            assert prev.getReg(0).equals(BigInteger.ZERO) :
                "r0 != 0";  // enforce zero register invariant
            transcript.add(prev);
            // record memory transaction for instruction fetch
            memUnsort.add(TrMemState.newTrMemState(1,prev.getPC(),prev.getInstr(),i+2*(numSteps-steps),true));
            // decode instruction
            instr = state.instrDecode(prev.getInstr());
            // execute
            retval = op(instr);
            next = state.getState();
            // record memory transaction for instruction execution
            currMem = TrMemState.newTrMemState(1,prev,next,i+2*(numSteps-steps)+1,instr);
            memUnsort.add(currMem);
            --steps;
        }

        assert retval != null :
            "Program did not ANSWER"; // we should have exited the above loop via an ANSWER instruction

        // keep dumping states until we hit numSteps
        prev = next;
        i = i + 2*(numSteps-steps);
        for ( ; steps > 0 ; --steps) {
            transcript.add(prev);
            memUnsort.add(TrMemState.newTrMemState(1,prev.getPC(),prev.getInstr(),i++,true));
            // we can use currMem because any correct program ANSWERed which means that it is a non-memop
            memUnsort.add(currMem.copy(i++));
        }

        // at this point, i should equal the length of memUnsort
        assert i == memUnsort.size() :
            "memUnsort mysteriously has the wrong size after execution completed.";

        if (steps < 0) {
            System.err.println("Finished execution with " + Integer.toString(-1 * steps - 1) + " steps.");
        }

        // now we have to fill up the memTranscript until it is of length 2^k
        for ( steps = (int) Math.pow(2,TrCommon.bitLength(i)) - i ; steps > 0 ; --steps) {
            // just fill with non-memops until it's the appropriate size
            memUnsort.add(currMem.copy(i++));
        }

        // size should be a power of 2
        assert memUnsort.size() == (int) Math.pow(2,TrCommon.bitLength(memUnsort.size())) :
            "memUnsort mysteriously is not a power of two size after padding.";

        // create the memory transcript object
        memTranscript = new TrMemTranscript(memUnsort,prog.wordsize);
        // the prover starts filling in values at the following point in the unsorted list:
        memTranscript.setOffset(memOffset);

        return retval;
    }

    // mutate the state through the next operation
    private BigInteger op (TrInstruction instr) throws SimPMemOOB, SimInvalidOp {

        final BigInteger retVal;
        switch (instr.op) {
            case 0  : retVal =  opAND(instr);break;  // ri = rj & A
            case 1  : retVal =  opOR(instr);break;   // ri = rj | A
            case 2  : retVal =  opXOR(instr);break;  // ri = rj ^ A
            case 3  : retVal =  opNOT(instr);break;  // ri =    ~ A

            case 4  : retVal =  opADD(instr);break;  // ri = rj + A    // no sign extension of immediate
            case 5  : retVal =  opSUB(instr);break;  // ri = rj - A    // " (see SUBS for sign-extended immediate subtract)

            case 6  : retVal =  opMULL(instr);break; // ri = rj * A (lower word)
            case 7  : retVal =  opUMULH(instr);break;// ri = rj * A (high word, unsigned multiply)
            case 8  : retVal =  opSMULH(instr);break;// ri = rj * A (high word, signed multiply, immediate gets sign-extended)

            case 9  : retVal =  opUDIV(instr);break; // ri = rj / A
            case 10 : retVal =  opUMOD(instr);break; // ri = rj % A

            case 11 : retVal =  opSHL(instr);break;  // ri = A << rj -- NOTE this is different from ver0!
                                               // for shifting by a constant, use multiply instead
            //case 12 : return opSHR(instr);
            // SHR is too expensive. However,
            // SHR z x y can be rewritten as
            //     SHL t,y,1
            //     UDIV z,x,t
            // This saves a *lot* of constraints

            // CMPE rfoo,Abar is the same as
            //     XOR r0,rfoo,Abar
            //case 13 : return opCMPE(instr);
            //case 14 : return opCMPA(instr);
            //case 15 : return opCMPAE(instr);
            //case 16 : return opCMPG(instr);
            //case 17 : return opCMPGE(instr);
            // comparisons can be handled by SUB or SUBS depending if you need signed or unsigned comparison
            // Unsigned comparison:
            // ra < rb   -- SUB r0,ra,rb    -> true if flag is set (and >= is true if flag is unset)
            // ra <= rb  -- SUB r0,rb,ra    -> true if flag is unset (and > is true if flag is set)
            // ra < imm  -- SUB r0,ra,imm   -> true if flag is set (and >= is true if flag is unset)
            // ra <= imm -- SUB r0,ra,imm+1 -> true if flag is set (and > is true if flag is unset)
            // for signed comparison, the same applies but with SUBS, which sets the flag if signed(op1) < signed(op2)
            case 12 : retVal = opSUBS(instr);break;
            // SUBS is the same as SUB except that immediate is sign-extended and flag is set as described above

            // MOV rfoo,A is the same as
            // ADD rfoo,r0,A
            // case 18 : return opMOV(instr);
            //
            // since we have relative jumps, we can eliminate CMOV
            // CMOV rfoo,A becomes
            //     CNJMP 2
            //     ADD rfoo,r0,A
            // case 19 : return opCMOV(instr);

            // if A is an immediate, jump to [pc]+signExtend([A])
            // otherwise, absolute jump to value in r[A]
            case 20 : retVal =  opJMP(instr);break;
            case 21 : retVal =  opCJMP(instr);break;
            case 22 : retVal =  opCNJMP(instr);break;
            // don't *really* need both cjmp and cnjmp, we can negate the flag with 4 instructions:
            //    CJMP 4
            //    AND r0,r0,0
            //    CJMP 2
            //    OR r0,r0,1
            //    ...

            // these are different than in ver0
            // STORE ri,rj,A -> mem[rj + {r3, signExtend(A)}] = ri
            case 28 : retVal =  opSTORE(instr);break;
            // LOAD ri,rj,A  -> ri = mem[rj + {r3, signExtend(A)}]
            case 29 : retVal =  opLOAD(instr);break;

            // READ is obviated by our reading input and aux tapes into memory above
            // case 30 : retVal =  opREAD(instr);break;
            case 31 : retVal =  opANSWER(instr);break;

            default : throw new SimInvalidOp(String.format("Invalid opcode %d",instr.op));
        }

        // fetch next instruction
        state.setInstr(state.getMem(state.getPC()));
        return retVal;
    }

// processor operations
// we ALWAYS assume that the immediate is canonicalized!

// bitwise operations
// flag register is set is the result is 0
    // r1 = r2 & {r3 , imm}
    // flag = r1 == 0
    private BigInteger opAND (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).and(op2);
        state.setReg(instr.r1,res);
        state.setFlag(res.equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = r2 | {r3 , imm}
    // flag = r1 == 0
     private BigInteger opOR (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).or(op2);
        state.setReg(instr.r1,res);
        state.setFlag(res.equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = r2 ^ {r3 , imm}
    // flag = r1 == 0
     private BigInteger opXOR (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = state.getReg(instr.r2).xor(op2);
        state.setReg(instr.r1,res);
        state.setFlag(res.equals(BigInteger.ZERO));
        state.incPC();
        return null; 
    }

    // r1 = ! {r3 , imm}
    // flag = r1 == 0
     private BigInteger opNOT (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final BigInteger res = op2.not();
        state.setReg(instr.r1,res);
        state.setFlag(res.equals(BigInteger.ZERO));
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

    // r1 = r2 - {r3, signExtend(imm)}
    // flag = signedValue(r2) < signedValue({r3, imm})
    private BigInteger opSUBS (final TrInstruction instr) {
        final BigInteger op1 = state.common.signedValue(state.getReg(instr.r2),true);
        final BigInteger op2 = state.common.signedValue(instr.useImm ? instr.imm : state.getReg(instr.r3),instr.useImm);
        state.setReg(instr.r1,op1.subtract(op2));
        // flag is signedValue(op1) < signedValue(op2)
        state.setFlag(op1.compareTo(op2) == -1);
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

    // r1 = {r3 , imm} << r2
    // flag = {r3 , imm}[wordsize-1]
    private BigInteger opSHL (final TrInstruction instr) {
        final BigInteger op2 = instr.useImm ? instr.imm : state.getReg(instr.r3);
        final int shift = (int) state.getReg(instr.r2).floatValue();
        // NOTE: in principle the above is unsafe. However, in practice we know
        // that the number of bits in a register is FAR less than maxint
        // Therefore all we need is to be sure that when op2 is > maxint, we
        // get the right behavior (i.e., shift = maxint, not shift = -1)
        // This is the case for floatValue
        state.setFlag(op2.testBit(prog.wordsize-1));
        state.setReg(instr.r1,op2.shiftLeft(shift));
        state.incPC();
        return null; 
    }

// jump instructions
    // PC = {r3, PC+signExtend(imm)}
    // flag unchanged
    private BigInteger opJMP (final TrInstruction instr) {
        final BigInteger op2;
        if (instr.useImm) {
            op2 = state.common.signedValue(instr.imm,true).add(state.getPC());
        } else {
            op2 = state.getReg(instr.r3);
        }
        state.setPC(op2);
        return null; 
    }

    // if (flag) { PC = {r3 , PC+signExtend(imm)} }
    // else       { PC++ }
    // flag unchanged
     private BigInteger opCJMP (final TrInstruction instr) {
        if (state.getFlag()) {
            return opJMP(instr);
        } else {
            state.incPC();
            return null; 
        }
    }

    // if (!flag) { PC = {r3 , PC+signExtend(imm)} }
    // else       { PC++ }
    // flag unchanged
     private BigInteger opCNJMP (final TrInstruction instr) {
        if (!state.getFlag()) {
            return opJMP(instr);
        } else { 
            state.incPC();
            return null;
        }
    }

// memory-related instructions
    // mem[{r3 , signExtend(imm)}+r2] = r1
    // flag unchanged
    private BigInteger opSTORE (final TrInstruction instr) {
        final BigInteger op2;
        if (instr.useImm) {
            op2 = state.getReg(instr.r2).add(state.common.signedValue(instr.imm,true));
        } else {
            op2 = state.getReg(instr.r2).add(state.getReg(instr.r3));
        }
        state.setMem(op2,state.getReg(instr.r1));
        state.incPC();
        return null; 
    }

    // r1 = mem[r2+{r3 , signExtend(imm)}]
    // flag unchanged
     private BigInteger opLOAD (final TrInstruction instr) {
         final BigInteger op2;
         if (instr.useImm) {
             op2 = state.getReg(instr.r2).add(state.common.signedValue(instr.imm,true));
         } else {
             op2 = state.getReg(instr.r2).add(state.getReg(instr.r3));
         }
         state.setReg(instr.r1,state.getMem(op2));
         state.incPC();
         return null; 
    }

    /*
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
    */

    // answer with return value {r3 , imm}
    private BigInteger opANSWER (final TrInstruction instr) {
        return instr.useImm ? instr.imm : state.getReg(instr.r3);
    }
}
