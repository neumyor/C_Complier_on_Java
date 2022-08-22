import ast.BranchNode;
import ast.LeafNode;
import ast.Node;
import component.GUNIT;
import component.Label;
import component.SYMBOL;
import component.datatype.ArrayType;
import component.datatype.Datatype;
import component.datatype.IntType;
import component.datatype.VoidType;
import component.narray.*;
import global.Error;
import imcode.imexp.IMExpType;
import imcode.imexp.IMFac;
import symbolstruct.*;
import symbolstruct.entries.*;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Converter
 * 将AST树转变为中间代码形式
 * 具体流程和Visitor差不多，都是由不同的AST节点决定行为
 * 差异在于
 * 1. Converter不考虑错误处理，默认Visitor之前完成了所有错误处理部分
 * 2. Converter中，每个函数对应的中间代码和符号表之间是耦合的，同时该符号表中还包含临时变量
 */
public class Converter {
    private final Symer symer;
    private final Node root;
    private Integer midVarCounter;

    public CodeRegion codeRegion;

    private Stack<String> whileRegionStrStack;

    public Converter(Node root) {
        this.symer = new Symer();
        this.root = root;
        this.midVarCounter = 0;
        this.codeRegion = new CodeRegion();
        this.whileRegionStrStack = new Stack<>();
    }

    private String genTmp() {
        return "_TEMPnym" + this.midVarCounter++;
    }

    private String genStrMarker() {
        return "_STR_VALUEnym" + this.midVarCounter++;
    }

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

    public CodeRegion visit() {
        CompUnit(root);
        codeRegion.gen();
        return codeRegion;
    }

