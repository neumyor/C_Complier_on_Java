package imcode.imexp;

import component.LineContainer;
import imcode.imitem.IntItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

/**
 * 将参数押入函数的参数栈
 * 通常在CallExp前使用，
 * 用于准备函数所需的参数
 */
public class PushParaExp extends IMExp {
    protected PushParaExp(IMItem item, IMItem counter) {
        assert item instanceof VarItem;
        assert counter instanceof IntItem;
        this.item2 = item;
        this.item3 = counter;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer c = new LineContainer();

        Entry var = ((VarItem) this.item2).entry;
        Integer off = ((IntItem) this.item3).intValue;

        if (var instanceof ConstValueEntry) {
            String regStr = pool.allocTmpReg();
            c.addLine(String.format("li %s, %d", regStr, ((ConstValueEntry) var).getValue()));
            c.addLine(String.format("sw %s, -%d($sp)", regStr, off * 4));
        } else {
            c.addLine(String.format("sw %s, -%d($sp)", pool.find(var), off * 4));
        }

        CodeText.textNLine(c.dump());
    }

    @Override
    public String toString() {
        String it = ((VarItem) this.item2).entry.name;
        return "push " + it;
    }
}
