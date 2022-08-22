package imcode.imitem;

import java.util.Objects;

public class StrItem extends IMItem{
    public final String strValue;

    protected StrItem(Object strValue) {
        assert strValue instanceof String;
        this.strValue = (String) strValue;
    }

    @Override
    public String toString() {
        return this.strValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrItem strItem = (StrItem) o;
        return Objects.equals(strValue, strItem.strValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strValue);
    }
}