    private void CompUnit(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        symer.pushScope();
        this.codeRegion.addGlbRegion(new GlobalRegion(symer.getCur()));
        int i = 0;
        while (ckBrN(export(cs, i), GUNIT.Decl)) {
            Decl(export(cs, i++));
        }

        while (ckBrN(export(cs, i), GUNIT.FuncDef)) {
            FuncDef(export(cs, i++));
        }

        MainFuncDef(export(cs, i));
        symer.quitScope();
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
        Datatype type = BType(export(cs, i++));
        ConstDef(export(cs, i++), type);
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ConstDef(export(cs, i++), type);
        }
    }

    private Datatype BType(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        // #TODO 无需特殊处理，因为只有int，而且不涉及错误处理，如果错误处理有需求再改吧
        return new IntType();
    }

    private void ConstDef(Node syn, Datatype type) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        int mark = i;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();   // 常量名称

        ArrayList<Integer> dimensions = new ArrayList<>();
        while (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 2), SYMBOL.RBRACK)) {
            Integer len = ConstExp(export(cs, i + 1));
            dimensions.add(len);
            i += 3;
        }   // 当发现该常量声明是数组，dimension的长度对应其维度，元素值对应该为长度

        i++; // 匹配'='

        Object initVal = ConstInitVal(new ArrayType(type, dimensions), export(cs, i));
        // 获取初始值，其可能是一个Integer（即非数组情况下的ConstExp）
        // 或一个NArray类型表示其数组

        if (dimensions.size() == 0) {
            // 如果是非数组的常量声明
            Entry toDef = symer.insertConstVar(name, type, (Integer) initVal);
            codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, toDef, new ConstValueEntry((Integer) initVal)));
        } else {
            // 如果是数组的常量声明
            ArrayType arrayType = new ArrayType(type, dimensions);
            Entry startPos = symer.allocSpace("Space" + genTmp(), arrayType.flat());
            Entry toDef = symer.insertConstArray(name, arrayType, (ConstNArray) initVal);

            codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVarAddr, toDef, startPos));

            for (int offset = 0; offset < arrayType.flat(); offset++) {
                Object instance = ((ConstNArray) initVal).getValues().get(offset).instance();
                if (instance instanceof Integer) {
                    Entry valueVar = symer.insertConstVar(genTmp(), type,
                            (Integer) ((ConstNArray) initVal).getValues().get(offset).instance());
                    Entry arrayOffset = symer.insertConstVar(genTmp(), new IntType(), offset * 4);
                    Entry addr = symer.insertVar(genTmp(), new IntType());
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, valueVar,
                            new ConstValueEntry((Integer) ((ConstNArray) initVal).getValues().get(offset).instance())));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, arrayOffset, new ConstValueEntry(offset * 4)));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.Add, addr, toDef, arrayOffset));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignToAddr, addr, valueVar));
                } else {
                    Error.warning("We find non-integer item in const-array-initial when we need to assign to pos");
                }
            }
        }
    }

    private Object ConstInitVal(Datatype reference, Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.LBRACE)) {
            i++;
            int referenceLen = ((ArrayType) reference).flat();
            ArrayList<NArrayItem> itemList = new ArrayList<>();

            if (!ckLfN(export(cs, i), SYMBOL.RBRACE)) {
                Datatype next = ((ArrayType) reference).getChildType();
                Object child = ConstInitVal(next, export(cs, i));
                if (child instanceof ConstNArray) {
                    itemList.addAll(((ConstNArray) child).getValues());
                } else if (child instanceof Integer) {
                    NInt tmp = new NInt((Integer) child);
                    itemList.add(tmp);
                } else {
                    Error.warning("unknown type of child returns in ConstInitialVal");
                }

                i++;

                while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                    i++;
                    Object childN = ConstInitVal(next, export(cs, i));
                    if (childN instanceof ConstNArray) {
                        itemList.addAll(((ConstNArray) childN).getValues());
                    } else if (childN instanceof Integer) {
                        NInt tmp = new NInt((Integer) childN);
                        itemList.add(tmp);
                    } else {
                        Error.warning("unknown type of child returns in ConstInitialVal");
                    }
                    i++;
                }
            }

            if (((ArrayType) reference).dimension.size() != 1 && itemList.size() != referenceLen) {
                // 在生成高维时，发现初始化的数组的长度不符合要求
                // 比如const int a[5][5] = {{1},{2}};
                Error.warning("Initial an array but lost some dimensions, it's not allowed when this is high dimension");
            } else if (itemList.size() != referenceLen) {
                // 在生成低维度时，可以用零补全
                while (itemList.size() < referenceLen) {
                    itemList.add(new NInt(0));
                }
            }

            ConstNArray ret = new ConstNArray(reference, itemList);
            return ret;
        } else {
            return ConstExp(export(cs, i));
        }
    }

    private void VarDecl(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        Datatype type = BType(export(cs, i++));
        VarDef(export(cs, i++), type);
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            VarDef(export(cs, i++), type);
        }
    }

    private void VarDef(Node syn, Datatype type) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();

        ArrayList<Integer> dimensions = new ArrayList<>();
        while (ckLfN(export(cs, i), SYMBOL.LBRACK) && ckLfN(export(cs, i + 2), SYMBOL.RBRACK)) {
            Integer len = ConstExp(export(cs, i + 1));
            dimensions.add(len);
            i += 3;
        }

        Object value = null;
        if (ckLfN(export(cs, i), SYMBOL.ASSIGN)) {
            i++; // =
            value = InitVal(new ArrayType(type, dimensions), export(cs, i));
            // 可能返回一个VarEntry，也可能返回一个NArray对象
        }

        AbsVarEntry toDef;

        if (dimensions.size() == 0) {
            // 如果是非数组声明
            toDef = (AbsVarEntry) symer.insertVar(name, type);

            if (value != null) {
                codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, toDef, value));
            }
        } else {
            // 如果是数组声明
            ArrayType arrayType = new ArrayType(type, dimensions);

            Entry startPos = symer.allocSpace("Space" + genTmp(), arrayType.flat());

            toDef = (AbsVarEntry) symer.insertArray(name, arrayType);

            codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVarAddr, toDef, startPos));

            if (value != null) {
                for (int offset = 0; offset < arrayType.flat(); offset++) {
                    Object instance = ((NArray) value).getValues().get(offset).instance();
                    Entry valueVar;

                    if (instance instanceof VarEntry) {
                        valueVar = (Entry) ((NArray) value).getValues().get(offset).instance();
                    } else if (instance instanceof Integer) {
                        valueVar = symer.insertConstVar(genTmp(), type,
                                (Integer) ((NArray) value).getValues().get(offset).instance());
                        codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, valueVar, ((NArray) value).getValues().get(offset).instance()));
                    } else {
                        valueVar = null;
                        Error.warning("Unknown type of object extracted from array when we need to assignToPos");
                    }

                    Entry arrayOffset = symer.insertConstVar(genTmp(), new IntType(), offset * 4);
                    Entry addr = symer.insertVar(genTmp(), new IntType());
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, arrayOffset,
                            new ConstValueEntry(offset * 4)));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.Add, addr, toDef, arrayOffset));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignToAddr, addr, valueVar));
                }
            }
        }
    }

    private Object InitVal(Datatype reference, Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.LBRACE)) {
            i++;
            int referenceLen = ((ArrayType) reference).flat();
            ArrayList<NArrayItem> itemList = new ArrayList<>();

            if (!ckLfN(export(cs, i), SYMBOL.RBRACE)) {
                Datatype next = ((ArrayType) reference).getChildType();
                Object child = InitVal(next, export(cs, i));
                if (child instanceof NArray) {
                    itemList.addAll(((NArray) child).getValues());
                } else {
                    itemList.add((VarEntry) child);
                }
                i++;
                while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                    i++;
                    Object childN = InitVal(next, export(cs, i));
                    if (childN instanceof NArray) {
                        itemList.addAll(((NArray) childN).getValues());
                    } else {
                        itemList.add((VarEntry) childN);
                    }
                    i++;
                }
            }

            if (((ArrayType) reference).dimension.size() != 1 && itemList.size() != referenceLen) {
                // 在生成高维时，发现初始化的数组的长度不符合要求
                // 比如const int a[5][5] = {{1},{2}};
                Error.warning("Initial an array but lost some dimensions, it's not allowed when this is high dimension");
            } else if (itemList.size() != referenceLen) {
                // 在生成低维度时，可以用零补全
                while (itemList.size() < referenceLen) {
                    itemList.add(new NInt(0));
                }
            }
            NArray ret = new NArray(reference, itemList);
            return ret;
        } else {
            return Exp(export(cs, i));
        }
    }

    private void FuncDef(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        Datatype retType = FuncType(export(cs, i++));
        String name = ((LeafNode) export(cs, i++)).getToken().getValue();
        i++;    // (

        FuncEntry funcEntry = (FuncEntry) symer.insertFunc(name, retType, null);

        symer.pushScope();

        FuncRegion region = new FuncRegion(name, symer.getCur());
        codeRegion.addRegion(region);


        ArrayList<AbsVarEntry> fParams = new ArrayList<>();
        if (ckBrN(export(cs, i), GUNIT.FuncFParams)) {
            fParams = FuncFParams(export(cs, i++));
        }

        funcEntry.params = fParams;

        i++;    // )

        Block(export(cs, i));

        symer.quitScope();
    }

    private void MainFuncDef(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        Datatype retType = new IntType();
        String name = "main";
        i += 3;    // int main (

        FuncEntry funcEntry = (FuncEntry) symer.insertFunc(name, retType, null);

        symer.pushScope();

        FuncRegion region = new FuncRegion(name, symer.getCur());
        codeRegion.addRegion(region);

        ArrayList<AbsVarEntry> fParams = new ArrayList<>();
        if (ckBrN(export(cs, i), GUNIT.FuncFParams)) {
            fParams = FuncFParams(export(cs, i++));
        }

        funcEntry.params = fParams;

        i++;    // )

        Block(export(cs, i));

        symer.quitScope();
    }

    private Datatype FuncType(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (((LeafNode) export(cs, i)).getToken().getSymbol().equals(SYMBOL.VOIDTK)) {
            return new VoidType();
        } else {
            return new IntType();
        }
    }

    private ArrayList<AbsVarEntry> FuncFParams(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        ArrayList<AbsVarEntry> ret = new ArrayList<>();
        ret.add(FuncFParam(export(cs, i++)));
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ret.add(FuncFParam(export(cs, i++)));
        }
        return ret;
    }

    private AbsVarEntry FuncFParam(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        Datatype type = BType(export(cs, i++));
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

        AbsVarEntry toDef;
        if (dimensions.size() == 0) {
            // 如果是非数组的声明
            toDef = (AbsVarEntry) symer.insertParaVar(name, type);
            codeRegion.addIMExp(IMFac.gen(IMExpType.ParaDef, toDef));
        } else {
            // 如果是数组的常量声明
            ArrayType arrayType = new ArrayType(type, dimensions);
            toDef = (AbsVarEntry) symer.insertParaArray(name, arrayType);
            codeRegion.addIMExp(IMFac.gen(IMExpType.ParaDef, toDef));
        }

//        codeRegion.addIMExp(IMFac.gen(IMExpType.FuncParaDef, toDef));
        return toDef;
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
            i += 2; // if and (

            String structStr = codeRegion.curRegion.name + genTmp();
            Label if_end_label = new Label(structStr + "_IF_END");
            Label if_body_begin_label = new Label(structStr + "_IF_BODY_BEGIN");

            CondNotJumpTo(export(cs, i++), if_body_begin_label, if_end_label);

            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, if_body_begin_label));

