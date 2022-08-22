package imcode.imitem;

import java.util.Objects;

public class IntItem extends IMItem{
    public final Integer intValue;

    protected IntItem(Object intValue) {
        assert intValue instanceof Integer;
        this.intValue = (Integer) intValue;
    }

    @Override
    public String toString() {
        return String.valueOf(this.intValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntItem intItem = (IntItem) o;
        return Objects.equals(intValue, intItem.intValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intValue);
    }
}
