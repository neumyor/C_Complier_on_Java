package symbolstruct.entries;

import component.datatype.IntType;

import java.util.Objects;

public class ConstValueEntry extends VarEntry implements ConstFeature {
    Integer value;

    public ConstValueEntry(Integer value) {
        super(String.valueOf(value), new IntType());
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstValueEntry that = (ConstValueEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass()) + Objects.hashCode(value);
    }
}
