package tinyram.ver0;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;

public class TrState extends tinyram.TrState {
    final BigInteger pc;
    final int iTPtr;
    final int aTPtr;
    final boolean flag;
    final List<BigInteger> regs;

    public TrState(BigInteger pc, int iTPtr, int aTPtr, boolean flag, List<BigInteger> regs) {
        this.pc = pc;
        this.iTPtr = iTPtr;
        this.aTPtr = aTPtr;
        this.flag = flag;
        this.regs = new ArrayList<BigInteger>(regs); // make a copy
    }

    public BigInteger getReg ( int regNum ) { return regs.get(regNum); }
    public BigInteger getPC () { return pc; }

    public String toC() {
        final StringBuilder s = new StringBuilder();

        s.append("{");
        s.append(".pc=0x").append(pc.toString(16)).append(",");
        s.append(".iTPtr=0x").append(Integer.toString(iTPtr,16)).append(",");
        s.append(".aTPtr=0x").append(Integer.toString(aTPtr,16)).append(",");
        s.append(".flag=").append(flag ? "1," : "0,");
        s.append(".regs={");
        int i;
        for (i=0;i<regs.size()-1;++i)
            s.append("0x").append(regs.get(i).toString(16)).append(",");
        s.append("0x").append(regs.get(i).toString(16)).append("}}");

        return s.toString();
    }

    public int toProverInput(final StringBuilder s) {
        s.append(" 0x").append(pc.toString(16));
        s.append(" 0x").append(Integer.toString(iTPtr,16));
        s.append(" 0x").append(Integer.toString(aTPtr,16));
        s.append(flag ? " 1" : " 0");

        for (final BigInteger reg : regs)
            s.append(" 0x").append(reg.toString(16));

        return 4 + regs.size();
    }
    
    public String toString() {
        final StringBuilder s = new StringBuilder();

        s.append("0x").append(pc.toString(16)).append(" ");
        for (BigInteger reg : regs)
            s.append("0x").append(reg.toString(16)).append(" ");
        s.append("0x").append(Integer.toString(iTPtr,16)).append(" ");
        s.append("0x").append(Integer.toString(aTPtr,16)).append(" ");
        s.append("0x").append(flag ? "1" : "0");

        return s.toString();
    }
}
