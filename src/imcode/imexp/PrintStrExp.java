package imcode.imexp;

import component.LineContainer;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;

import imcode.imitem.IMItem;
import imcode.imitem.StrItem;

public class PrintStrExp extends IMExp {
    protected PrintStrExp(IMItem strItem) {
        assert strItem instanceof StrItem;
        this.item2 = strItem;
    }

    @Override
    public void toCode(RegPool pool) {
        LineContainer c = new LineContainer();
        String marker = ((StrItem) this.item2).strValue;
        c.addLine(String.format("la $a0, %s", marker));
        c.addLine(String.format("li $v0, 4"));
        c.addLine(String.format("syscall"));
        CodeText.textNLine(c.dump());
        return;
    }

    @Override
    public String toString() {
        String top = ((StrItem) this.item2).strValue;
        return "print \"" + top + "\"";
    }
}
