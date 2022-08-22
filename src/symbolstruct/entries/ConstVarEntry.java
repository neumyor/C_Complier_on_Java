package symbolstruct.entries;

import component.datatype.Datatype;

public class ConstVarEntry extends VarEntry implements ConstFeature {
    private Integer value;

    public ConstVarEntry(String name, Datatype type, Object value) {
        super(name, type);
        this.value = (Integer) value;
        this.size = 1;
    }

    @Override
    public Object getValue() {
        return this.value;
    }
}
