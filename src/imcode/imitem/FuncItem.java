package imcode.imitem;

import component.datatype.Datatype;
import symbolstruct.entries.FuncEntry;

import java.util.ArrayList;
import java.util.Objects;

public class FuncItem extends IMItem {
    public final FuncEntry entry;

    protected FuncItem(Object entry) {
        assert entry instanceof FuncEntry;
        this.entry = (FuncEntry) entry;
    }

    @Override
    public String toString() {
        return this.entry.name + "()";
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry);
    }
}
