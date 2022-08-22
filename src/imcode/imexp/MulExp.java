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

public class MulExp extends IMExp {
    protected MulExp(IMItem item1, IMItem item2, IMItem item3) {
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
                    ((Integer) ((ConstValueEntry) a).getValue()) * ((Integer) ((ConstValueEntry) b).getValue())));
        } else if (a instanceof ConstValueEntry) {
            String bReg = pool.find(b);
            ret.addLine(String.format("mul %s, %s, %d", pool.findNoLoad(toAssign), bReg, ((ConstValueEntry) a).getValue()));
        } else if (b instanceof ConstValueEntry) {
            Integer bValue = (Integer) ((ConstValueEntry) b).getValue();
            Integer powerOfTwo = Config.PowerOfTwo(bValue);
            String aReg = pool.find(a);
            if (bValue == 1) {
                ret.addLine(String.format("add %s, %s, $0", pool.findNoLoad(toAssign), aReg));
            } else if (powerOfTwo != null) {
                ret.addLine(String.format("sll %s, %s, %d", pool.findNoLoad(toAssign), aReg, powerOfTwo));
            } else {
                ret.addLine(String.format("mul %s, %s, %d", pool.findNoLoad(toAssign), aReg, bValue));
            }
        } else {
            String aReg = pool.find(a);
            String bReg = pool.find(b);
            ret.addLine(String.format("mul %s, %s, %s", pool.findNoLoad(toAssign), aReg, bReg));
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
        return it + " = " + a + " * " + b;
    }
}
