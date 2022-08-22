package symboltable;

import global.Config;


public class SymbolTable {
    public Block present;
    public Block root;

    public SymbolTable() {
        this.root = null;
        this.present = null;
    }

    public void addOneBlock(BlockType type) {
        Block temp;
        if (type.equals(BlockType.FUNC_BLOCK)) {
            temp = new Block("#unfilledFunc" + Config.getTmpNameSed(), type);
        } else if (type.equals(BlockType.IF_BLOCK)) {
            temp = new Block("#if" + Config.getTmpNameSed(), type);
        } else if (type.equals(BlockType.WHILE_BLOCK)) {
            temp = new Block("#while" + Config.getTmpNameSed(), type);
        } else if (type.equals(BlockType.GLOBAL_BLOCK)) {
            temp = new Block("#global", type);
        } else {
            temp = new Block("#normal" + Config.getTmpNameSed(), type);
        }

        if (type.equals(BlockType.GLOBAL_BLOCK)) {
            temp.level = 0;
            temp.pre = null;
            present = temp;
            if (root == null) {
                root = present;
            }
        } else {
            temp.level = present.level + 1;
            temp.pre = this.present;
            if (present != null) {
                present.nexts.add(temp);
            }
            present = temp;
        }
    }

    public void exitPresentBlock() {
        present = present.pre;
    }

    public Block getPresentBlock() {
        return this.present;
    }

    public void toLog() {
        this.root.toLog();
    }
}
