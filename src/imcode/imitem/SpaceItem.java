package imcode.imitem;

import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.SpaceEntry;

import java.util.Objects;

public class SpaceItem extends IMItem {
    public final SpaceEntry entry;

    protected SpaceItem(Object entry) {
        assert entry instanceof AbsVarEntry;
        this.entry = (SpaceEntry) entry;
    }

    @Override
    public String toString() {
        return "SPACE[" + entry.name + "]";
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
