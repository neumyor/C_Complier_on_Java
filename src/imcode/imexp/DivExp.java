package imcode.imexp;

import global.Config;
import improve.component.regpool.RegPool;

import component.LineContainer;
import imcode.imitem.IMItem;
import imcode.imitem.IntItem;
import imcode.imitem.VarItem;
import symbolstruct.CodeText;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

/**
 * #TODO 除法优化
 */
public class DivExp extends IMExp {
    protected DivExp(IMItem item1, IMItem item2, IMItem item3) {
        assert item1 instanceof VarItem;
        assert item2 instanceof VarItem;
        assert item3 instanceof VarItem;
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer ret = new LineContainer();

        Entry toAssign = ((VarItem) this.item1).entry;
        Entry a = ((VarItem) this.item2).entry;
        Entry b = ((VarItem) this.item3).entry;

        if (a instanceof ConstValueEntry && b instanceof ConstValueEntry) {
            ret.addLine(String.format("li %s, %d", pool.findNoLoad(toAssign),
                    ((Integer) ((ConstValueEntry) a).getValue()) / ((Integer) ((ConstValueEntry) b).getValue())));
        } else if (a instanceof ConstValueEntry) {
            String regStr = pool.allocTmpReg();
            ret.addLine(String.format("li %s %d", regStr, ((ConstValueEntry) a).getValue()));
            ret.addLine(String.format("div %s, %s", regStr, pool.find(b)));
            ret.addLine(String.format("mflo %s", pool.findNoLoad(toAssign)));
        } else if (b instanceof ConstValueEntry) {
            Integer bValue = (Integer) ((ConstValueEntry) b).getValue();
            Integer powerOfTwo = Config.PowerOfTwo(bValue);
            if(bValue == 1) {
                String aReg = pool.find(a);
                ret.addLine(String.format("add %s, %s, $0", pool.findNoLoad(toAssign), aReg));
            }
            else if (powerOfTwo != null) {
                String tmpReg = pool.allocTmpReg();
                String aReg = pool.find(a);
                String assignReg = pool.findNoLoad(toAssign);
                String tmpLabel = "templabel" + Config.getTmpNameSed();
                ret.addLine(String.format("add %s, %s, $0", tmpReg, aReg));
                ret.addLine(String.format("sra %s, %s, %d", assignReg, aReg, powerOfTwo));
                ret.addLine(String.format("bgez %s, %s", aReg, tmpLabel));
                ret.addLine(String.format("ori %s, %s, -%d", tmpReg, tmpReg, bValue));
                ret.addLine(String.format("sub %s, %s, -%d", tmpReg, tmpReg, bValue));
                ret.addLine(String.format("beqz %s, %s", tmpReg, tmpLabel));
                ret.addLine(String.format("add %s, %s, 1", assignReg, assignReg));
                ret.addLine(String.format("%s:", tmpLabel));
            } else {
                String regStr = pool.allocTmpReg();
                ret.addLine(String.format("li %s %d", regStr, bValue));
                ret.addLine(String.format("div %s, %s", pool.find(a), regStr));
                ret.addLine(String.format("mflo %s", pool.findNoLoad(toAssign)));
            }
        } else {
            ret.addLine(String.format("div %s, %s", pool.find(a), pool.find(b)));
            ret.addLine(String.format("mflo %s", pool.findNoLoad(toAssign)));
        }

        CodeText.textNLine(ret.dump());
    }

    @Override
    public String toString() {
        String a = null;
        if (this.item2 instanceof IntItem) {
            a = String.valueOf(((IntItem) this.item2).intValue);
        } else if (this.item2 instanceof VarItem) {
            a = ((VarItem) this.item2).entry.name;
        }

        String b = null;
        if (this.item3 instanceof IntItem) {
            b = String.valueOf(((IntItem) this.item3).intValue);
        } else if (this.item3 instanceof VarItem) {
            b = ((VarItem) this.item3).entry.name;
        }
        String it = ((VarItem) this.item1).entry.name;
        return it + " = " + a + " / " + b;
    }
}
