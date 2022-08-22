package component.datatype;

import global.Error;

public class VoidType implements Datatype {

    @Override
    public String toString() {
        return "void";
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public Integer spaceSize() {
        Error.warning("you are querying an void type's spaceSize!");
        return 0;
    }
}
