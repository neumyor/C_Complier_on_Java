package imcode.imitem;

import symbolstruct.entries.AbsVarEntry;

import java.util.Objects;

public class VarItem extends IMItem {
    public final AbsVarEntry entry;

    protected VarItem(Object entry) {
        assert entry instanceof AbsVarEntry;
        this.entry = (AbsVarEntry) entry;
    }

    @Override
    public String toString() {
        return entry.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarItem varItem = (VarItem) o;
        return entry.equals(varItem.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry);
    }
}
