package imcode.imexp;

import component.LineContainer;
import global.Config;
import imcode.imitem.IntItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;

import component.Label;
import imcode.imitem.IMItem;
import imcode.imitem.LabelItem;
import imcode.imitem.VarItem;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

public class ConJumpExp extends IMExp {
    protected ConJumpExp(IMItem label, IMItem conVar) {
        assert label instanceof LabelItem;
        assert conVar instanceof VarItem;
        this.item1 = label;
        this.item2 = conVar;
    }

    @Override
    public void toCode(RegPool pool) {
        Label label = ((LabelItem) this.item1).labelName;

        Entry a = ((VarItem) this.item2).entry;
        if (a instanceof ConstValueEntry) {
            Integer value = (Integer) ((ConstValueEntry) a).getValue();
            if (value == 1) {
                CodeText.textNLine(String.format("j %s", label.labelName));
            }
        } else {
            CodeText.textNLine(String.format("bne $0, %s, %s", pool.find(a), label.labelName));
        }
    }

    @Override
    public String toString() {
        Label label = ((LabelItem) this.item1).labelName;
        String con = ((VarItem) this.item2).entry.name;
        return "conjump " + label.labelName + " by " + con;
    }
}
