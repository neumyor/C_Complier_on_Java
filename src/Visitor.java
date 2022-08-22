import ast.BranchNode;
import ast.LeafNode;
import ast.Node;
import component.GUNIT;
import component.SYMBOL;
import exceptions.DuplicatedDefineException;
import global.Logger;
import global.Error;
import symboltable.*;
import symboltable.symbols.ArraySymbolItem;
import symboltable.symbols.FuncSymbolItem;
import symboltable.symbols.SymbolItem;
import symboltable.symbols.VarSymbolItem;

import java.util.ArrayList;

/**
 * Visitor   遍历者
 * 遍历 AST 进行语义检查 和 生成中间代码
 * 同时 填充符号表
 * 注意，AST树的节点是完整的，但是部分节点由于缺乏vipNode，是incorrect的
 * Visitor需要同时找出这些语法上的问题
 */
public class Visitor {
    private SymbolTable symbolTable;
    private Node root;

    public Visitor(Node root) {
        this.symbolTable = new SymbolTable();
        this.root = root;
    }

    private void logU4Error(String message) {
        Logger.GetLogger().ErrorLog(message);
    }

    /**
     * 检查分支节点n是否是特定的非终结符类型
     *
     * @param n    分支节点
     * @param type 非终结符类型
     * @return 是否
     */
    private Boolean ckBrN(Node n, GUNIT type) {
        return (n instanceof BranchNode &&
                ((BranchNode) n).getGUnit().equals(type));
    }

    private Boolean ckLfN(Node n, SYMBOL type) {
        return (n instanceof LeafNode &&
                ((LeafNode) n).getType().equals(type));
    }

    private Node export(ArrayList<Node> children, int i) {
        try {
            return children.get(i);
        } catch (IndexOutOfBoundsException e) {
            return new LeafNode(SYMBOL.UNKNOWN, null, false);
        }
    }

    public void visit() {
        CompUnit(root);
        symbolTable.toLog();
    }

    private void CompUnit(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        symbolTable.addOneBlock(BlockType.GLOBAL_BLOCK);
        int i = 0;
        while (ckBrN(export(cs, i), GUNIT.Decl)) {
            Decl(export(cs, i++));
        }

        while (ckBrN(export(cs, i), GUNIT.FuncDef)) {
            FuncDef(export(cs, i++));
        }

        MainFuncDef(export(cs, i));
        symbolTable.exitPresentBlock();
    }

