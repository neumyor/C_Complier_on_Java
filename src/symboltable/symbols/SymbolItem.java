package symboltable.symbols;

import symboltable.BasicType;
import symboltable.Block;
import symboltable.SymbolItemType;

/**
 * SymbolItem
 * 符号表表项
 */
public abstract class SymbolItem {
    public BasicType dataType;  // 数据类型
    public SymbolItemType itemType;  // 符号项类型
    public String name;     // 符号项名称
    public Block block;     //所处的Block对象

    protected SymbolItem(BasicType dataType, SymbolItemType itemType, String name, Block block) {
        this.dataType = dataType;
        this.itemType = itemType;
        this.name = name;
        this.block = block;
    }

    public String type(){
        return "unknown";
    }
}