//            codeRegion.addIMExp(IMFac.gen(IMExpType.ConNotJump, if_end_label, cond));

            i++;  // )
            Stmt(export(cs, i++));

            if (ckLfN(export(cs, i), SYMBOL.ELSETK)) {
                i++;

                Label else_end_label = new Label(structStr + "_ELSE_END");
                codeRegion.addIMExp(IMFac.gen(IMExpType.Jump, else_end_label));

                codeRegion.addIMExp(IMFac.gen(IMExpType.Label, if_end_label));

                symer.pushScope();
                Stmt(export(cs, i));
                symer.quitScope();

                codeRegion.addIMExp(IMFac.gen(IMExpType.Label, else_end_label));
            } else {
                codeRegion.addIMExp(IMFac.gen(IMExpType.Label, if_end_label));
            }
        } else if (ckLfN(export(cs, i), SYMBOL.WHILETK)) {

            i += 2; // while and (

            this.whileRegionStrStack.push(codeRegion.curRegion.name + genTmp());

            Label while_end_label = new Label(whileRegionStrStack.peek() + "_WHILE_END");
            Label while_begin_label = new Label(whileRegionStrStack.peek() + "_WHILE_BEGIN");
            Label while_body_begin_label = new Label(whileRegionStrStack.peek() + "_WHILE_BODY_BEGIN");

            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, while_begin_label));

            CondNotJumpTo(export(cs, i++), while_body_begin_label, while_end_label);

            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, while_body_begin_label));

