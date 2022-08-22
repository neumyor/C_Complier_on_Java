package imcode.imexp;

import component.LineContainer;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.Entry;

/**
 * 在调用函数后，从$v0寄存器获取返回值
 */
public class AssignByRetExp extends IMExp {
    protected AssignByRetExp(IMItem var) {
        assert var instanceof VarItem;
        this.item1 = var;
        this.item2 = this.item3 = null;
    }

    @Override
    public void toCode(RegPool pool) {
        Entry toa = ((VarItem) this.item1).entry;
        LineContainer c = new LineContainer();

        c.addLine(String.format("add %s, $v0, $0", pool.findNoLoad(toa)));

        CodeText.textNLine(c.dump());
        return;
    }

    @Override
    public String toString() {
        String ret = ((VarItem) this.item1).entry.name;
        return ret + " = " + "$v0";
    }
}
