package imcode.imitem;


import component.narray.NPos;

import java.util.Objects;

public class PosItem extends IMItem{
    public final NPos value;
    protected PosItem(Object npos){
        assert npos instanceof NPos;
        this.value = (NPos) npos;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
