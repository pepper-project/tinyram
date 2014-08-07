package tinyram.ver1;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import tinyram.TrCommon;
import tinyram.TrProgram;
import tinyram.TrInstruction;

public class SimState {

    // for converting BigIntegers to TrInstructions
    private final BigInteger opMask;
    private final BigInteger uImmMask;
    private final BigInteger reg1Mask;
    private final BigInteger reg2Mask;
    private final BigInteger reg3Mask;
    private final Map<BigInteger,TrInstruction> instrCache = new HashMap<BigInteger,TrInstruction>();
    private final int numOpBits;
    private final int numRegBits;
    private final int numImmBits;

    // state of the simulator
    final TrCommon common;
    private BigInteger pc = BigInteger.ZERO;    // pc is wordsize, so it can in principle be gigantic
    private BigInteger instr = BigInteger.ZERO;
    private boolean flag = false;
    private final List<BigInteger> regs;        // regs are wordsize
    final List<BigInteger> inTape;
    final List<BigInteger> auxTape;     // auxiliary tape
    private final Map<BigInteger,BigInteger> mem = new HashMap<BigInteger,BigInteger>();
                                        // by definition, memory addresses are wordsize
                                        // this means we can't use List or SparseArray
                                        // because these have int indices, which are too small
    /*
     * It's acceptable to use a List<> for the tapes because they're not random access.
     * We can safely assume that we will not execute any program whose input or aux tape
     * has size >= 2^31, thus a List<> is appropriate.
     *
     * Sadly, since program and data memory are random access, they must be implemented as
     * a mapping from BigInteger address to BigInteger data. Any other way of doing this
     * is necessarily just a reimplementation of Map<>
     */

    public SimState (TrProgram prog, Map<Integer,List<BigInteger>> tapes) {
        // initialize registers
        final int nRegs = (int) Math.pow(2,prog.numRegBits);
        this.regs = new ArrayList<BigInteger>(nRegs);
        for (int i=0;i<nRegs;i++) { regs.add(i,BigInteger.ZERO); }

        // input tapes
        this.inTape = tapes.get(0);
        this.auxTape = tapes.get(1);

        common = new TrCommon ( prog.wordsize, prog.numRegBits, 1 );

        numOpBits = common.numOpBits;
        numRegBits = prog.numRegBits;
        numImmBits = common.nImmBits();
        String mask = TrCommon.rep('1',numOpBits) + TrCommon.rep('0',1+2*numRegBits+numImmBits);
        opMask = new BigInteger(mask,2);
        mask = "1" + mask.substring(numOpBits+1);
        uImmMask = new BigInteger(mask,2);
        mask = TrCommon.rep('1',numRegBits) + mask.substring(1+numRegBits);
        reg1Mask = new BigInteger(mask,2);
        reg2Mask = reg1Mask.shiftRight(numRegBits);
        reg3Mask = TrCommon.TWO.pow(numImmBits).subtract(BigInteger.ONE);
    }

    // state dump
    public TrState getState () {
        return new TrState(pc, mem.get(pc), flag, regs);
    }

    // memory
    // when reading memory, uninitialized space is by definition zero
    public BigInteger getMem ( final BigInteger addr ) {
        final BigInteger data = mem.get(common.canonicalize(addr,false));
        return data == null ? BigInteger.ZERO : data;
    }
    public BigInteger setMem ( final BigInteger addr, final BigInteger data ) {
        final BigInteger oldData = mem.put(common.canonicalize(addr,false),common.canonicalize(data,false));
        return oldData == null ? BigInteger.ZERO : oldData;
    }

    // instruction cache so we're not constantly creating and destroying TrInstruction objects
    public void setMemInstr( final BigInteger addr, final BigInteger data, final TrInstruction instr ) {
        setMem(addr,data);
        instrCache.put(data,instr);
    }

    public TrInstruction instrDecode ( final BigInteger instrPacked ) {
        TrInstruction instr = instrCache.get(instrPacked);
        if (null == instr) {
            instr = instrDecodeH(instrPacked);
            instrCache.put(instrPacked,instr);
        }
        return instr;
    }

    // decode a BigInteger into an instruction
    private TrInstruction instrDecodeH ( BigInteger inst ) {
        final boolean useImm = inst.and(uImmMask).shiftRight(numImmBits+2*numRegBits).equals(BigInteger.ONE);
        final BigInteger imm = useImm ? inst.and(reg3Mask) : BigInteger.ZERO;
        final int r3 = useImm ? 0 : inst.and(reg3Mask).intValue();
        final int r2 = inst.and(reg2Mask).shiftRight(numImmBits).intValue();
        final int r1 = inst.and(reg1Mask).shiftRight(numImmBits+numRegBits).intValue();
        final int op = inst.and(opMask).shiftRight(numImmBits+2*numRegBits+1).intValue();

        return new TrInstruction(op,r1,r2,r3,imm,useImm);
    }

    // program counter
    public BigInteger getPC () { return pc; }
    public BigInteger incPC () { 
        final BigInteger oldPC = pc;
        pc = common.canonicalize(pc.add(BigInteger.ONE),false);
        return oldPC;
    }
    public BigInteger setPC (final BigInteger pc) {
        final BigInteger oldPC = this.pc;
        this.pc = common.canonicalize(pc,false);
        return oldPC;
    }

    // INSTR register
    public BigInteger getInstr () { return instr; }
    public BigInteger setInstr (final BigInteger instr) {
        final BigInteger oldInstr = this.instr;
        this.instr = common.canonicalize(instr,false);
        return oldInstr;
    }

    // flag
    public boolean getFlag () { return flag; }
    public boolean setFlag ( final boolean flag ) {
        final boolean oldFlag = this.flag;
        this.flag = flag;
        return oldFlag;
    }
    public boolean set1Flag () { return setFlag(true); }
    public boolean set0Flag () { return setFlag(false); }

    // registers
    public BigInteger getReg ( final int r ) {
        if (0 == r) {
            return BigInteger.ZERO;
        } else {
            return regs.get(r);
        }
    }

    public BigInteger setReg ( final int r, final BigInteger v ) {
        if (0 == r) {
            return BigInteger.ZERO;
        } else {
            final BigInteger data = regs.get(r);    // if register is unimplemented, don't write
            regs.set(r,common.canonicalize(v,false));
            return data;
        }
    }

    // tapes
    // generic handlers
    private TrCommon.Tuple<Integer,BigInteger> readTape (final int idx, final List<BigInteger> tape) {
        if (tape == null || tape.size() <= idx) {
            return new TrCommon.Tuple<Integer,BigInteger>(idx+1,null);
        } else {
            return new TrCommon.Tuple<Integer,BigInteger>(idx+1,tape.get(idx));
        }
    }
    private BigInteger peekTape (final int idx, final List<BigInteger> tape) {
        if (tape == null || tape.size() <= idx) {
            return null;
        } else {
            return tape.get(idx);
        }
    }

    // tape-specific handlers -- no more tapes!
    /*
    public BigInteger readInTape () {
        final TrCommon.Tuple<Integer,BigInteger> res = readTape(inTapePosn,inTape);
        inTapePosn = res.fst;
        return res.snd;
    }
    public BigInteger peekInTape () { return peekTape(inTapePosn,inTape); }

    public BigInteger readAuxTape () {
        final TrCommon.Tuple<Integer,BigInteger> res = readTape(auxTapePosn,auxTape);
        auxTapePosn = res.fst;
        return res.snd;
    }
    public BigInteger peekAuxTape () { return peekTape(auxTapePosn,auxTape); }
    */
}
