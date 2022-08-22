package imcode.imexp;

import component.LineContainer;
import imcode.imitem.SpaceItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.Entry;

/**
 * 将变量的地址赋值给另一个变量
 * 要求item1、2都是变量
 */
public class AssignByVarAddrExp extends IMExp {
    protected AssignByVarAddrExp(IMItem array, IMItem offset) {
        assert array instanceof VarItem;
        assert offset instanceof SpaceItem;
        this.item1 = array;
        this.item2 = offset;
        this.item3 = null;
    }

    @Override
    public String toString() {
        String itm1 = ((VarItem) this.item1).entry.name;
        String itm2 = ((SpaceItem) this.item2).entry.name;
        return itm1 + " = " + "addr<" + itm2 + ">";
    }

    @Override
    public void toCode(RegPool pool) {
        Entry itm1 = ((VarItem) this.item1).entry;
        Entry itm2 = ((SpaceItem) this.item2).entry;
        LineContainer c = new LineContainer();

        if (itm2.isGlobal()) {
            c.addLine(String.format("la %s, %s", pool.findNoLoad(itm1), itm2.name));
        } else {
            c.addLine(String.format("addi %s, $sp, %d", pool.findNoLoad(itm1), pool.getFrame().offsetMap.get(itm2)));
        }

        CodeText.textNLine(c.dump());
        return;
    }
}
