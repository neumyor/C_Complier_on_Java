package symboltable;


import exceptions.DuplicatedDefineException;
import global.Config;
import global.Error;
import global.Logger;
import symboltable.symbols.*;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Block
 * 语法树节点
 * 表示一个作用域
 */
public class Block {
    public String name;     // 作用域名称
    public BlockType type;  // 作用域类型

    public Block pre;       // 父作用域指针
    public ArrayList<Block> nexts;      // 后继作用域指针

    public HashMap<SymbolItemType, ArrayList<SymbolItem>> items;    // 该作用域下所有符号表项
    public HashMap<BlockType, Integer> statementCount;              //  记录该作用域下各种语句的数量
    public SymbolItem funcHead;     // 当前作用域如果是函数，则该变量是一个指向符号表中某函数表项的指针

    public Integer level;           // 嵌套层次，设全局块的嵌套层次为0，依次递增
    public Boolean hasReturn;       // 是否有返回值

    public Block(String name, BlockType type) {
        this.pre = null;
        this.nexts = new ArrayList<>();
        this.items = new HashMap<>();
        this.statementCount = new HashMap<>();
        this.funcHead = null;
        this.name = name;
        this.type = type;
        this.hasReturn = false;
    }

    public void registerStatement(BlockType type) {
        if (!this.statementCount.containsKey(type)) {
            this.statementCount.put(type, 0);
        }
        this.statementCount.put(type, this.statementCount.get(type) + 1);
    }

    public SymbolItem findIdent(String name) {
        SymbolItem var = findVarAndConst(name, true);
        if (var != null) {
            var = findFunc(name, true);
        }
        return var;
    }

    public SymbolItem findIdentCurrent(String name) {
        SymbolItem var = findVarAndConst(name, false);
        if (var == null) {
            var = findFunc(name, false);
        }
        return var;
    }

    public SymbolItem findVarAndConst(String name, Boolean recur) {
        Block tmpBlock = this;

        while (tmpBlock != null) {
            ArrayList<SymbolItem> tmpList = tmpBlock.items.getOrDefault(SymbolItemType.VAR, new ArrayList<>());
            for (SymbolItem item : tmpList) {
                if (item.name.equals(name)) {
                    return item;
                }
            }

            ArrayList<SymbolItem> tmpArrayList = tmpBlock.items.getOrDefault(SymbolItemType.ARRAY, new ArrayList<>());
            for (SymbolItem item : tmpArrayList) {
                if (item.name.equals(name)) {
                    return item;
                }
            }

            tmpBlock = tmpBlock.pre;
            if (!recur) {
                break;
            }
        }
        return null;
    }

    public SymbolItem findFunc(String name, Boolean recur) {
        Block tmpBlock = this;

        while (tmpBlock != null) {
            ArrayList<SymbolItem> tmpList = tmpBlock.items.getOrDefault(SymbolItemType.FUNC, new ArrayList<>());
            for (SymbolItem item : tmpList) {
                if (item.name.equals(name)) {
                    return item;
                }
            }

            tmpBlock = tmpBlock.pre;
            if (!recur) {
                break;
            }
        }
        return null;
    }

    public void insertRecord(SymbolItem record) throws DuplicatedDefineException {
        SymbolItem item = this.findIdentCurrent(record.name);
        if (item == null) {
            if (!this.items.containsKey(record.itemType)) {
                this.items.put(record.itemType, new ArrayList<>());
            }
            record.block = this;
            this.items.get(record.itemType).add(record);
        } else {
            throw new DuplicatedDefineException();
        }
    }

    public void setFuncHead(SymbolItem funcHead) {
        if (this.funcHead != null) {
            Error.warning("You are changing the func head of an existing Block\'s func head!");
        }
        if (!type.equals(BlockType.FUNC_BLOCK)) {
            Error.warning("You are setting a func head of an none-func block!");
        }
        this.funcHead = funcHead;
        this.name = funcHead.name;
    }

    public SymbolItem createFunc(BasicType returnType, String name) throws DuplicatedDefineException {
        SymbolItem func = new FuncSymbolItem(returnType, name, this);
        this.insertRecord(func);
        return func;
    }

