package imcode.imexp;

import component.LineContainer;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;

import imcode.imitem.IMItem;
import imcode.imitem.StrItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

/**
 * 打印一个变量操作数或一个字符串操作数
 */
public class PrintVarExp extends IMExp {
    protected PrintVarExp(IMItem item) {
        assert item instanceof VarItem;
        this.item2 = item;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer ret = new LineContainer();
        Entry entry = ((VarItem) this.item2).entry;
        if (entry instanceof ConstValueEntry) {
            ret.addLine(String.format("addi $a0, $0, %d", ((ConstValueEntry) entry).getValue()));
        } else {
            ret.addLine(String.format("add $a0, %s, $0", pool.find(entry)));
        }
        ret.addLine(String.format("li $v0, 1"));
        ret.addLine(String.format("syscall"));
        CodeText.textNLine(ret.dump());
        return;
    }

    @Override
    public String toString() {
        String it = ((VarItem) this.item2).entry.name;
        return "print " + it;
    }
}
