package imcode.imexp;

import imcode.imitem.IMItem;
import imcode.imitem.VarItem;
import improve.component.regpool.RegPool;
import symbolstruct.FuncRegion;

public class ParaDefExp extends IMExp {
    public ParaDefExp(IMItem var) {
        assert var instanceof VarItem;
        this.item1 = var;
    }

    @Override
    public void toCode(RegPool pool) {
        return;
    }

    @Override
    public String toString() {
        return "import " + ((VarItem) this.item1).entry.name;
    }
}
