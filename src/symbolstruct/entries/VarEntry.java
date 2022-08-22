package symbolstruct.entries;

import component.datatype.Datatype;
import component.narray.NArrayItem;

public class VarEntry extends AbsVarEntry implements NArrayItem {

    public VarEntry(String name, Datatype datatype) {
        super(name, datatype);
        this.size = 1;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.datatype + " " + this.name;
    }

    @Override
    public Object instance() {
        return this;
    }
}
