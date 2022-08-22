import ast.BranchNode;
import ast.LeafNode;
import ast.Node;
import component.GUNIT;
import component.SYMBOL;
import component.Token;
import global.Error;
import global.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GrammarParser同时进行语法处理
 * 符号表在这一步被填充
 * 最终生成中间代码
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class GrammarParser {
    private final List<Token> tokenList;
    private Integer pToken;


    private ArrayList<Token> DEBUG_WINDOW;

    public GrammarParser(List<Token> tokenList) {
        this.tokenList = tokenList;
        this.pToken = -1;
    }

    private void DEBUG() {
        this.DEBUG_WINDOW = new ArrayList<>();
        this.DEBUG_WINDOW.add(this.tokenList.get(pToken - 2));
        this.DEBUG_WINDOW.add(this.tokenList.get(pToken - 1));
        this.DEBUG_WINDOW.add(this.tokenList.get(pToken));
        this.DEBUG_WINDOW.add(this.tokenList.get(pToken + 1));
        this.DEBUG_WINDOW.add(this.tokenList.get(pToken + 2));
    }

    private void logU4Error(String message) {
        Logger.GetLogger().ErrorLog(message);
    }

    private Boolean isNextExpAndSemicon() {
        int pTokenMark = pToken;
        Node trial = Exp(-1);
        if (pToken == pTokenMark) {
            // 如果根本没检查到Exp，就返回错误
            return false;
        }
        Node semicn = checkSymbol(SYMBOL.SEMICN);
        pToken = pTokenMark;
        //  如果查到了Exp，但是Exp由于缺失中括号呈现false，此时分号一定正确，因此返回正确
        //  如果查到Exp但是缺失分号，此时Exp一定正确，因此返回正确
        return trial.isCorrect() || semicn.isCorrect();
    }

    private Boolean isNextLValAndAssign() {
        int pTokenMark = pToken;
        Node lval = LVal(-1);
        Node assign = checkSymbol(SYMBOL.ASSIGN);
        pToken = pTokenMark;
        //  如果跨过LVal检测到=号，返回正确
        //  LVal的正确性或存在性（目前不可能不存在）不影响正确性
        return assign.isCorrect();
    }

    private Boolean isNextFuncRParams() {
        int pTokenMark = pToken;
        Node funcRParams = FuncRParams(-1);
        pToken = pTokenMark;
        return funcRParams.isCorrect();
    }

    private Boolean isNextFuncFParams() {
        int pTokenMark = pToken;
        Node funcFParams = FuncFParams(-1);
        if (pToken == pTokenMark) {
            //  没有检测到，则返回错误
            return false;
        }
        pToken = pTokenMark;
        //  FuncFParams由于缺失中括号而呈现错误，但是显然检测到了
        //  如果FuncFParams确实缺失中括号，那么后一个符号一定是右括号，因为错误不会发生在同一行
        return true;
    }

    /* 指针管理函数 */

    private void next() {
        this.pToken++;
    }

    private Token getCurToken() {
        if (this.pToken == -1) return new Token(SYMBOL.UNKNOWN, "STARTVALUE", -1);
        try {
            return this.tokenList.get(this.pToken);
        } catch (IndexOutOfBoundsException e) {
            return new Token(SYMBOL.UNKNOWN, "ENDVALUE", Integer.MAX_VALUE);
        }

    }

    private Token preToken() {
        if (this.pToken == -1) return new Token(SYMBOL.UNKNOWN, "STARTVALUE", -1);
        try {
            return this.tokenList.get(this.pToken - 1);
        } catch (IndexOutOfBoundsException e) {
            return new Token(SYMBOL.UNKNOWN, "ENDVALUE", Integer.MAX_VALUE);
        }
    }
    
    private Token extractTokenFromLeafNode(Node node) {
        if(!(node instanceof LeafNode)) {
            Error.warning("can't extract token from a non-leaf node");
            return null;
        }
        return ((LeafNode)node).getToken();
    }

    /**
     * 超前读取Token
     *
     * @return 当前指针的之后的Token
     */
    private Token peek() {
        return tokenList.get(pToken + 1);
    }

    private Token peek(Integer offset) {
        if (pToken + offset >= tokenList.size()) {
            Error.warning("peek() crossed the border");
            return new Token(SYMBOL.UNKNOWN, "unknown", Integer.MAX_VALUE);
        }
        return tokenList.get(pToken + offset);
    }

    /**
     * 不断超前读取直到遇到分号
     *
     * @param symbol 需要超前读取到的SYMBOL类型
     * @return 是否在遇到分号前读取到指定SYMBOL类型
     */
    private Boolean peekUntil(SYMBOL until, SYMBOL symbol) {
        int i = 1;
        Token token = peek(i);
        while (!token.getSymbol().equals(symbol) && !token.getSymbol().equals(until) &&
                !token.getSymbol().equals(SYMBOL.UNKNOWN)) {
            token = peek(++i);
        }
        return !token.getSymbol().equals(until);
    }

    private Boolean peekMultiUntil(SYMBOL until, SYMBOL... symbols) {
        int i = 1;
        Token token = peek(i);
        Set<SYMBOL> set = Arrays.stream(symbols).collect(Collectors.toSet());
        while (!set.contains(token.getSymbol()) || token.getSymbol().equals(until)) {
            token = peek(++i);
        }
        return !token.getSymbol().equals(until);
    }


    /* 非终结符检查函数 */
    private Node checkMultSymbol(SYMBOL... symbols) {
        System.out.println("Checking: " + peek().getValue() + " " + peek().getLine() + " with " + symbols);
        int cnt = 0;
        for (SYMBOL symbol : symbols) {
            if (peek().getSymbol().equals(symbol)) {
                cnt += 1;
            }
        }
        if (cnt == 0) {
            //  当检查失败，返回的Node内存储的是上一个符号的位置
            return new LeafNode(symbols[0], preToken(), false);
        } else {
            next();
            System.out.println("check pass");
            return new LeafNode(getCurToken().getSymbol(), getCurToken(), true);
        }

    }

    private Node checkSymbol(SYMBOL symbol) {
        System.out.println("Checking: " + peek().getValue() + " " + peek().getLine() + " with " + symbol);
        if (peek().getSymbol().equals(symbol)) {
            next();
            System.out.println("check pass");
            return new LeafNode(symbol, getCurToken(), true);
        } else {
            //  当检查失败，返回的Node内存储的是上一个符号的位置
            return new LeafNode(symbol, preToken(), false);
        }

    }

    /* 符号判断函数，仅仅是因为我懒得到处写  */
    private Node pureMulOp(int level) {
        SYMBOL[] bolster = {SYMBOL.DIV, SYMBOL.MULT, SYMBOL.MOD};
        return checkMultSymbol(bolster);
    }

    private Node pureCompareOp(int level) {
        SYMBOL[] bolster = {SYMBOL.LEQ, SYMBOL.GEQ, SYMBOL.LSS, SYMBOL.GRE};
        return checkMultSymbol(bolster);
    }

    private Node pureEquOp(int level) {
        SYMBOL[] bolster = {SYMBOL.EQL, SYMBOL.NEQ};
        return checkMultSymbol(bolster);
    }

    private Node pureAddOp(int level) {
        SYMBOL[] bolster = {SYMBOL.PLUS, SYMBOL.MINU};
        return checkMultSymbol(bolster);
    }


    /* 入口函数 */

    private void postorderTraversal(Node root) {
        if (root instanceof BranchNode) {
            ArrayList<Node> children = root.getChildren();
            for (Node node : children) {
                postorderTraversal(node);
            }

            GUNIT type = ((BranchNode) root).getGUnit();
            if (!type.equals(GUNIT.BlockItem) &&
                    !type.equals(GUNIT.BType) &&
                    !type.equals(GUNIT.Decl)) {
                Logger.GetLogger().GrammarLog("<" + type + ">");
            }
        } else if (root instanceof LeafNode) {
            Logger.GetLogger().GrammarLog(((LeafNode) root).getType().toString() + " " + ((LeafNode) root).getToken().getValue());
        } else {
            throw new RuntimeException("IN GrammarParser: Unknown type of Node is being postorder traversing!");
        }
    }

    public Node parse() {
        Node root = CompUnit(0);
        postorderTraversal(root);
        return root;
    }

    public void test() {
        Node root = BlockItem(0);
        postorderTraversal(root);
    }

    /* 正儿八经的解析函数 */
    /* 解析函数需要遵从：在抛出异常并返回时，能够将状态回退到进入try语句块之前 */

    private Node CompUnit(int level) {
        GUNIT curBlock = GUNIT.CompUnit;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        while (!peek(3).getSymbol().equals(SYMBOL.LPARENT)) {
            /* 当前往后的第三个字符，对于FuncDef和MainFunc来说都是LPARENT，而对于Decl来说不可能是LPARENT */
            Node tmp = Decl(level + 1);
            children.add(tmp);
        }

        while (!peek(2).getSymbol().equals(SYMBOL.MAINTK)) {
            /* 当前往后两个字符对于MainFuncDef来说一定是MAINTK，而对于FuncDef来说是IDENT */
            Node tmp = FuncDef(level + 1);
            children.add(tmp);  // tmp 说明该节点可有可无
        }

        Node vipNode = MainFuncDef(level + 1);  // vipNode 说明该子节点对于当前节点是否成立来说是至关重要的
        children.add(vipNode);  // vipNode 会被立即加入children列表，无论其是否正确

        ret.setCorrect(vipNode.isCorrect());
        children.forEach(ret::addChild);

        return ret;
    }

    private Node Decl(int level) {
        GUNIT curBlock = GUNIT.Decl;
        BranchNode ret = new BranchNode(curBlock);
        Node vipNode;
        if (peek().getSymbol().equals(SYMBOL.CONSTTK)) {
            vipNode = ConstDecl(level);
        } else {
            vipNode = VarDecl(level);
        }
        ret.addChild(vipNode);
        ret.setCorrect(vipNode.isCorrect());
        return ret;
    }

    private Node ConstDecl(int level) {
        GUNIT curBlock = GUNIT.ConstDecl;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.CONSTTK);
        children.add(vipNode1);
        Node vipNode2 = BType(level);
        children.add(vipNode2);
        Node vipNode3 = ConstDef(level);
        children.add(vipNode3);
        while (peek().getSymbol().equals(SYMBOL.COMMA)) {
            Node tmp1 = checkSymbol(SYMBOL.COMMA);
            Node tmp2 = ConstDef(level);
            children.add(tmp1);
            children.add(tmp2);  // tmp1 和 tmp2 是相互依存的关系，因此必须一起存在
        }
        Node vipNode4 = checkSymbol(SYMBOL.SEMICN);
        if (!vipNode4.isCorrect()) {
            logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
        }
        children.add(vipNode4);
        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect());
        children.forEach(ret::addChild);
        return ret;
    }

    private Node BType(int level) {
        GUNIT curBlock = GUNIT.BType;
        BranchNode ret = new BranchNode(curBlock);
        Node vipNode = checkSymbol(SYMBOL.INTTK);
        ret.addChild(vipNode);
        ret.setCorrect(vipNode.isCorrect());
        return ret;
    }

    private Node ConstDef(int level) {
        GUNIT curBlock = GUNIT.ConstDef;

        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.IDENFR);
        children.add(vipNode1);
        while (peek().getSymbol().equals(SYMBOL.LBRACK)) {
            Node tmp1 = checkSymbol(SYMBOL.LBRACK);
            Node tmp2 = ConstExp(level);

            Node tmp3 = checkSymbol(SYMBOL.RBRACK);
            if (!tmp3.isCorrect()) {
                logU4Error(Error.leftBraketMissingError(extractTokenFromLeafNode(tmp3).getLine()));
            }

            children.add(tmp1);
            children.add(tmp2);
            children.add(tmp3);
        }
        Node vipNode2 = checkSymbol(SYMBOL.ASSIGN);
        children.add(vipNode2);
        Node vipNode3 = ConstInitVal(level);
        children.add(vipNode3);
        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect());
        children.forEach(ret::addChild);
        return ret;
    }

    private Node ConstInitVal(int level) {
        GUNIT curBlock = GUNIT.ConstInitVal;

        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1;
        if (peek().getSymbol().equals(SYMBOL.LBRACE)) {
            vipNode1 = checkSymbol(SYMBOL.LBRACE);
            children.add(vipNode1);

            if (!peek().getSymbol().equals(SYMBOL.RBRACE)) {
                Node tmp1 = ConstInitVal(level);
                children.add(tmp1);
                while (peek().getSymbol().equals(SYMBOL.COMMA)) {
                    Node tmp2 = checkSymbol(SYMBOL.COMMA);
                    Node tmp3 = ConstInitVal(level);
                    children.add(tmp2);
                    children.add(tmp3);
                }

            }
            Node vipNode2 = checkSymbol(SYMBOL.RBRACE);
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());
            children.forEach(ret::addChild);
        } else {
            vipNode1 = ConstExp(level);
            ret.setCorrect(vipNode1.isCorrect());
            ret.addChild(vipNode1);
        }
        return ret;
    }

    private Node VarDecl(int level) {
        GUNIT curBlock = GUNIT.VarDecl;

        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = BType(level);
        children.add(vipNode1);
        Node vipNode2 = VarDef(level);
        children.add(vipNode2);

        while (peek().getSymbol().equals(SYMBOL.COMMA)) {
            Node tmp1 = checkSymbol(SYMBOL.COMMA);
            Node tmp2 = VarDef(level);
            children.add(tmp1);
            children.add(tmp2);
        }
        Node vipNode3 = checkSymbol(SYMBOL.SEMICN);
        if (!vipNode3.isCorrect()) {
            logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode3).getLine()));
        }
        children.add(vipNode3);

        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect());
        children.forEach(ret::addChild);
        return ret;
    }

    private Node VarDef(int level) {
        GUNIT curBlock = GUNIT.VarDef;

        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.IDENFR);
        children.add(vipNode1);

        while (peek().getSymbol().equals(SYMBOL.LBRACK)) {
            Node tmp1 = checkSymbol(SYMBOL.LBRACK);
            Node tmp2 = ConstExp(level);
            Node tmp3 = checkSymbol(SYMBOL.RBRACK);
            if (!tmp3.isCorrect()) {
                logU4Error(Error.leftBraketMissingError(extractTokenFromLeafNode(tmp3).getLine()));
            }
            children.add(tmp1);
            children.add(tmp2);
            children.add(tmp3);
        }

        if (peek().getSymbol().equals(SYMBOL.ASSIGN)) {
            Node tmp1 = checkSymbol(SYMBOL.ASSIGN);
            Node tmp2 = InitVal(level);
            children.add(tmp1);
            children.add(tmp2);
        }

        ret.setCorrect(vipNode1.isCorrect());
        children.forEach(ret::addChild);
        return ret;
    }

    private Node InitVal(int level) {
        GUNIT curBlock = GUNIT.InitVal;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        if (peek().getSymbol().equals(SYMBOL.LBRACE)) {
            Node vipNode1 = checkSymbol(SYMBOL.LBRACE);
            children.add(vipNode1);

            if (!peek().getSymbol().equals(SYMBOL.RBRACE)) {
                Node tmp1 = InitVal(level);
                children.add(tmp1);
                while (peek().getSymbol().equals(SYMBOL.COMMA)) {
                    Node tmp2 = checkSymbol(SYMBOL.COMMA);
                    Node tmp3 = InitVal(level);
                    children.add(tmp2);
                    children.add(tmp3);
                }
            }

            Node vipNode2 = checkSymbol(SYMBOL.RBRACE);
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());
        } else {
            Node vipNode1 = Exp(level);
            children.add(vipNode1);

            ret.setCorrect(vipNode1.isCorrect());
        }

        children.forEach(ret::addChild);
        return ret;
    }

    private Node FuncDef(int level) {
        GUNIT curBlock = GUNIT.FuncDef;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = FuncType(level);
        children.add(vipNode1);

        Node vipNode2 = checkSymbol(SYMBOL.IDENFR);
        children.add(vipNode2);

        Node vipNode3 = checkSymbol(SYMBOL.LPARENT);
        children.add(vipNode3);

        if (isNextFuncFParams()) {
            Node tmp1 = FuncFParams(level);
            children.add(tmp1);
        }

        Node vipNode4 = checkSymbol(SYMBOL.RPARENT);
        if (!vipNode4.isCorrect()) {
            logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
        }
        children.add(vipNode4);

        Node vipNode5 = Block(level + 1);
        children.add(vipNode5);

        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect() && vipNode5.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node MainFuncDef(int level) {
        GUNIT curBlock = GUNIT.MainFuncDef;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.INTTK);
        children.add(vipNode1);
        Node vipNode2 = checkSymbol(SYMBOL.MAINTK);
        children.add(vipNode2);
        Node vipNode3 = checkSymbol(SYMBOL.LPARENT);
        children.add(vipNode3);
        Node vipNode4 = checkSymbol(SYMBOL.RPARENT);
        if (!vipNode4.isCorrect()) {
            logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
        }
        children.add(vipNode4);
        Node vipNode5 = Block(level + 1);
        children.add(vipNode5);

        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect() && vipNode5.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node FuncType(int level) {
        GUNIT curBlock = GUNIT.FuncType;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        SYMBOL[] bolster = {SYMBOL.VOIDTK, SYMBOL.INTTK};

        Node vipNode1 = checkMultSymbol(bolster);
        children.add(vipNode1);

        if (vipNode1.isCorrect()) {
            ret.setCorrect(true);
        }

        children.forEach(ret::addChild);
        return ret;
    }

    private Node FuncFParams(int level) {
        GUNIT curBlock = GUNIT.FuncFParams;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = FuncFParam(level);
        children.add(vipNode1);

        while (peek().getSymbol().equals(SYMBOL.COMMA)) {
            Node tmp1 = checkSymbol(SYMBOL.COMMA);
            Node tmp2 = FuncFParam(level);
            children.add(tmp1);
            children.add(tmp2);
        }

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node FuncFParam(int level) {
        GUNIT curBlock = GUNIT.FuncFParam;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = BType(level);
        children.add(vipNode1);
        Node vipNode2 = checkSymbol(SYMBOL.IDENFR);
        children.add(vipNode2);

        if (peek().getSymbol().equals(SYMBOL.LBRACK)) {
            Node tmp1 = checkSymbol(SYMBOL.LBRACK);
            Node tmp2 = checkSymbol(SYMBOL.RBRACK);
            if (!tmp2.isCorrect()) {
                logU4Error(Error.leftBraketMissingError(extractTokenFromLeafNode(tmp2).getLine()));
            }
            children.add(tmp1);
            children.add(tmp2);

            while (peek().getSymbol().equals(SYMBOL.LBRACK)) {
                Node tmp3 = checkSymbol(SYMBOL.LBRACK);
                Node tmp4 = ConstExp(level);
                Node tmp5 = checkSymbol(SYMBOL.RBRACK);
                if (!tmp5.isCorrect()) {
                    logU4Error(Error.leftBraketMissingError(extractTokenFromLeafNode(tmp5).getLine()));
                }
                children.add(tmp3);
                children.add(tmp4);
                children.add(tmp5);
            }
        }

        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node Block(int level) {
        GUNIT curBlock = GUNIT.Block;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.LBRACE);
        children.add(vipNode1);

        while (!peek().getSymbol().equals(SYMBOL.RBRACE)) {
            Node tmp1 = BlockItem(level + 1);
            children.add(tmp1);
        }

        Node vipNode2 = checkSymbol(SYMBOL.RBRACE);
        children.add(vipNode2);

        ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node BlockItem(int level) {
        GUNIT curBlock = GUNIT.BlockItem;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1;
        if (peek().getSymbol().equals(SYMBOL.CONSTTK) || peek().getSymbol().equals(SYMBOL.INTTK)) {
            vipNode1 = Decl(level);
        } else {
            vipNode1 = Stmt(level);
        }
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node Stmt(int level) {
        GUNIT curBlock = GUNIT.Stmt;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        if (peek().getSymbol().equals(SYMBOL.IFTK)) {
            Node vipNode1 = checkSymbol(SYMBOL.IFTK);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.LPARENT);
            children.add(vipNode2);
            Node vipNode3 = Cond(level);
            children.add(vipNode3);
            Node vipNode4 = checkSymbol(SYMBOL.RPARENT);
            if (!vipNode4.isCorrect()) {
                logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
            }
            children.add(vipNode4);
            Node vipNode5 = Stmt(level);
            children.add(vipNode5);

            if (peek().getSymbol().equals(SYMBOL.ELSETK)) {
                Node tmp1 = checkSymbol(SYMBOL.ELSETK);
                Node tmp2 = Stmt(level);
                children.add(tmp1);
                children.add(tmp2);
            }

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect() && vipNode5.isCorrect());

        } else if (peek().getSymbol().equals(SYMBOL.WHILETK)) {
            Node vipNode1 = checkSymbol(SYMBOL.WHILETK);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.LPARENT);
            children.add(vipNode2);
            Node vipNode3 = Cond(level);
            children.add(vipNode3);
            Node vipNode4 = checkSymbol(SYMBOL.RPARENT);
            if (!vipNode4.isCorrect()) {
                logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
            }
            children.add(vipNode4);
            Node vipNode5 = Stmt(level);
            children.add(vipNode5);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect() && vipNode5.isCorrect());

        } else if (peek().getSymbol().equals(SYMBOL.BREAKTK)) {
            Node vipNode1 = checkSymbol(SYMBOL.BREAKTK);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.SEMICN);
            if (!vipNode2.isCorrect()) {
                logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode2).getLine()));
            }
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());
        } else if (peek().getSymbol().equals(SYMBOL.CONTINUETK)) {
            Node vipNode1 = checkSymbol(SYMBOL.CONTINUETK);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.SEMICN);
            if (!vipNode2.isCorrect()) {
                logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode2).getLine()));
            }
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());
        } else if (peek().getSymbol().equals(SYMBOL.RETURNTK)) {
            Node vipNode1 = checkSymbol(SYMBOL.RETURNTK);
            children.add(vipNode1);

            if (!peek().getSymbol().equals(SYMBOL.SEMICN)) {
                Node tmp1 = Exp(level);
                children.add(tmp1);
            }

            Node vipNode2 = checkSymbol(SYMBOL.SEMICN);
            if (!vipNode2.isCorrect()) {
                logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode2).getLine()));
            }
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());

        } else if (peek().getSymbol().equals(SYMBOL.PRINTFTK)) {
            Node vipNode1 = checkSymbol(SYMBOL.PRINTFTK);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.LPARENT);
            children.add(vipNode2);
            Node vipNode3 = checkSymbol(SYMBOL.STRCON);
            children.add(vipNode3);

            while (peek().getSymbol().equals(SYMBOL.COMMA)) {
                Node tmp1 = checkSymbol(SYMBOL.COMMA);
                Node tmp2 = Exp(level);
                children.add(tmp1);
                children.add(tmp2);
            }

            Node vipNode4 = checkSymbol(SYMBOL.RPARENT);
            if (!vipNode4.isCorrect()) {
                logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
            }
            children.add(vipNode4);
            Node vipNode5 = checkSymbol(SYMBOL.SEMICN);
            if (!vipNode5.isCorrect()) {
                logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode5).getLine()));
            }
            children.add(vipNode5);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect() && vipNode5.isCorrect());
        } else if (peek().getSymbol().equals(SYMBOL.LBRACE)) {
            Node vipNode1 = Block(level + 1);
            children.add(vipNode1);

            ret.setCorrect(vipNode1.isCorrect());
        } else if (isNextLValAndAssign()) {
            Node vipNode1 = LVal(level);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.ASSIGN);
            children.add(vipNode2);

            if (peek().getSymbol().equals(SYMBOL.GETINTTK)) {
                Node vipNode3 = checkSymbol(SYMBOL.GETINTTK);
                children.add(vipNode3);
                Node vipNode4 = checkSymbol(SYMBOL.LPARENT);
                children.add(vipNode4);

                Node vipNode5 = checkSymbol(SYMBOL.RPARENT);
                if (!vipNode5.isCorrect()) {
                    logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode5).getLine()));
                }
                children.add(vipNode5);

                Node vipNode6 = checkSymbol(SYMBOL.SEMICN);
                if (!vipNode6.isCorrect()) {
                    logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode6).getLine()));
                }
                children.add(vipNode6);

                ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect()
                        && vipNode4.isCorrect() && vipNode5.isCorrect() && vipNode6.isCorrect());
            } else {
                Node vipNode3 = Exp(level);
                children.add(vipNode3);
                Node vipNode4 = checkSymbol(SYMBOL.SEMICN);
                if (!vipNode4.isCorrect()) {
                    logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode4).getLine()));
                }
                children.add(vipNode4);

                ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect() && vipNode4.isCorrect());
            }


        } else {
            if (isNextExpAndSemicon()) {
                Node tmp1 = Exp(level);
                children.add(tmp1);

                Node vipNode1 = checkSymbol(SYMBOL.SEMICN);
                if (!vipNode1.isCorrect()) {
                    logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode1).getLine()));
                }
                children.add(vipNode1);

                ret.setCorrect(vipNode1.isCorrect());
            } else {
                Node vipNode1 = checkSymbol(SYMBOL.SEMICN);
                if (!vipNode1.isCorrect()) {
                    logU4Error(Error.semiconMissingError(extractTokenFromLeafNode(vipNode1).getLine()));
                }
                children.add(vipNode1);

                ret.setCorrect(vipNode1.isCorrect());
            }
        }

        children.forEach(ret::addChild);
        return ret;
    }

    private Node Exp(int level) {
        GUNIT curBlock = GUNIT.Exp;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = AddExp(level);
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node Cond(int level) {
        GUNIT curBlock = GUNIT.Cond;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = LOrExp(level);
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node LVal(int level) {
        GUNIT curBlock = GUNIT.LVal;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.IDENFR);
        children.add(vipNode1);

        while (peek().getSymbol().equals(SYMBOL.LBRACK)) {
            Node tmp1 = checkSymbol(SYMBOL.LBRACK);
            Node tmp2 = Exp(level);
            Node tmp3 = checkSymbol(SYMBOL.RBRACK);
            if (!tmp3.isCorrect()) {
                logU4Error(Error.leftBraketMissingError(extractTokenFromLeafNode(tmp3).getLine()));
            }
            children.add(tmp1);
            children.add(tmp2);
            children.add(tmp3);
        }

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node PrimaryExp(int level) {
        GUNIT curBlock = GUNIT.PrimaryExp;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        if (peek().getSymbol().equals(SYMBOL.INTCON)) {
            Node vipNode1 = Number(level);
            children.add(vipNode1);

            ret.setCorrect(vipNode1.isCorrect());
        } else if (peek().getSymbol().equals(SYMBOL.LPARENT)) {
            Node vipNode1 = checkSymbol(SYMBOL.LPARENT);
            children.add(vipNode1);
            Node vipNode2 = Exp(level);
            children.add(vipNode2);
            Node vipNode3 = checkSymbol(SYMBOL.RPARENT);
            if (!vipNode3.isCorrect()) {
                logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode3).getLine()));
            }
            children.add(vipNode3);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect());
        } else {
            Node vipNode1 = LVal(level);
            children.add(vipNode1);

            ret.setCorrect(vipNode1.isCorrect());
        }

        children.forEach(ret::addChild);
        return ret;
    }

    private Node Number(int level) {
        GUNIT curBlock = GUNIT.Number;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = checkSymbol(SYMBOL.INTCON);
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node UnaryExp(int level) {
        GUNIT curBlock = GUNIT.UnaryExp;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        if (peek().getSymbol().equals(SYMBOL.PLUS) ||
                peek().getSymbol().equals(SYMBOL.MINU) ||
                peek().getSymbol().equals(SYMBOL.NOT)) {
            Node vipNode1 = UnaryOp(level);
            children.add(vipNode1);
            Node vipNode2 = UnaryExp(level);
            children.add(vipNode2);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect());
        } else if (peek().getSymbol().equals(SYMBOL.IDENFR) && peek(2).getSymbol().equals(SYMBOL.LPARENT)) {
            Node vipNode1 = checkSymbol(SYMBOL.IDENFR);
            children.add(vipNode1);
            Node vipNode2 = checkSymbol(SYMBOL.LPARENT);
            children.add(vipNode2);
            if (isNextFuncRParams()) {
                Node tmp1 = FuncRParams(level);
                children.add(tmp1);
            }

            Node vipNode3 = checkSymbol(SYMBOL.RPARENT);
            if (!vipNode3.isCorrect()) {
                logU4Error(Error.leftParentMissingError(extractTokenFromLeafNode(vipNode3).getLine()));
            }

            children.add(vipNode3);

            ret.setCorrect(vipNode1.isCorrect() && vipNode2.isCorrect() && vipNode3.isCorrect());
        } else {
            Node vipNode1 = PrimaryExp(level);
            children.add(vipNode1);

            ret.setCorrect(vipNode1.isCorrect());
        }

        children.forEach(ret::addChild);
        return ret;
    }

    private Node UnaryOp(int level) {
        GUNIT curBlock = GUNIT.UnaryOp;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        SYMBOL[] bolster = {SYMBOL.PLUS, SYMBOL.MINU, SYMBOL.NOT};

        Node vipNode1 = checkMultSymbol(bolster);
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    private Node FuncRParams(int level) {
        GUNIT curBlock = GUNIT.FuncRParams;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = Exp(level);
        children.add(vipNode1);

        while (peek().getSymbol().equals(SYMBOL.COMMA)) {
            Node tmp1 = checkSymbol(SYMBOL.COMMA);
            Node tmp2 = Exp(level);
            children.add(tmp1);
            children.add(tmp2);
        }

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }

    /**
     * MulExp是一个左递归规则 MulExp -> UnaryExp | MulExp ('*'|'/'|'%') UnaryExp
     * 改写为 MulExp -> UnaryExp {('*'|'/'|'%') UnaryExp}
     *
     * @param level
     * @
     */
    private Node MulExp(int level) {
        GUNIT curBlock = GUNIT.MulExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = UnaryExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.MULT) ||
                peek().getSymbol().equals(SYMBOL.DIV) ||
                peek().getSymbol().equals(SYMBOL.MOD)) {

            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = pureMulOp(level);
            Node tmp2 = UnaryExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    /**
     * AddExp是一个左递归规则 AddExp -> MulExp | AddExp ('+'|'-') MulExp
     * 改写为AddExp -> MulExp {('+'|'-') MulExp}
     *
     * @param level
     * @
     */
    private Node AddExp(int level) {
        GUNIT curBlock = GUNIT.AddExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = MulExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.PLUS) ||
                peek().getSymbol().equals(SYMBOL.MINU)) {

            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = pureAddOp(level);
            Node tmp2 = MulExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    /**
     * RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp是左递归规则
     * 转变为 RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp}
     */
    private Node RelExp(int level) {
        GUNIT curBlock = GUNIT.RelExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = AddExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.LEQ) ||
                peek().getSymbol().equals(SYMBOL.LSS) ||
                peek().getSymbol().equals(SYMBOL.GEQ) ||
                peek().getSymbol().equals(SYMBOL.GRE)) {
            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = pureCompareOp(level);
            Node tmp2 = AddExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    /**
     * EqExp -> RelExp | EqExp ('==' | '!=') RelExp 左递归
     * 改为 EqExp -> RelExp {  ('==' | '!=') RelExp }
     */
    private Node EqExp(int level) {
        GUNIT curBlock = GUNIT.EqExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = RelExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.EQL) ||
                peek().getSymbol().equals(SYMBOL.NEQ)) {
            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = pureEquOp(level);
            Node tmp2 = RelExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    /**
     * LAndExp → EqExp | LAndExp '&&' EqExp 左递归
     * 改为 LAndExp → EqExp { '&&' EqExp }
     *
     * @param level
     * @
     */
    private Node LAndExp(int level) {
        GUNIT curBlock = GUNIT.LAndExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = EqExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.AND)) {
            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = checkSymbol(SYMBOL.AND);
            Node tmp2 = EqExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    /**
     * LOrExp → LAndExp | LOrExp '||' LAndExp 左递归
     * 改为 LOrExp -> LAndExp { '||' LAndExp }
     *
     * @param level
     * @
     */
    private Node LOrExp(int level) {
        GUNIT curBlock = GUNIT.LOrExp;

        BranchNode ret = new BranchNode(curBlock);

        Node vipNode1 = LAndExp(level);
        ret.addChild(vipNode1);
        ret.setCorrect(vipNode1.isCorrect());

        while (peek().getSymbol().equals(SYMBOL.OR)) {
            BranchNode tmpBranch = new BranchNode(curBlock);
            tmpBranch.addChild(ret);
            tmpBranch.setCorrect(ret.isCorrect());
            ret = tmpBranch;

            Node tmp1 = checkSymbol(SYMBOL.OR);
            Node tmp2 = LAndExp(level);

            ret.addChild(tmp1);
            ret.addChild(tmp2);
        }

        return ret;
    }

    private Node ConstExp(int level) {
        GUNIT curBlock = GUNIT.ConstExp;
        BranchNode ret = new BranchNode(curBlock);
        ArrayList<Node> children = new ArrayList<>();

        Node vipNode1 = AddExp(level);
        children.add(vipNode1);

        ret.setCorrect(vipNode1.isCorrect());

        children.forEach(ret::addChild);
        return ret;
    }
}
