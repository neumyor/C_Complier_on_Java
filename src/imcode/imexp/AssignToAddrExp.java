package imcode.imexp;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

public class AssignToAddrExp extends IMExp {
    protected AssignToAddrExp(IMItem addr, IMItem value) {
        assert addr instanceof VarItem;
        assert value instanceof VarItem;
        this.item2 = addr;
        this.item3 = value;
    }

    @Override
    public void toCode(RegPool pool) {
        Entry addr = ((VarItem) this.item2).entry;
        Entry value = ((VarItem) this.item3).entry;

        if (value instanceof ConstValueEntry) {
            String regStr = pool.allocTmpReg();
            CodeText.textNLine(String.format("li %s, %d", regStr, ((ConstValueEntry) value).getValue()));
            CodeText.textNLine(String.format("sw %s, (%s)", regStr, pool.find(addr)));
            pool.freeTmpReg(regStr);
        } else {
            CodeText.textNLine(String.format("sw %s, (%s)", pool.find(value), pool.find(addr)));
        }
    }

    @Override
    public String toString() {
        String addr = ((VarItem) this.item2).entry.name;
        String value = ((VarItem) this.item3).entry.name;
        return String.format("[%s] <- %s", addr, value);
    }
}