    private void Decl(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.ConstDecl)) {
            ConstDecl(export(cs, i));
        } else {
            VarDecl(export(cs, i));
        }
    }

    private void ConstDecl(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        i++; // const
        BasicType type = BType(export(cs, i++));
        ConstDef(export(cs, i++), type);
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ConstDef(export(cs, i++), type);
        }
    }

    private BasicType BType(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        return BasicType.INT;
    }

    private void ConstDef(Node syn, BasicType type) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();

        ArrayList<Integer> dimensions = new ArrayList<>();
        while (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 2), SYMBOL.RBRACK)) {
            Integer len = ConstExp(export(cs, i + 1));
            dimensions.add(len);
            i += 3;
        }

        i++; // =

        Object initVal = ConstInitVal(export(cs, i));

        if (dimensions.size() == 0) {
            SymbolItem funcHead;
            try {
                symbolTable.getPresentBlock().createConst(type, name, (Integer) initVal);
            } catch (DuplicatedDefineException e) {
                symbolTable.getPresentBlock().createUnknown();
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
            }

        } else {
            try {
                symbolTable.getPresentBlock().createConstArray(type, name, dimensions, initVal);
            } catch (DuplicatedDefineException e) {
                symbolTable.getPresentBlock().createUnknown();
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
            }
        }


    }

    private Object ConstInitVal(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;


        if (ckLfN(export(cs, i), SYMBOL.LBRACE)) {
            i++;
            ArrayList<Object> ret = new ArrayList<>();
            if (!ckLfN(export(cs, i), SYMBOL.RBRACE)) {
                ret.add(ConstInitVal(export(cs, i)));
                i++;
                while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                    i++;
                    ret.add(ConstInitVal(export(cs, i)));
                    i++;
                }
            }
            return ret;
        } else {
            return ConstExp(export(cs, i));
        }
    }

    private void VarDecl(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        BasicType type = BType(export(cs, i++));
        VarDef(export(cs, i++), type);
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            VarDef(export(cs, i++), type);
        }
    }

    private void VarDef(Node syn, BasicType type) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();

        ArrayList<Integer> dimensions = new ArrayList<>();
        while (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 2), SYMBOL.RBRACK)) {
            Integer len = ConstExp(export(cs, i + 1));
            dimensions.add(len);
            i += 3;
        }

        if (ckLfN(export(cs, i), SYMBOL.ASSIGN)) {
            i++; // =
            InitVal(export(cs, i));
        }

        if (dimensions.size() == 0) {
            SymbolItem funcHead;
            try {
                symbolTable.getPresentBlock().createVar(type, name);
            } catch (DuplicatedDefineException e) {
                symbolTable.getPresentBlock().createUnknown();
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
            }

        } else {
            try {
                symbolTable.getPresentBlock().createVarArray(type, name, dimensions);
            } catch (DuplicatedDefineException e) {
                symbolTable.getPresentBlock().createUnknown();
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
            }
        }
    }

    private void InitVal(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.LBRACE)) {
            i++;
            if (!ckLfN(export(cs, i), SYMBOL.RBRACE)) {
                InitVal(export(cs, i));
                i++;
                while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                    i++;
                    InitVal(export(cs, i));
                    i++;
                }
            }
        } else {
            Exp(export(cs, i));
        }
    }

    private void FuncDef(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        BasicType retType = FuncType(export(cs, i++));
        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();
        i++;    // (

        SymbolItem funcHead;
        try {
            funcHead = symbolTable.getPresentBlock().createFunc(retType, name);
        } catch (DuplicatedDefineException e) {
            funcHead = symbolTable.getPresentBlock().createUnknownFunc();
            logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
        }


        symbolTable.addOneBlock(BlockType.FUNC_BLOCK);

        ArrayList<SymbolItem> fParams = new ArrayList<>();
        if (ckBrN(export(cs, i), GUNIT.FuncFParams)) {
            fParams = FuncFParams(export(cs, i++));
        }

        ((FuncSymbolItem) funcHead).setParamsList(fParams);

        i++;    // )
        symbolTable.getPresentBlock().setFuncHead(funcHead);
        Block(export(cs, i));

        int lineAtLastBrace = ((LeafNode) export(cs, i).getChildren().get(export(cs, i).getChildren().size() - 1)).getToken().getLine();

//        if (!symbolTable.getPresentBlock().hasReturn &&
//                symbolTable.getPresentBlock().funcHead.dataType.equals(BasicType.INT)) {
//            logU4Error(Error.intFuncButNoReturnError(lineAtLastBrace));
//        }

        if (symbolTable.getPresentBlock().funcHead.dataType.equals(BasicType.INT)) {
            boolean lastElementReturnExp = false;
            BranchNode block = (BranchNode) export(cs, i);
            ArrayList<Node> blockChlidren = block.getChildren();
            if(blockChlidren.size() > 2) {
                Node lastBlockItem = blockChlidren.get(blockChlidren.size() - 2);
                Node stmtNode = lastBlockItem.getChildren().get(0);
                ArrayList<Node> stmtNodeChildren = stmtNode.getChildren();
                if (ckBrN(stmtNode, GUNIT.Stmt) && ckLfN(stmtNodeChildren.get(0), SYMBOL.RETURNTK)){
                    int curP = 0;
                    curP++;    // return
                    if (ckBrN(stmtNodeChildren.get(curP), GUNIT.Exp)) {
                        lastElementReturnExp = true;
                    }
                }
            }
            if (!lastElementReturnExp) {
                logU4Error(Error.intFuncButNoReturnError(lineAtLastBrace));
            }
        }

        symbolTable.exitPresentBlock();
    }

    private void MainFuncDef(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        BasicType retType = BasicType.INT;
        String name = "main";
        i += 3;    // int main (

        SymbolItem funcHead;
        try {
            funcHead = symbolTable.getPresentBlock().createFunc(retType, name);
        } catch (DuplicatedDefineException e) {
            funcHead = symbolTable.getPresentBlock().createUnknownFunc();
            logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, i++)).getToken().getLine()));
        }

        symbolTable.addOneBlock(BlockType.FUNC_BLOCK);
        ((FuncSymbolItem) funcHead).setParamsList(new ArrayList<>());

        i++;    // )
        symbolTable.getPresentBlock().setFuncHead(funcHead);
        Block(export(cs, i));

        int lineAtLastBrace = ((LeafNode) export(cs, i).getChildren().get(export(cs, i).getChildren().size() - 1)).getToken().getLine();
