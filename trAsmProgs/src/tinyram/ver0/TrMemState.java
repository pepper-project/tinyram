package tinyram.ver0;

import java.math.BigInteger;

import tinyram.TrCommon;
import tinyram.TrInstruction;

public class TrMemState extends tinyram.TrMemState{

    // two subsequent states are used to generate a memory transcript
    // we need the next state only in the case that the instruction is a load
    public TrMemState(tinyram.TrState st, tinyram.TrState stNext, int stepNum, TrInstruction instr, int version) {
        this.stepNum = stepNum;

        switch ((TrCommon.getOpcodes(version))[instr.op]) {
            case "load" :
                this.isMem = true;
                this.isLoad = true;
                this.addr = instr.useImm ? instr.imm : st.getReg(instr.r3);
                if (null != stNext)
                    this.data = stNext.getReg(instr.r1);
                else
                    throw new IllegalArgumentException("Need a valid stNext after load instruction when creating a memory transcript.");
                break;
            case "store":
                this.isMem = true;
                this.isLoad = false;
                this.addr = instr.useImm ? instr.imm : st.getReg(instr.r3);
                this.data = st.getReg(instr.r1);
                break;
            default     :
                this.isMem = false;
                this.isLoad = false;
                this.addr = BigInteger.ZERO;
                this.data = BigInteger.ZERO;
        }
    }

    // constructor for use by copy() method
    private TrMemState(boolean isMem, boolean isLoad, BigInteger addr, int stepNum, BigInteger data) {
        this.isMem = isMem;
        this.isLoad = isLoad;
        this.addr = addr;
        this.stepNum = stepNum;
        this.data = data;
    }

    public TrMemState copy ( int stepNum ) {
        return new TrMemState(isMem,isLoad,addr,stepNum,data);
    }

    // constructor for creating reads/writes to memory for instructions
    public TrMemState(BigInteger addr, BigInteger data, int stepNum, boolean isLoad) {
      	this.isMem = true;
      	this.isLoad = isLoad;
      	this.addr = addr;
      	this.stepNum = stepNum;
      	this.data = data;
    }
}

