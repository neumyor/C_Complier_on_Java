package component.datatype;

public class IntType implements Datatype {

    public IntType() {
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntType;
    }

    @Override
    public String toString() {
        return "int";
    }

    @Override
    public Integer spaceSize() {
        return 4;
    }
}
