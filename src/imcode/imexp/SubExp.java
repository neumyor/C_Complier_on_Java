package imcode.imexp;

import improve.component.regpool.RegPool;
import symbolstruct.FuncRegion;

import component.LineContainer;
import imcode.imitem.IMItem;
import imcode.imitem.IntItem;
import imcode.imitem.VarItem;
import symbolstruct.CodeText;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

public class SubExp extends IMExp {
    protected SubExp(IMItem item1, IMItem item2, IMItem item3) {
        assert item1 instanceof VarItem;
        assert item2 != null;
        assert item3 != null;
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer ret = new LineContainer();

        Entry a = ((VarItem) this.item2).entry;
        Entry b = ((VarItem) this.item3).entry;
        Entry toAssign = ((VarItem) this.item1).entry;

        if (a instanceof ConstValueEntry && b instanceof ConstValueEntry) {
            ret.addLine(String.format("li %s, %d", pool.findNoLoad(toAssign),
                    ((Integer) ((ConstValueEntry) a).getValue()) - ((Integer) ((ConstValueEntry) b).getValue())));
        } else if (a instanceof ConstValueEntry) {
            String bReg = pool.find(b);
            ret.addLine(String.format("subi %s, %s, %d",
                    pool.findNoLoad(toAssign), bReg, ((ConstValueEntry) a).getValue()));
            ret.addLine(String.format("sub %s, $0, %s", pool.findNoLoad(toAssign), pool.findNoLoad(toAssign)));
        } else if (b instanceof ConstValueEntry) {
            String aReg = pool.find(a);
            ret.addLine(String.format("subi %s, %s, %d",
                    pool.findNoLoad(toAssign), aReg, ((ConstValueEntry) b).getValue()));
        } else {
            String aReg = pool.find(a);
            String bReg = pool.find(b);
            ret.addLine(String.format("sub %s, %s, %s", pool.findNoLoad(toAssign), aReg, bReg));
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
        return it + " = " + a + " - " + b;
    }
}
