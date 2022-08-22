package symbolstruct.entries;

import component.datatype.Datatype;
import symbolstruct.FuncRegion;
import symbolstruct.Scope;
import symboltable.Block;

import java.util.Objects;

public abstract class Entry {
    public String name;
    public Datatype datatype;
    public Scope in;
    public Boolean isParam;

    protected Entry(String name, Datatype datatype) {
        this.name = name;
        this.datatype = datatype;
        this.isParam = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return name.equals(entry.name) && Objects.equals(in, entry.in);
    }

    @Override
    public int hashCode() {
        int code = Objects.hash(name) + Objects.hash(in);
        return code;
    }

    public Boolean isGlobal() {
        return this.in.getLevel() == 0;
    }
}
