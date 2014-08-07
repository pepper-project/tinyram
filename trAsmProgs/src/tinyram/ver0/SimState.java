package tinyram.ver0;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import tinyram.TrCommon;

public class SimState {
    // state of the simulator
    final TrCommon common;
    private BigInteger pc = BigInteger.ZERO;    // pc is wordsize, so it can in principle be gigantic
    private boolean flag = false;
    private final List<BigInteger> regs;        // regs are wordsize
    private final List<BigInteger> inTape;      // input tape
    private int inTapePosn = 0;                 // tape position
    private final List<BigInteger> auxTape;     // auxiliary tape
    private int auxTapePosn = 0;                // tape position
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

    public SimState (int wordsize, int numRegBits, Map<Integer,List<BigInteger>> tapes) {
        // initialize registers
        this.regs = new ArrayList<BigInteger>((int) Math.pow(2,numRegBits));
        for (int i = 0 ; i < (int) Math.pow(2,numRegBits) ; i++) { regs.add(i,BigInteger.ZERO); }

        // input tapes
        this.inTape = tapes.get(0);
        this.auxTape = tapes.get(1);

        common = new TrCommon ( wordsize, numRegBits, 0 );
    }

    // state dump
    public TrState getState () {
        return new TrState(pc, inTapePosn, auxTapePosn, flag, regs);
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
    public BigInteger getReg ( final int r ) { return regs.get(r); }
    public BigInteger setReg ( final int r, final BigInteger v ) {
        return regs.set(r,common.canonicalize(v,false));
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

    // tape-specific handlers
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
}