    public SymbolItem createConst(BasicType basicType, String name, Integer initVal) throws DuplicatedDefineException {
        SymbolItem const_ = new VarSymbolItem(true, basicType, name, this);
        ((VarSymbolItem) const_).setConstValue(initVal);
        this.insertRecord(const_);
        return const_;
    }

    public SymbolItem createVar(BasicType basicType, String name) throws DuplicatedDefineException {
        SymbolItem var = new VarSymbolItem(false, basicType, name, this);
        this.insertRecord(var);
        return var;
    }

    public SymbolItem createTemp(BasicType basicType, Boolean isConst) {
        SymbolItem temp = new VarSymbolItem(isConst, basicType, allocTempName(basicType.name()), this);
        try {
            this.insertRecord(temp);
        } catch (DuplicatedDefineException ignored) {
            ;
        }
        return temp;
    }

    public SymbolItem createConstArray(BasicType basicType, String name, ArrayList<Integer> size, Object initVal) throws DuplicatedDefineException {
        ArraySymbolItem constArray = new ArraySymbolItem(true, basicType, name, this, size);
        constArray.setConstValue(initVal);
        this.insertRecord(constArray);
        return constArray;
    }

    public SymbolItem createVarArray(BasicType basicType, String name, ArrayList<Integer> size) throws DuplicatedDefineException {
        ArraySymbolItem array = new ArraySymbolItem(false, basicType, name, this, size);
        this.insertRecord(array);
        return array;
    }

    public SymbolItem createTempArray(BasicType basicType, ArrayList<Integer> size, Boolean isConst) {
        ArraySymbolItem array = new ArraySymbolItem(isConst, basicType, allocTempName(basicType.name()), this, size);
        try {
            this.insertRecord(array);
        } catch (DuplicatedDefineException ignored) {
            ;
        }
        return array;
    }

    public SymbolItem createStr(String name, String str) throws DuplicatedDefineException {
        SymbolItem var = new StrSymbolItem(allocTempName("str"), this, str);
        this.insertRecord(var);
        return var;
    }

    public SymbolItem createUnknown() {
        SymbolItem unk = new SymbolItem(BasicType.UNK, SymbolItemType.unknown, allocTempName("unknown"), this) {
            @Override
            public String type() {
                return "unknown";
            }
        };
        try {
            this.insertRecord(unk);
        } catch (DuplicatedDefineException ignored) {
            ;
        }
        return unk;
    }

    public SymbolItem createUnknownFunc() {
        SymbolItem unk = new FuncSymbolItem(BasicType.UNK, allocTempName("unknown"), this);
        try {
            this.insertRecord(unk);
        } catch (DuplicatedDefineException ignored) {
            ;
        }
        return unk;
    }

    public String allocTempName(String type) {
        return "#" + type + Config.getTmpNameSed();
    }

    public void toLog() {
        for (SymbolItemType key : this.items.keySet()) {
            ArrayList<SymbolItem> items = this.items.get(key);
            for (SymbolItem item : items) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < level; i++) {
                    line.append("\t");
                }
                line.append(item.name + " " + item.type());
                Logger.GetLogger().SymbolLog(line.toString());
            }
        }
        for (Block block : this.nexts) {
            block.toLog();
        }
    }

    /**
     * 找到当前块之前的最后一个函数块
     * 用于判断return语句的正确性
     * 因为会Block会嵌套很多层
     *
     * @return 最近一个父亲函数块Func_Block的返回类型
     */
    public Block getLastFunc() {
        Block tmpBlock = this;

        while (tmpBlock != null && tmpBlock.funcHead == null) {
            tmpBlock = tmpBlock.pre;
        }
        if (tmpBlock == null) {
            return null;
        } else {
            return tmpBlock;
        }
    }

    public Boolean isInWhileBlock() {
        Block tmpBlock = this;

        while (tmpBlock != null && tmpBlock.type != BlockType.WHILE_BLOCK) {
            tmpBlock = tmpBlock.pre;
        }
        if (tmpBlock == null) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean isInIfBlock() {
        Block tmpBlock = this;

        while (tmpBlock != null && tmpBlock.type != BlockType.WHILE_BLOCK) {
            tmpBlock = tmpBlock.pre;
        }
        if (tmpBlock == null) {
            return false;
        } else {
            return true;
        }
    }
}
