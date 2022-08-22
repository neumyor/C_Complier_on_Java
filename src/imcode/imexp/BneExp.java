package imcode.imexp;

import component.LineContainer;
import global.Config;
import imcode.imitem.VarItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;

import imcode.imitem.IMItem;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

import java.util.Objects;

/**
 *
 */
public class BneExp extends IMExp {
    protected BneExp(IMItem item1, IMItem item2, IMItem item3) {
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
            Integer aValue = (Integer) ((ConstValueEntry) a).getValue();
            Integer bValue = (Integer) ((ConstValueEntry) b).getValue();
            Integer boolValue = (!Objects.equals(aValue, bValue)) ? 1 : 0;
            ret.addLine(String.format("li %s, %d", pool.findNoLoad(toAssign), boolValue));
        } else if (a instanceof ConstValueEntry) {
            String bReg = pool.find(b);;
            Integer aValue = (Integer) ((ConstValueEntry) a).getValue();
            ret.addLine(String.format("sne %s, %s, %d", pool.findNoLoad(toAssign), bReg, aValue));
        } else if (b instanceof ConstValueEntry) {
            String aReg = pool.find(a);
            Integer bValue = (Integer) ((ConstValueEntry) b).getValue();
            ret.addLine(String.format("sne %s, %s, %d", pool.findNoLoad(toAssign), aReg, bValue));
        } else {
            String aReg = pool.find(a);
            String bReg = pool.find(b);
            ret.addLine(String.format("sne %s, %s, %s", pool.findNoLoad(toAssign), aReg, bReg));
        }

        CodeText.textNLine(ret.dump());
    }
}
