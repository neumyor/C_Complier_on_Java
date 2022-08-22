package imcode.imexp;

import component.LineContainer;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.Entry;

/**
 * 将getint()的值赋值给一个变量
 * item1必须是变量类型
 */
public class AssignByGetInt extends IMExp {
    protected AssignByGetInt(IMItem toAssign) {
        assert toAssign instanceof VarItem;
        this.item1 = toAssign;
        this.item2 = this.item3 = null;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer c = new LineContainer();

        Entry toAssign = ((VarItem) this.item1).entry;
        c.addLine("li $v0, 5");
        c.addLine("syscall");
        c.addLine(String.format("add %s, $v0, $0", pool.findNoLoad(toAssign)));


        CodeText.textNLine(c.dump());
        return;
    }

    @Override
    public String toString() {
        String toa = ((VarItem) this.item1).entry.name;
        return toa + " = getint()";
    }
}
