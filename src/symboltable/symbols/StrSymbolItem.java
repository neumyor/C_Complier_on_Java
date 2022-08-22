package symboltable.symbols;

import symboltable.BasicType;
import symboltable.Block;
import symboltable.SymbolItemType;

public class StrSymbolItem extends SymbolItem {

    private String content;

    public StrSymbolItem(String name, Block block, String content) {
        super(BasicType.STR, SymbolItemType.STR, name, block);
        this.content = content;
    }
}
