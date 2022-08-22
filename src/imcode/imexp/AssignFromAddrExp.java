package imcode.imexp;

import component.LineContainer;
import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;
import symbolstruct.entries.Entry;

public class AssignFromAddrExp extends IMExp{
    protected AssignFromAddrExp(IMItem addr, IMItem value) {
        assert addr instanceof VarItem;
        assert value instanceof VarItem;
        this.item1 = value;
        this.item2 = addr;
    }

    @Override
    public void toCode(RegPool pool) {
        Entry value = ((VarItem) this.item1).entry;
        Entry addr = ((VarItem) this.item2).entry;
        LineContainer c = new LineContainer();

        String addrReg = pool.find(addr);
        c.addLine(String.format("lw %s, (%s)", pool.findNoLoad(value), addrReg));

        CodeText.textNLine(c.dump());
        return ;
    }

    @Override
    public String toString() {
        String value = ((VarItem) this.item1).entry.name;
        String addr = ((VarItem) this.item2).entry.name;
        return String.format("%s <- [%s]", value, addr);
    }
}
