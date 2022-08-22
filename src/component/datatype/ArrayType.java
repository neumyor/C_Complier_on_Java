package component.datatype;

import global.Error;

import java.util.ArrayList;
import java.util.Objects;

public class ArrayType implements Datatype {
    public Datatype basicType;
    public ArrayList<Integer> dimension;

    public ArrayType(Datatype basicType, ArrayList<Integer> dimensions) {
        this.basicType = basicType;
        this.dimension = dimensions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(basicType, arrayType.basicType) &&
                Objects.equals(dimension.size(), arrayType.dimension.size());
    }

    @Override
    public Integer spaceSize() {
        return flat() * this.basicType.spaceSize();
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        for (Integer len : dimension) {
            temp.append("[" + len + "]");
        }
        return this.basicType + temp.toString();
    }

    public Integer flat() {
        int length = 1;
        for (Integer len : this.dimension) {
            if (len == null) Error.warning("flat should not be used when dimensions are in-complete");
            length *= len;
        }
        return length;
    }

    public Datatype getChildType() {
        ArrayList<Integer> tmp = new ArrayList<>();
        for (int i = 1; i < this.dimension.size(); i++) {
            tmp.add(this.dimension.get(i));
        }
        if(tmp.size() == 0){
            return this.basicType;
        }
        return new ArrayType(this.basicType, tmp);
    }
}