//        if (!symbolTable.getPresentBlock().hasReturn &&
//                symbolTable.getPresentBlock().funcHead.dataType.equals(BasicType.INT)) {
//            logU4Error(Error.intFuncButNoReturnError(lineAtLastBrace));
//        }
        // 上面这个是有返回值内部只要有return就认为有返回值
        // 有返回值函数中的Block的最后一个BlockItem中必须是Stmt而且有返回Exp
        if (symbolTable.getPresentBlock().funcHead.dataType.equals(BasicType.INT)) {
            boolean lastElementReturnExp = false;
            BranchNode block = (BranchNode) export(cs, i);
            ArrayList<Node> blockChlidren = block.getChildren();
            if(blockChlidren.size() > 2) {
                Node lastBlockItem = blockChlidren.get(blockChlidren.size() - 2);
                Node stmtNode = lastBlockItem.getChildren().get(0);
                ArrayList<Node> stmtNodeChildren = stmtNode.getChildren();
                if (ckBrN(stmtNode, GUNIT.Stmt) && ckLfN(stmtNodeChildren.get(0), SYMBOL.RETURNTK)){
                    int curP = 0;
                    curP++;    // return
                    if (ckBrN(stmtNodeChildren.get(curP), GUNIT.Exp)) {
                        lastElementReturnExp = true;
                    }
                }
            }
            if (!lastElementReturnExp) {
                logU4Error(Error.intFuncButNoReturnError(lineAtLastBrace));
            }
        }

        symbolTable.exitPresentBlock();
    }

    private BasicType FuncType(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (((LeafNode) export(cs, i)).getToken().getSymbol().equals(SYMBOL.VOIDTK)) {
            return BasicType.VOID;
        } else {
            return BasicType.INT;
        }
    }

    private ArrayList<SymbolItem> FuncFParams(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        ArrayList<SymbolItem> ret = new ArrayList<>();
        ret.add(FuncFParam(export(cs, i++)));
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ret.add(FuncFParam(export(cs, i++)));
        }
        return ret;
    }

    private SymbolItem FuncFParam(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        BasicType type = BType(export(cs, i++));
        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();

        ArrayList<Integer> dimensions = new ArrayList<>();

        if (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 1), SYMBOL.RBRACK)) {
            i += 2;
            dimensions.add(null);

            while (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 2), SYMBOL.RBRACK)) {
                Integer len = ConstExp(export(cs, i + 1));
                dimensions.add(len);
                i += 3;
            }
        }

        if (dimensions.size() == 0) {
            SymbolItem funcHead;
            try {
                return symbolTable.getPresentBlock().createVar(type, name);
            } catch (DuplicatedDefineException e) {
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
                return symbolTable.getPresentBlock().createUnknown();
            }
        } else {
            try {
                return symbolTable.getPresentBlock().createVarArray(type, name, dimensions);
            } catch (DuplicatedDefineException e) {
                logU4Error(Error.duplicatedDefinitionError(((LeafNode) export(cs, mark)).getToken().getLine()));
                return symbolTable.getPresentBlock().createUnknown();
            }
        }
    }

    private void Block(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        i++;    // {

        while (i < cs.size() - 1) {
            BlockItem(export(cs, i++));
        }
    }

    private void BlockItem(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckBrN(export(cs, i), GUNIT.Decl)) {
            Decl(export(cs, i));
        } else {
            Stmt(export(cs, i));
        }
    }

    private void Stmt(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.IFTK)) {
            symbolTable.addOneBlock(BlockType.IF_BLOCK);
            i += 2; // if and (
            Cond(export(cs, i++));


            i++;  // )
            Stmt(export(cs, i++));
            symbolTable.exitPresentBlock();
            if (ckLfN(export(cs, i), SYMBOL.ELSETK)) {
                i++;
                symbolTable.addOneBlock(BlockType.IF_BLOCK);
                Stmt(export(cs, i));
                symbolTable.exitPresentBlock();
            }
        } else if (ckLfN(export(cs, i), SYMBOL.WHILETK)) {
            symbolTable.addOneBlock(BlockType.WHILE_BLOCK);
            i += 2; // while and (
            Cond(export(cs, i++));


            i++;  // )
            Stmt(export(cs, i));
            symbolTable.exitPresentBlock();
        } else if (ckLfN(export(cs, i), SYMBOL.BREAKTK)) {
            i++;    // break
            if (!symbolTable.getPresentBlock().isInWhileBlock()) {
                logU4Error(Error.breakContinueError(((LeafNode) export(cs, i)).getToken().getLine()));
            }
        } else if (ckLfN(export(cs, i), SYMBOL.CONTINUETK)) {
            i++;    // continue
            if (!symbolTable.getPresentBlock().isInWhileBlock()) {
                logU4Error(Error.breakContinueError(((LeafNode) export(cs, i)).getToken().getLine()));
            }
        } else if (ckLfN(export(cs, i), SYMBOL.RETURNTK)) {
            int mark = i;

            i++;    // return

            Block lastFunc = symbolTable.getPresentBlock().getLastFunc();

            if (ckBrN(export(cs, i), GUNIT.Exp)) {
                lastFunc.hasReturn = true;
                if (lastFunc != null && lastFunc.funcHead.dataType.equals(BasicType.VOID)) {
                    logU4Error(Error.voidFuncButReturnError(((LeafNode) export(cs, mark)).getToken().getLine()));
                }
                i++;
            } else {
                lastFunc.hasReturn = false;
            }
        } else if (ckLfN(export(cs, i), SYMBOL.PRINTFTK)) {
            i += 2;   //printf and (

            char[] strArray = ((LeafNode) export(cs, i)).getToken().getValue().toCharArray();
            int size = strArray.length;

            for (int iter = 1; iter < size - 1; iter++) {   // 去除头尾的引号
                if (!((strArray[iter] == 32) || (strArray[iter] == 33) || (strArray[iter] <= 126 && strArray[iter] >= 40))) {
                    if (!(strArray[iter] == '%' && strArray[iter + 1] == 'd')) {
                        logU4Error(Error.formatStringError(((LeafNode) export(cs, i)).getToken().getLine()));
                        break;
                    }
                }

                if (strArray[iter] == 92) {
                    if (!(strArray[iter + 1] == 'n')) {
                        logU4Error(Error.formatStringError(((LeafNode) export(cs, i)).getToken().getLine()));
                        break;
                    }
                }
            }

            int countD = 0;
            for (int iter = 0; iter < size; iter++) {
                if (strArray[iter] == '%' && strArray[iter + 1] == 'd') {
                    countD++;
                }
            }

            i++;    // FormatString

            int countE = 0;
            while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                i++;
                Exp(export(cs, i++));
                countE++;
            }

            if (countD != countE) {
                logU4Error(Error.printfExpAmountError(((LeafNode) export(cs, i)).getToken().getLine()));
            }


        } else if (ckBrN(export(cs, i), GUNIT.LVal)) {
            SymbolItem toModify = LVal(export(cs, i));

            if (toModify instanceof VarSymbolItem && ((VarSymbolItem) toModify).isConst) {
                logU4Error(Error.modifiedConstError(((LeafNode) (export(cs, i).getChildren().get(0))).getToken().getLine()));
            } else if (toModify instanceof ArraySymbolItem && ((ArraySymbolItem) toModify).isConst) {
                logU4Error(Error.modifiedConstError(((LeafNode) (export(cs, i).getChildren().get(0))).getToken().getLine()));
            }

            i++;    // pass LVal

            i++;    // =

            if (ckLfN(export(cs, i), SYMBOL.GETINTTK)) {
                ;
            } else {
                Exp(export(cs, i));
            }
        } else if (ckBrN(export(cs, i), GUNIT.Block)) {
            symbolTable.addOneBlock(BlockType.NORMAL_BLOCK);
            Block(export(cs, i));
            symbolTable.exitPresentBlock();
        } else if (ckBrN(export(cs, i), GUNIT.Exp)) {
            Exp(export(cs, i++));

        } else {
            ;
        }
    }

    private SymbolItem Exp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        return AddExp(export(cs, i));
    }

    private SymbolItem Cond(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        return LOrExp(export(cs, i));
    }

    private SymbolItem LVal(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();

        SymbolItem found = symbolTable.getPresentBlock().findVarAndConst(name, true);
        if (found == null) {
            logU4Error(Error.undefinedError(((LeafNode) export(cs, mark)).getToken().getLine()));
            return symbolTable.getPresentBlock().createUnknown();
        }

        if (found instanceof VarSymbolItem) {
            return found;
        } else if (found instanceof ArraySymbolItem) {
            int dimensions = ((ArraySymbolItem) found).getSize().size();
            Boolean isConst = ((ArraySymbolItem) found).isConst;
            while (ckLfN(export(cs, i), SYMBOL.LBRACK)) {
                Exp(export(cs, i + 1));
                i += 3;
                dimensions--;
            }

            if (dimensions == 0) {
                return symbolTable.getPresentBlock().createTemp(found.dataType, isConst);
            }

            ArrayList<Integer> temp = new ArrayList<>();

            while (dimensions > 0) {
                dimensions--;
                temp.add(null);
            }
            return symbolTable.getPresentBlock().createTempArray(found.dataType, temp, isConst);
        } else {
            Error.warning("unexpected SymbolItem found in LVal");
            return symbolTable.getPresentBlock().createUnknown();
        }
    }

    private SymbolItem PrimaryExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.LPARENT)) {
            return Exp(export(cs, i + 1));
        } else if (ckBrN(export(cs, i), GUNIT.LVal)) {
            return LVal(export(cs, i));
        } else {
            return Number(export(cs, i));
        }
    }

    private SymbolItem Number(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        Integer value = Integer.valueOf(((LeafNode) export(cs, i)).getToken().getValue());

        return symbolTable.getPresentBlock().createTemp(BasicType.INT, true);
    }

    private SymbolItem UnaryExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckBrN(export(cs, i), GUNIT.PrimaryExp)) {
            return PrimaryExp(export(cs, i));
        } else if (ckLfN(export(cs, i), SYMBOL.IDENFR)) {
            Node funcNameNode = export(cs, i);
            String name = ((LeafNode) export(cs, i++)).getToken().getValue();

            i++;    // (

            ArrayList<SymbolItem> rParams;
            if (ckBrN(export(cs, i), GUNIT.FuncRParams)) {
                rParams = FuncRParams(export(cs, i++));
            } else {
                rParams = new ArrayList<>();
            }

            SymbolItem found = symbolTable.getPresentBlock().findFunc(name, true);

            if (found == null) {
                logU4Error(Error.undefinedError(((LeafNode) export(cs, i)).getToken().getLine()));
                return symbolTable.getPresentBlock().createUnknown();
            }

            if (found != null) {
                ArrayList<SymbolItem> fParams = ((FuncSymbolItem) found).paramsList;
                int countF = fParams.size();
                int countR = rParams.size();
                if (countF != countR) {
                    logU4Error(Error.parameterAmountError(((LeafNode) funcNameNode).getToken().getLine()));
                } else {
                    for (int iter = 0; iter < countF; iter++) {
                        String fpureType = fParams.get(iter).type().replace("const", "").replace(" ", "");
                        String rpureType = rParams.get(iter).type().replace("const", "").replace(" ", "");
                        if (!fpureType.equals(rpureType)) {
                            logU4Error(Error.parameterTypeError(((LeafNode) funcNameNode).getToken().getLine()));
                            return symbolTable.getPresentBlock().createUnknown();
                        }
                    }
                }
            }

            i++;
            if (found.dataType.equals(BasicType.INT)) {
                return symbolTable.getPresentBlock().createTemp(found.dataType, false);
            } else {
                return symbolTable.getPresentBlock().createUnknown();
            }

        } else if (ckBrN(export(cs, i), GUNIT.UnaryOp)) {
//            LeafNode op = (LeafNode) export(cs, i++).getChildren().get(0);
//            Boolean isConst = true;
//            SymbolItem item = UnaryExp(export(cs, i));
//            if (item instanceof VarSymbolItem) {
//                isConst = ((VarSymbolItem) item).isConst;
//            } else if (item instanceof ArraySymbolItem) {
//                isConst = ((ArraySymbolItem) item).isConst;
//            }
            return symbolTable.getPresentBlock().createTemp(BasicType.INT, false);
        }
        Error.warning("UnaryExp not detected");
        return null;
    }

    private void UnaryOp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
    }

    private ArrayList<SymbolItem> FuncRParams(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        ArrayList<SymbolItem> ret = new ArrayList<>();

        ret.add(Exp(export(cs, i++)));
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ret.add(Exp(export(cs, i++)));
        }
        return ret;
    }

    private SymbolItem MulExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckBrN(export(cs, i), GUNIT.UnaryExp)) {
            return UnaryExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private SymbolItem AddExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.MulExp)) {
            return MulExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private SymbolItem RelExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.AddExp)) {
            return AddExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private SymbolItem EqExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.RelExp)) {
            return RelExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private SymbolItem LAndExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.EqExp)) {
            return EqExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private SymbolItem LOrExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.LAndExp)) {
            return LAndExp(export(cs, i));
        } else {
            return symbolTable.present.createTemp(BasicType.INT, false);
        }
    }

    private Integer ConstExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        return calNode(export(cs, i));
    }

    private Integer calNode(Node syn) {
        if (syn instanceof LeafNode) {
            LeafNode curNode = ((LeafNode) syn);
            SYMBOL type = curNode.getType();
            switch (type) {
                case IDENFR:
                    SymbolItem item = symbolTable.getPresentBlock().findVarAndConst(curNode.getToken().getValue(), true);

                    Integer i = 0;
                    try {
                        i = (Integer) ((VarSymbolItem) item).getConstValue();
                    } catch (Exception e) {
                        Error.warning("when try to cal value from a ConstExp, we encounter a var can't be transformed to VarSymbolItem");
                    }

                    return i;
                case INTCON:
                    return Integer.valueOf(curNode.getToken().getValue());
            }
        } else {
            BranchNode curNode = (BranchNode) syn;
            GUNIT type = curNode.getGUnit();
            switch (type) {
                case Exp:
                    return calNode(curNode.getChildren().get(0));
                case AddExp:
                    if (curNode.getChildren().size() == 1) {
                        return calNode(curNode.getChildren().get(0));
                    }
                    Integer aa = calNode(curNode.getChildren().get(0));
                    Integer ab = calNode(curNode.getChildren().get(2));
                    SYMBOL ac = ((LeafNode) curNode.getChildren().get(1)).getToken().getSymbol();
                    if (ac.equals(SYMBOL.PLUS)) {
                        return aa + ab;
                    } else {
                        return aa - ab;
                    }
                case MulExp:
                    if (curNode.getChildren().size() == 1) {
                        return calNode(curNode.getChildren().get(0));
                    }
                    Integer ma = calNode(curNode.getChildren().get(0));
                    Integer mb = calNode(curNode.getChildren().get(2));
                    SYMBOL mc = ((LeafNode) curNode.getChildren().get(1)).getToken().getSymbol();
                    if (mc.equals(SYMBOL.MULT)) {
                        return ma * mb;
                    } else if (mc.equals(SYMBOL.DIV)) {
                        return ma / mb;
                    } else {
                        return ma % mb;
                    }
                case UnaryExp:
                    ArrayList<Node> uchildren = curNode.getChildren();
                    if (ckLfN(uchildren.get(0), SYMBOL.IDENFR)) {
                        Error.warning("a func is detected while calculating ConstExp");
                    } else if (ckBrN(uchildren.get(0), GUNIT.PrimaryExp)) {
                        return calNode(uchildren.get(0));
                    } else {
                        SYMBOL unaryOp = ((LeafNode) (uchildren.get(0).getChildren().get(0))).getToken().getSymbol();
                        Node unaryExp = uchildren.get(1);
                        if (unaryOp.equals(SYMBOL.PLUS)) {
                            return calNode(unaryExp);
                        } else if (unaryOp.equals(SYMBOL.MINU)) {
                            return -calNode(unaryExp);
                        } else {
                            Error.warning("a ! is detected when calculating ConstExp");
                        }
                    }
                case PrimaryExp:
                    ArrayList<Node> pchildren = curNode.getChildren();
                    if (ckLfN(pchildren.get(0), SYMBOL.LPARENT)) {
                        return calNode(pchildren.get(1));
                    } else if (ckBrN(pchildren.get(0), GUNIT.LVal)) {
                        return calNode(pchildren.get(0));
                    } else {
                        return calNode(pchildren.get(0));
                    }
                case LVal:
                    return calNode(curNode.getChildren().get(0));
                case Number:
                    return calNode(curNode.getChildren().get(0));
            }
        }
        Error.warning("while calculating ConstExp, we encounter a unexpected type of node");
        return null;
    }
}