//            codeRegion.addIMExp(IMFac.gen(IMExpType.ConNotJump, while_end_label, cond));

            i++;  // )
            Stmt(export(cs, i));

            codeRegion.addIMExp(IMFac.gen(IMExpType.Jump, while_begin_label));
            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, while_end_label));

            this.whileRegionStrStack.pop();

        } else if (ckLfN(export(cs, i), SYMBOL.BREAKTK)) {
            i++;    // break
            Label while_end_label = new Label(whileRegionStrStack.peek() + "_WHILE_END");
            codeRegion.addIMExp(IMFac.gen(IMExpType.Jump, while_end_label));
        } else if (ckLfN(export(cs, i), SYMBOL.CONTINUETK)) {
            i++;    // continue
            Label while_begin_label = new Label(whileRegionStrStack.peek() + "_WHILE_BEGIN");
            codeRegion.addIMExp(IMFac.gen(IMExpType.Jump, while_begin_label));
        } else if (ckLfN(export(cs, i), SYMBOL.RETURNTK)) {
            int mark = i;

            i++;    // return

            if (ckBrN(export(cs, i), GUNIT.Exp)) {
                Entry retEntry = Exp(export(cs, i));
                codeRegion.addIMExp(IMFac.gen(IMExpType.Return, retEntry));
                i++;
            } else {
                codeRegion.addIMExp(IMFac.gen(IMExpType.Return, null));
            }
        } else if (ckLfN(export(cs, i), SYMBOL.PRINTFTK)) {
            i += 2;   //printf and (

            String rawStr = ((LeafNode) export(cs, i)).getToken().getValue().replace("\"", "");

            i++;    // FormatString

            ArrayList<Entry> entries = new ArrayList<>();
            while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
                i++;
                entries.add(Exp(export(cs, i++)));
            }

            String[] slices = rawStr.split("(?<=%d)|(?=%d)");
            int entryIter = 0;
            for (String slice : slices) {
                if (slice.equals("%d")) {
                    codeRegion.addIMExp(IMFac.gen(IMExpType.PrintVar, entries.get(entryIter)));
                    entryIter++;
                } else {
                    String marker = genStrMarker();
                    this.codeRegion.addStr(marker, slice);
                    codeRegion.addIMExp(IMFac.gen(IMExpType.PrintStr, marker));
                }
            }
        } else if (ckBrN(export(cs, i), GUNIT.LVal)) {
            Node lvalNode = export(cs, i);
            LeafNode ident = (LeafNode) lvalNode.getChildren().get(0);
            Entry entry = symer.findVar(ident.getToken().getValue());

            if (entry instanceof VarEntry) {
                i++;    // pass LVal

                i++;    // =

                if (ckLfN(export(cs, i), SYMBOL.GETINTTK)) {
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByGetInt, entry));
                } else {
                    Entry value = Exp(export(cs, i));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, entry, value));
                }
            } else {
                ArrayEntry arrayEntry = (ArrayEntry) entry;

                if (lvalNode.getChildren().size() > 3 &&
                        ckLfN(lvalNode.getChildren().get(3), SYMBOL.RBRACK)) {

                    int counter = 3;
                    ArrayList<VarEntry> pos = new ArrayList<>();
                    while (counter < lvalNode.getChildren().size() &&
                            ckLfN(lvalNode.getChildren().get(counter), SYMBOL.RBRACK)) {
                        Entry dim = Exp(lvalNode.getChildren().get(counter - 1));
                        counter += 3;
                        pos.add((VarEntry) dim);
                    }

                    NPos npos = new NPos(pos);
                    if (npos.getDims() != ((ArrayType) arrayEntry.datatype).dimension.size()) {
                        Error.warning("LVal can't be an array when it's a left-value");
                    }

                    i++;    // pass LVal

                    i++;    // =

                    Entry value;

                    if (ckLfN(export(cs, i), SYMBOL.GETINTTK)) {
                        value = symer.insertVar(genTmp(), new IntType());
                        codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByGetInt, value));
                    } else {
                        value = Exp(export(cs, i));
                    }

                    Entry arrayOffset = symer.insertVar(genTmp(), new IntType());
                    Entry addr = symer.insertVar(genTmp(), new IntType());
                    genPosToOffset(arrayOffset, arrayEntry, npos);
                    codeRegion.addIMExp(IMFac.gen(IMExpType.Add, addr, arrayEntry, arrayOffset));
                    codeRegion.addIMExp(IMFac.gen(IMExpType.AssignToAddr, addr, value));
                } else {
                    Error.warning("LVal is an array but with no [], this is not allowed");
                }
            }
        } else if (ckBrN(export(cs, i), GUNIT.Block)) {
            symer.pushScope();
            Block(export(cs, i));
            symer.quitScope();
        } else if (ckBrN(export(cs, i), GUNIT.Exp)) {
            Exp(export(cs, i++));
        } else {
            // 对应单一个 ;
            ;
        }
    }

    private Entry Exp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        return AddExp(export(cs, i));
    }

    private void CondNotJumpTo(Node syn, Label yesLabel, Label notlabel) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        LOrExp(export(cs, i), yesLabel, notlabel, true);
    }

    private Entry LVal(Boolean isLeft, Node syn) {
        ArrayList<Node> cs = syn.getChildren();

        LeafNode ident = (LeafNode) syn.getChildren().get(0);
        Entry entry = symer.findVar(ident.getToken().getValue());

        if (isLeft) {
            Error.warning("When LVal plats the left-value role, it should be processed in Stmt instead of LVal");
            return null;
        } else {
            if (entry instanceof VarEntry) {
                Entry tmp = symer.insertVar(genTmp(), entry.datatype);

                codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, tmp, entry));
                return tmp;
            }
            ArrayEntry arrayEntry = (ArrayEntry) entry;
            if (syn.getChildren().size() > 3 &&
                    ckLfN(syn.getChildren().get(3), SYMBOL.RBRACK)) {
                int i = 3;
                ArrayList<VarEntry> pos = new ArrayList<>();
                while (i < syn.getChildren().size() &&
                        ckLfN(syn.getChildren().get(i), SYMBOL.RBRACK)) {
                    Entry dim = Exp(syn.getChildren().get(i - 1));
                    i += 3;
                    pos.add((VarEntry) dim);
                }
                NPos npos = new NPos(pos);
                if (npos.getDims() < ((ArrayType) arrayEntry.datatype).dimension.size()) {

                    Entry offsetEntry = symer.insertVar(genTmp(), new IntType());

                    genPosToOffset(offsetEntry, arrayEntry, npos);

                    ArrayList<Integer> tmp = new ArrayList<>();
                    for (int iter = 0; iter < ((ArrayType) arrayEntry.datatype).dimension.size() - npos.getDims(); iter++) {
                        tmp.add(((ArrayType) arrayEntry.datatype).dimension.get(iter));
                    }

                    Entry sliceEntry = symer.insertArray(genTmp(),
                            new ArrayType(((ArrayType) arrayEntry.datatype).basicType, tmp));

                    codeRegion.addIMExp(IMFac.gen(IMExpType.Add, sliceEntry, offsetEntry, arrayEntry));

                    return sliceEntry;
                }
                Entry tmp = symer.insertVar(genTmp(), ((ArrayType) arrayEntry.datatype).basicType);
                Entry offset = symer.insertVar(genTmp(), new IntType());

                genPosToOffset(offset, arrayEntry, npos);
                codeRegion.addIMExp(IMFac.gen(IMExpType.Add, tmp, arrayEntry, offset));
                codeRegion.addIMExp(IMFac.gen(IMExpType.AssignFromAddr, tmp, tmp));
                return tmp;
            } else {
                return entry;
            }
        }
    }

    private Entry PrimaryExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckLfN(export(cs, i), SYMBOL.LPARENT)) {
            return Exp(export(cs, i + 1));
        } else if (ckBrN(export(cs, i), GUNIT.LVal)) {
            return LVal(false, export(cs, i));
        } else {
            return Number(export(cs, i));
        }
    }

    private Entry Number(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        Integer intvalue = Integer.valueOf(((LeafNode) export(cs, i)).getToken().getValue());
        Entry tmp = symer.insertConstVar(genTmp(), new IntType(), intvalue);
        codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, tmp, new ConstValueEntry(intvalue)));
        return tmp;
    }

    private Entry UnaryExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckBrN(export(cs, i), GUNIT.PrimaryExp)) {
            return PrimaryExp(export(cs, i));
        } else if (ckLfN(export(cs, i), SYMBOL.IDENFR)) {
            Node funcNameNode = export(cs, i);
            String name = ((LeafNode) export(cs, i++)).getToken().getValue();

            i++;    // (

            ArrayList<Entry> rParams;
            if (ckBrN(export(cs, i), GUNIT.FuncRParams)) {
                rParams = FuncRParams(export(cs, i++));
            } else {
                rParams = new ArrayList<>();
            }

            int counter = 1;
            for (Entry e : rParams) {
                codeRegion.addIMExp(IMFac.gen(IMExpType.PushPara, e, counter++)); // 加载参数,注意加载顺序是从左往右
            }

            Entry found = symer.findFunc(name);

            i++;

            if (found.datatype instanceof IntType) {
                Entry getRet = symer.insertVar(genTmp(), found.datatype);
                codeRegion.addIMExp(IMFac.gen(IMExpType.Call, found));
                codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByRet, getRet));
                return getRet;
            } else {
                codeRegion.addIMExp(IMFac.gen(IMExpType.Call, found));
                return null;
            }

        } else if (ckBrN(export(cs, i), GUNIT.UnaryOp)) {
            String op = UnaryOp(export(cs, i));
            i++;
            Entry toChange = UnaryExp(export(cs, i));
            if (op.equals("-")) {
                Entry changed = symer.insertVar(genTmp(), toChange.datatype);
                Entry negativeOne = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, negativeOne, new ConstValueEntry(-1)));
                codeRegion.addIMExp(IMFac.gen(IMExpType.Mul, changed, toChange, negativeOne));
                return changed;
            } else if (op.equals("!")) {
                Entry changed = symer.insertVar(genTmp(), toChange.datatype);
                codeRegion.addIMExp(IMFac.gen(IMExpType.Beq, changed, toChange, new ConstValueEntry(0)));
                return changed;
            }
            return toChange;
        }
        Error.warning("UnaryExp not detected");
        return null;
    }

    private String UnaryOp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();

        return ((LeafNode) cs.get(0)).getToken().getValue();
    }

    private ArrayList<Entry> FuncRParams(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        ArrayList<Entry> ret = new ArrayList<>();

        ret.add(Exp(export(cs, i++)));
        while (ckLfN(export(cs, i), SYMBOL.COMMA)) {
            i++;
            ret.add(Exp(export(cs, i++)));
        }
        return ret;
    }

    private Entry MulExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;

        if (ckBrN(export(cs, i), GUNIT.UnaryExp)) {
            return UnaryExp(export(cs, i));
        } else {
            Entry a = MulExp((export(cs, i)));
            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;
            Entry b = UnaryExp(export(cs, i));

            if (op.equals("*")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Mul, ret, a, b));
                return ret;
            } else if (op.equals("/")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Div, ret, a, b));
                return ret;
            } else if (op.equals("%")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Mod, ret, a, b));
                return ret;
            } else {
                Error.warning("Unsupported operator in MulExp");
                return null;
            }
        }
    }

    private Entry AddExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.MulExp)) {
            return MulExp(export(cs, i));
        } else {
            Entry a = AddExp((export(cs, i)));
            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;
            Entry b = MulExp(export(cs, i));

            if (op.equals("+")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Add, ret, a, b));
                return ret;
            } else if (op.equals("-")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Sub, ret, a, b));
                return ret;
            } else {
                Error.warning("Unsupported operator in AddExp");
                return null;
            }
        }
    }

    private Entry RelExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.AddExp)) {
            return AddExp(export(cs, i));
        } else {
            Entry a = RelExp((export(cs, i)));
            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;
            Entry b = AddExp(export(cs, i));

            if (op.equals(">")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Bgt, ret, a, b));
                return ret;
            } else if (op.equals("<")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Bgt, ret, b, a));
                return ret;
            } else if (op.equals("<=")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Bge, ret, b, a));
                return ret;
            } else if (op.equals(">=")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Bge, ret, a, b));
                return ret;
            } else {
                Error.warning("Unsupported operator in RelExp");
                return null;
            }
        }
    }

    private Entry EqExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.RelExp)) {
            return RelExp(export(cs, i));
        } else {
            Entry a = EqExp((export(cs, i)));
            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;
            Entry b = RelExp(export(cs, i));

            if (op.equals("==")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Beq, ret, a, b));
                return ret;
            } else if (op.equals("!=")) {
                Entry ret = symer.insertVar(genTmp(), new IntType());
                codeRegion.addIMExp(IMFac.gen(IMExpType.Bne, ret, a, b));
                return ret;
            } else {
                Error.warning("Unsupported operator in EqExp");
                return null;
            }
        }
    }

    private void SimpleCondExp(Node syn, Label yesLabel, Label failLabel, Boolean isYesFollow) {
        Entry entry = EqExp(syn);
        if(isYesFollow) {
            codeRegion.addIMExp(IMFac.gen(IMExpType.ConNotJump, failLabel, entry));
        } else {
            codeRegion.addIMExp(IMFac.gen(IMExpType.ConJump, yesLabel, entry));
        }
    }

    private void LAndExp(Node syn, Label yesLabel, Label failLabel, Boolean isYesFollow) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.EqExp)) {
            SimpleCondExp(export(cs, i), yesLabel, failLabel, isYesFollow);
        } else {
            String structStr = codeRegion.curRegion.name + genTmp();
            Label ifFirstCondYesLabel = new Label(structStr);

            LAndExp((export(cs, i)), ifFirstCondYesLabel, failLabel, true);

            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, ifFirstCondYesLabel));

            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;

            SimpleCondExp(export(cs, i), yesLabel, failLabel, isYesFollow);

            if (op.equals("&&")) {
                ;
            } else {
                Error.warning("Unsupported operator in LAndExp");
            }
        }
    }

    private void LOrExp(Node syn, Label yesLabel, Label failLabel, Boolean isYesFollow) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        if (ckBrN(export(cs, i), GUNIT.LAndExp)) {
            LAndExp(export(cs, i), yesLabel, failLabel, isYesFollow);
        } else {
            String structStr = codeRegion.curRegion.name + genTmp();
            Label ifFirstCondFailLabel = new Label(structStr);

            LOrExp((export(cs, i)), yesLabel, ifFirstCondFailLabel, false);

            codeRegion.addIMExp(IMFac.gen(IMExpType.Label, ifFirstCondFailLabel));

            i++;
            String op = ((LeafNode) export(cs, i)).getToken().getValue();
            i++;

            LAndExp(export(cs, i), yesLabel, failLabel, isYesFollow);

            if (op.equals("||")) {
                ;
            } else {
                Error.warning("Unsupported operator in LOrExp");
            }
        }
    }

    private Integer ConstExp(Node syn) {
        ArrayList<Node> cs = syn.getChildren();
        int i = 0;
        return calNode(export(cs, i));
    }

    /**
     * 用于计算ConstExp中Exp的值的
     * 当该Exp结果不是整型 或 不是常量时，该函数会寄
     *
     * @param syn Exp对应节点
     * @return 整型结果
     */
    private Integer calNode(Node syn) {
        if (syn instanceof LeafNode) {
            LeafNode curNode = ((LeafNode) syn);
            SYMBOL type = curNode.getType();
            switch (type) {
                case INTCON:
                    return Integer.valueOf(curNode.getToken().getValue());
            }
        } else {
            BranchNode curNode = (BranchNode) syn;
            GUNIT type = curNode.getGUnit();
            switch (type) {
                case LVal:
                    LeafNode ident = (LeafNode) curNode.getChildren().get(0);
                    Entry entry = symer.findVar(ident.getToken().getValue());
                    if (curNode.getChildren().size() > 3 &&
                            ckLfN(curNode.getChildren().get(3), SYMBOL.RBRACK)) {
                        int i = 3;
                        ArrayList<Integer> pos = new ArrayList<>();
                        while (i < curNode.getChildren().size() &&
                                ckLfN(curNode.getChildren().get(i), SYMBOL.RBRACK)) {
                            Integer dim = calNode(curNode.getChildren().get(i - 1));
                            i += 3;
                            pos.add(dim);
                        }
                        ConstNArray array = ((ConstNArray) ((ConstArrayEntry) entry).getValue());
                        ArrayType arrayType = array.getDatatype();
                        int offset = 0;
                        for (int iter = 0; iter < pos.size(); iter++) {
                            int tmp = pos.get(iter);
                            for (int iter2 = iter + 1; iter2 < arrayType.dimension.size(); iter2++) {
                                tmp *= arrayType.dimension.get(iter2);
                            }
                            offset += tmp;
                        }

                        return (Integer) array.getValues().get(offset).instance();
                    } else {
                        return (Integer) ((ConstVarEntry) entry).getValue();
                    }
                case Exp:
                case Number:
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
            }
        }
        Error.warning("while calculating ConstExp, we encounter a unexpected type of node");
        return null;
    }


    private void genPosToOffset(Entry toAssign, Entry array, NPos npos) {
        assert toAssign instanceof AbsVarEntry;
        assert array instanceof ArrayEntry;

        ArrayType arrayType = (ArrayType) array.datatype;
        Datatype basicType = arrayType.basicType;
        int basicLen = basicType.spaceSize();

        Entry temp = symer.insertVar(genTmp(), new IntType());

        codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, toAssign, new ConstValueEntry(0)));

        for (int i = 0; i < npos.getDims(); i++) {
            int len = basicLen;
            for (int j = i + 1; j < arrayType.dimension.size(); j++) {
                len *= arrayType.dimension.get(j);
            }

            codeRegion.addIMExp(IMFac.gen(IMExpType.AssignByVar, temp, new ConstValueEntry(len)));

            Entry b = npos.getOnDim(i);

            codeRegion.addIMExp(IMFac.gen(IMExpType.Mul, temp, b, temp));
            codeRegion.addIMExp(IMFac.gen(IMExpType.Add, toAssign, toAssign, temp));
        }
    }
}
