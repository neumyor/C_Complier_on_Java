package symbolstruct.entries;

import component.datatype.Datatype;

import java.util.ArrayList;

public class FuncEntry extends Entry {
    public ArrayList<AbsVarEntry> params;

    public FuncEntry(String name, Datatype datatype, ArrayList<AbsVarEntry> params) {
        super(name, datatype);
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        for (Entry param : params) {
            temp.append(param.datatype.toString() + " ");
        }
        return this.datatype + " " + this.getClass().getSimpleName() + " (" + ")";
    }
}
