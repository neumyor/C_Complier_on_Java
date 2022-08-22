package imcode.imitem;

import component.Label;

import java.util.Objects;

public class LabelItem extends IMItem {
    public final Label labelName;

    protected LabelItem(Object name) {
        assert name instanceof Label;
        this.labelName = (Label) name;
    }

    @Override
    public String toString() {
        return "#label-"+this.labelName+":";
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelName);
    }
}
