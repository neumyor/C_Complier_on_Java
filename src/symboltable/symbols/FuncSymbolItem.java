package symboltable.symbols;

import symboltable.BasicType;
import symboltable.Block;
import symboltable.SymbolItemType;

import java.util.ArrayList;
import java.util.Locale;

public class FuncSymbolItem extends SymbolItem {
    public ArrayList<SymbolItem> paramsList;

    public FuncSymbolItem(BasicType basicType, String name, Block block) {
        super(basicType, SymbolItemType.FUNC, name, block);
    }

    public void setParamsList(ArrayList<SymbolItem> paramsList) {
        this.paramsList = paramsList;
    }

    @Override
    public String type() {
        return this.dataType.toString().toLowerCase(Locale.ROOT) + " func";
    }

    @Override
    public String toString() {
        return type() + "\t" + name;
    }
}
