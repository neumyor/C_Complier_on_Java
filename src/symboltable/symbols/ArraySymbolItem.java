package symboltable.symbols;

import global.Error;
import symboltable.BasicType;
import symboltable.Block;
import symboltable.SymbolItemType;

import java.util.ArrayList;

public class ArraySymbolItem extends SymbolItem {
    public Boolean isConst;
    private Object value;
    private ArrayList<Integer> size;

    public ArraySymbolItem(Boolean isConst, BasicType dataType, String name, Block block, ArrayList<Integer> size) {
        super(dataType, SymbolItemType.ARRAY, name, block);
        this.size = size;
        this.isConst = isConst;
    }

    public void setConstValue(Object value) {
        if (!isConst) {
            Error.warning("try to set value for an non-const array symbol");
        }
        this.value = value;
    }

    public Integer getArrayLen(int dimension) {
        if (dimension >= size.size()) {
            return 0;       // 0 表示该维度不存在
        } else {
            // size中可能有null值，表示该维度长度不定
            return size.get(dimension);
        }
    }

    public ArrayList<Integer> getSize() {
        return size;
    }

    @Override
    public String type() {
        if (isConst) {
            if (size.size() == 1) {
                return "const int[]";
            } else if (size.size() == 2) {
                return "const int[][]";
            } else {
                Error.warning("wrong size of array item");
                return "unknown";
            }
        } else {
            if (size.size() == 1) {
                return "int[]";
            } else if (size.size() == 2) {
                return "int[][]";
            } else {
                Error.warning("wrong size of array item");
                return "unknown";
            }
        }
    }
}
