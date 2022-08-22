package improve.component;

import component.Label;
import component.graph.Graph;
import global.Config;
import global.Error;
import global.Logger;
import imcode.imexp.*;
import imcode.imitem.IMItem;
import imcode.imitem.LabelItem;
import imcode.imitem.VarItem;
import improve.component.optimizers.ConstOptimizer;
import improve.component.optimizers.GlobalExpOptimizer;
import improve.component.optimizers.SeekOptimizer;
import improve.component.regpool.RegPool;
import mips.register.Register;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;
import symbolstruct.Region;
import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据流图
 * 每个数据流图对应一个函数Region
 */
public class FlowGraph {
    private ArrayList<BasicBlock> blocks;
    private ArrayList<IMExp> imExps;
    private HashMap<IMExp, BasicBlock> exp2block;
    public Region belong;

    private HashMap<IMExp, HashSet<IMExp>> expArriveIn;
    private HashMap<IMExp, HashSet<IMExp>> expArriveOut;
    private HashMap<IMExp, HashSet<IMExp>> expArriveGen;
    private HashMap<IMExp, HashSet<IMExp>> expArriveKill;

    private HashMap<IMExp, HashSet<Entry>> expActiveIn;
    private HashMap<IMExp, HashSet<Entry>> expActiveOut;
    private HashMap<IMExp, HashSet<Entry>> expActiveUse;
    private HashMap<IMExp, HashSet<Entry>> expActiveDef;

    public FlowGraph(Region funcRegion) {
        this.belong = funcRegion;
        this.imExps = funcRegion.imexps;
        this.expArriveOut = new HashMap<>();
        this.expArriveIn = new HashMap<>();
        this.expArriveGen = new HashMap<>();
        this.expArriveKill = new HashMap<>();

        this.expActiveIn = new HashMap<>();
        this.expActiveOut = new HashMap<>();
        this.expActiveUse = new HashMap<>();
        this.expActiveDef = new HashMap<>();

        genFlow();

        /* 开始全局优化 */

        int preSize = 0;
        while (preSize != this.imExps.size()) {
            preSize = this.imExps.size();

            // 局部常量传播
            ConstOptimizer constOptimizer = new ConstOptimizer(this.imExps, this.blocks, this.exp2block);
            this.imExps = constOptimizer.dump();
            genFlow();

            // 全局公共表达式消解
            GlobalExpOptimizer globalExpOptimizer = new GlobalExpOptimizer(this.imExps, this.blocks, this.exp2block, this.belong);
            this.imExps = globalExpOptimizer.dump();
            genFlow();

            // 全局复制传播
            SeekOptimizer seekOptimizer = new SeekOptimizer(this.imExps, this.blocks, this.exp2block, this.expArriveIn);
            this.imExps = seekOptimizer.dump();
            genFlow();
        }

        // 开始窥孔优化
        for (int i = 0; i < this.blocks.size(); i++) {
            BasicBlock blk = this.blocks.get(i);

            for (int k = 0; k < blk.imexps.size(); k++) {
                IMExp exp = blk.imexps.get(k);
                IMExp preExp = (k == 0) ? null : blk.imexps.get(k - 1);
                IMExp nextExp = (k == blk.imexps.size() - 1) ? null : blk.imexps.get(k + 1);

                // 如果后一条语句使用了前一条语句的计算结果进行赋值，那就直接使用;这里需要特殊处理函数变量引入定义，因为import不能直接替换
                if (preExp != null
                        && preExp.item1 instanceof VarItem
                        && !(preExp instanceof ParaDefExp)
                        && exp instanceof AssignByVarExp) {
                    Entry assigned = ((VarItem) preExp.item1).entry;
                    Entry value = ((VarItem) exp.item2).entry;

                    // 当变量assigned不是全局变量，也不从该基本块出去时
                    if (assigned.equals(value)
                            && !blk.activeOut.contains(assigned)
                            && !assigned.isGlobal()) {

                        // 还需要保证其在该基本块内只有exp一处引用了它
                        Boolean referenced = false;
                        for (int g = k + 1; g < blk.imexps.size(); g++) {
                            IMExp refExp = blk.imexps.get(g);
                            IMItem item2 = refExp.item2;
                            IMItem item3 = refExp.item3;
                            if ((item2 instanceof VarItem && assigned.equals(((VarItem) item2).entry))
                                    || (item3 instanceof VarItem && assigned.equals(((VarItem) item3).entry))) {
                                referenced = true;
                                break;
                            }
                        }

                        if (!referenced) {
                            preExp.item1 = exp.item1;
                            blk.imexps.remove(k);
                            k--;
                            continue;
                        }
                    }
                }

                // 优化条件跳转语句
                if (exp instanceof ConNotJumpExp || exp instanceof ConJumpExp) {
                    if (preExp instanceof BgtExp || preExp instanceof BneExp
                            || preExp instanceof BeqExp || preExp instanceof BgeExp) {
                        Entry cond = ((VarItem) preExp.item1).entry;
                        Entry value = ((VarItem) exp.item2).entry;
                        if (cond.equals(value) && !blk.activeOut.contains(cond)) {
                            IMExp optExp = new OPTBranchExp(preExp, exp);
                            blk.imexps.set(k, optExp);
                            blk.imexps.remove(k - 1);
                            k--;
                            continue;
                        }
                    }
                }

                //如果一个语句的出口活跃变量没有其定义的变量 && 该变量不是全局变量 && 该语句没有副作用，那么这条语句是死代码
                if (exp.item1 instanceof VarItem
                        && !((VarItem) exp.item1).entry.isGlobal()
                        && !expActiveOut.get(exp).contains(((VarItem) exp.item1).entry)
                        && !(exp instanceof AssignByGetInt)) {
                    blk.imexps.remove(k);
                    k--;
                    continue;
                }
            }
        }

        // 由于窥孔优化是在基本块内做的，所以需要同步到总表达式序列中，并重新生成数据流信息以供生成目标代码时寄存器分配使用
        this.imExps = new ArrayList<>();
        for (BasicBlock blk : this.blocks) {
            this.imExps.addAll(blk.imexps);
        }
        genFlow();

        // 去除多余while循环
        for (int i = 0; i < this.imExps.size(); i++) {
            IMExp exp = this.imExps.get(i);

            // 寻找循环的开始
            if (exp instanceof LabelExp
                    && ((LabelItem) exp.item1).labelName.labelName.contains("_WHILE_BEGIN")) {
                Label label = ((LabelItem) exp.item1).labelName;
                String labelStr = label.labelName;
                String mark = labelStr.replace("_WHILE_BEGIN", "");

                // 找到对应的循环结构的开始和结束位置
                int begin = i;
                int end = i + 1;
                for (; end < this.imExps.size(); end++) {
                    IMExp endExp = this.imExps.get(end);
                    if (endExp instanceof LabelExp
                            && ((LabelItem) endExp.item1).labelName.labelName.equals(mark + "_WHILE_END")) {
                        break;
                    }
                }

                // 获取循环结构的数据流信息
                IMExp endExp = this.imExps.get(end);
                BasicBlock endBlock = this.exp2block.get(endExp);
                HashSet<Entry> whileActiveOut = endBlock.activeIn;

                Boolean removable = true;
                for (int k = begin + 1; k < end; k++) {
                    IMExp toCheck = this.imExps.get(k);
                    // 如果循环结构中出现副作用语句，则不可删除
                    if (toCheck instanceof AssignByGetInt
                            || toCheck instanceof PrintVarExp
                            || toCheck instanceof CallExp
                            || toCheck instanceof ReturnExp
                            || toCheck instanceof PushParaExp
                            || toCheck instanceof ParaDefExp
                            || toCheck instanceof PrintStrExp
                            || toCheck instanceof AssignToAddrExp) {
                        removable = false;
                        break;
                    }

                    // 如果是赋值语句，那么赋值语句的赋值对象如果：
                    // 1. 是全局变量
                    // 2. 属于循环结构的activeOut集合
                    // 那么不可删除
                    if (toCheck.item1 instanceof VarItem) {
                        Entry entry = ((VarItem) toCheck.item1).entry;
                        if (whileActiveOut.contains(entry)) {
                            removable = false;
                            break;
                        } else if (entry.isGlobal()) {
                            if (belong.name.equals("main")) {
                                Boolean canRemove = true;
                                for (int g = end; g < this.imExps.size(); g++) {
                                    IMExp laterExp = this.imExps.get(g);
                                    if (laterExp instanceof CallExp) {
                                        canRemove = false;
                                        break;
                                    }
                                    if (laterExp.item1 instanceof VarItem) {
                                        Entry laterEntry = ((VarItem) laterExp.item1).entry;
                                        if(laterEntry.equals(entry)){
                                            canRemove=false;
                                            break;
                                        }
                                    }
                                }
                                if(!canRemove){
                                    removable = false;
                                    break;
                                }
                            } else {
                                removable = false;
                                break;
                            }
                        }
                    }
                }

                if (removable) {
                    // 从后往前删，以保证删除后下一个需要删除的表达式的序号不变
                    for (int k = end; k >= begin; k--) {
                        this.imExps.remove(k);
                    }
                    genFlow(); // 删除循环后重新生成数据流图
                    i--;  // 由于exp被删除了，因此i需要回退一个单位
                }
            }
        }

        ConstOptimizer constOptimizer = new ConstOptimizer(this.imExps, this.blocks, this.exp2block);
        this.imExps = constOptimizer.dump();
        genFlow();

        for (BasicBlock blk : this.blocks) {
            Logger.GetLogger().IMExpLogPath("// " + blk.name + " begins //");
            for (IMExp exp : blk.imexps) {
                if (!(exp instanceof LabelExp)) {
                    Logger.GetLogger().IMExpLogPath("\t" + exp.toString());
                } else {
                    Logger.GetLogger().IMExpLogPath(exp.toString());
                }
            }
            Logger.GetLogger().IMExpLogPath("// " + blk.name + " ends //");
        }

        dumpToCodeText();
    }

    private void genFlow() {
        this.blocks = new ArrayList<>();
        this.exp2block = new HashMap<>();
        genBasicBlocks();  // 初步建立基本块实体完成
        flowBasicBlocks(); // 构建进行基本块之间的联系
        genFlowData();  // 进行数据流分析，分析数据会放在BasicBlock内
    }

    /**
     * 将流图中的代码导出为MIPS代码，并写入CodeText对象
     */
    public void dumpToCodeText() {

        if (this.belong instanceof FuncRegion) {
            CodeText.textNLine("# Function starts here " + belong.name);
            CodeText.textNLine(belong.name + ":");

            CodeText.textNLine(String.format("addiu $sp, $sp, -%d", belong.frame.size));
            CodeText.textNLine(String.format("sw $ra, %d($sp)", belong.frame.offsetRA));
            // 函数块：开辟栈帧空间，并且将$ra存入指定位置
        } else {
            for (Entry e : belong.getEntries()) {
                Integer spaceSize = ((AbsVarEntry) e).size;
                CodeText.dataNLine(String.format("%s: .space %d", e.name, spaceSize * 4));
            }
            // 全局块：在data区内声明全局变量的空间
        }

        RegPool pool = new RegPool(this.belong, genGlbRegMap());

        // 刚进入函数，将全局寄存器对应的值load到全局寄存器中
        pool.glbPool.init();
        for (BasicBlock curBlock : this.blocks) {
            // 每个BasicBlock开始，需要将临时寄存器对应的值load进行临时寄存器中
            pool.locPool.init(curBlock);
            for (int j = 0; j < curBlock.imexps.size(); j++) {
                IMExp exp = curBlock.imexps.get(j);

                if (exp.toString().equals("flag = getint()")) {
                    int a = 10;
                    int b = a;
                }

                // 在Call前，由于要离开函数后返回，均需要存好全局变量和活跃变量
                if (exp instanceof CallExp) {
                    pool.glbPool.saveActive(expActiveIn.get(exp));
                }

                // 在Return之前，需要存好全局变量
                if (exp instanceof ReturnExp) {
                    pool.glbPool.save();
                }

                // 对于跳转指令，需要将临时寄存器中的值先存储下来再跳转
                if (exp instanceof JumpExp || exp instanceof ConJumpExp || exp instanceof OPTBranchExp ||
                        exp instanceof ConNotJumpExp || exp instanceof CallExp || exp instanceof ReturnExp) {

                    if (j == curBlock.imexps.size() - 1) {
                        pool.locPool.save(curBlock);
                    }
                    CodeText.textNLine("\n# " + exp);
                    exp.toCode(pool);
                }

                // 对于其他指令（包括赋值语句），需要在语句执行后将临时寄存器值存储下来
                else {
                    CodeText.textNLine("\n# " + exp);
                    exp.toCode(pool);
                    if (j == curBlock.imexps.size() - 1) {
                        pool.locPool.save(curBlock);
                    }
                }

                // 从Call返回，则需要恢复活跃变量对应寄存器中的值
                if (exp instanceof CallExp) {
                    pool.glbPool.loadActive(expActiveIn.get(exp));
                }
            }
        }
        // 退出函数，保存所有全局变量
        pool.glbPool.save();

        if (belong instanceof FuncRegion) {
            // 函数块：恢复函数栈帧并返回指定地址
            CodeText.textNLine("# final return");
            CodeText.textNLine(String.format("lw $ra, %d($sp)", belong.frame.offsetRA));
            CodeText.textNLine(String.format("addiu $sp, $sp, %d", belong.frame.size));
            CodeText.textNLine(String.format("jr $ra"));
            CodeText.textNLine("# Function ends here " + belong.name);
        }
    }

    private HashMap<Entry, Register> genGlbRegMap() {
        // 引用计数，获取所有Entry和相应的计数
        HashMap<Entry, Integer> counter = getAllRelatedEntryAndCount();

        // 剔除ConstValueEntry后，排序
        List<Map.Entry<Entry, Integer>> list = new ArrayList<>();
        list.addAll(counter.entrySet());
        list.removeIf(entryIntegerEntry -> entryIntegerEntry.getKey() instanceof ConstValueEntry);


        list.sort((o1, o2) -> o1.getValue() - o2.getValue());
        Graph graph = new Graph(list);
        for (BasicBlock block : this.blocks) {
            for (IMExp exp : block.imexps) {
                if (expActiveOut.get(exp) == null || expActiveOut.get(exp).size() == 0) {
                    continue;
                }
                // 过滤掉全局变量，不分配全局寄存器。因为分配后寄存器的值不一定会被及时保存。
                List<Entry> activeIn = expActiveOut.get(exp).stream()
                        .filter(e -> !e.isGlobal()).collect(Collectors.toList());
                for (int i = 0; i < activeIn.size(); i++) {
                    graph.addNode(activeIn.get(i));
                    for (int k = 0; k < i; k++) {
                        graph.addEdge(activeIn.get(i), activeIn.get(k));
                    }
                }
            }
        }

        graph.executeDistribution();
        return graph.dump();
//        // 引用计数
//        list.sort((o1, o2) -> o2.getValue() - o1.getValue());
//        HashMap<Entry, Register> ret = new HashMap<>();
//        int i = 0;
//        for (Register reg : Config.getGlbReg()) {
//            if (i >= list.size()) {
//                break;
//            }
//            ret.put(list.get(i).getKey(), reg);
//            i++;
//        }
//
//        return ret;
    }

    private HashMap<Entry, Integer> getAllRelatedEntryAndCount() {
        HashMap<Entry, Integer> ret = new HashMap<>();
        for (IMExp exp : this.imExps) {
            if ((exp.item1 instanceof VarItem)) {
                if (!ret.containsKey(((VarItem) exp.item1).entry)) {
                    ret.put(((VarItem) exp.item1).entry, 1);
                } else {
                    ret.put(((VarItem) exp.item1).entry, ret.get(((VarItem) exp.item1).entry) + 1);
                }
            }

            if ((exp.item2 instanceof VarItem)) {
                if (!ret.containsKey(((VarItem) exp.item2).entry)) {
                    ret.put(((VarItem) exp.item2).entry, 1);
                } else {
                    ret.put(((VarItem) exp.item2).entry, ret.get(((VarItem) exp.item2).entry) + 1);
                }
            }

            if ((exp.item3 instanceof VarItem)) {
                if (!ret.containsKey(((VarItem) exp.item3).entry)) {
                    ret.put(((VarItem) exp.item3).entry, 1);
                } else {
                    ret.put(((VarItem) exp.item3).entry, ret.get(((VarItem) exp.item3).entry) + 1);
                }
            }
        }
        return ret;
    }

    private void genFlowData() {
        // 活跃变量分析
        for (BasicBlock block : blocks) {
            for (IMExp exp : block.imexps) {
                expActiveDef.put(exp, new HashSet<>());
                expActiveUse.put(exp, new HashSet<>());

                // 一定先检查并添加Use集合，因为对于一条语句而言，Use永远先于Def

                if (exp.item2 != null && (exp.item2 instanceof VarItem)) {
                    Entry tmp = ((VarItem) exp.item2).entry;
                    if (!block.activeDef.contains(tmp) && !(tmp instanceof ConstValueEntry)) block.activeUse.add(tmp);
                    if (!expActiveDef.get(exp).contains(tmp) && !(tmp instanceof ConstValueEntry))
                        expActiveUse.get(exp).add(tmp);
                }

                if (exp.item3 != null && (exp.item3 instanceof VarItem)) {
                    Entry tmp = ((VarItem) exp.item3).entry;
                    if (!block.activeDef.contains(tmp) && !(tmp instanceof ConstValueEntry)) block.activeUse.add(tmp);
                    if (!expActiveDef.get(exp).contains(tmp) && !(tmp instanceof ConstValueEntry))
                        expActiveUse.get(exp).add(tmp);
                }

                if (exp.item1 != null && (exp.item1 instanceof VarItem)) {
                    Entry tmp = ((VarItem) exp.item1).entry;
                    if (!block.activeUse.contains(tmp) && !(tmp instanceof ConstValueEntry)) block.activeDef.add(tmp);
                    if (!expActiveUse.get(exp).contains(tmp) && !(tmp instanceof ConstValueEntry))
                        expActiveDef.get(exp).add(tmp);
                }
            }
        }

        // 活跃变量分析迭代
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Queue<BasicBlock> queue = new LinkedList<>();
            queue.add(this.blocks.get(i));
            while (queue.size() > 0) {
                BasicBlock head = queue.peek();
                queue.poll();
                HashSet<Entry> temp2 = union(head.activeUse, diff(head.activeOut, head.activeDef));

                if (!setCmp(head.activeIn, temp2)) {
                    HashSet<Entry> check1 = diff(head.activeIn, temp2);
                    if (check1.size() > 0) {
                        Error.warning("BasicBlock.activeIn is Shrinking, which is not expected");
                    }
                    head.activeIn = temp2;
                    for (BasicBlock pre : head.pre) {
                        HashSet<Entry> check2 = union(head.activeIn, pre.activeOut);
                        if (!setCmp(pre.activeOut, check2)) {
                            queue.add(pre);
                            pre.activeOut = check2;
                        }
                    }
                }
            }
        }

        // 活跃变量分析，为每条语句获得入口出口活跃变量信息
        for (BasicBlock block : this.blocks) {
            for (int i = block.imexps.size() - 1; i >= 0; i--) {
                IMExp exp = block.imexps.get(i);
                if (i == block.imexps.size() - 1) {
                    expActiveOut.put(exp, block.activeOut);
                } else {
                    IMExp nextExp = block.imexps.get(i + 1);
                    expActiveOut.put(exp, expActiveIn.get(nextExp));
                }
                expActiveIn.put(exp, union(expActiveUse.get(exp), diff(expActiveOut.get(exp), expActiveDef.get(exp))));

                if (exp.toString().equals("_TEMPnym42 = 4")) {
                    IMExp nextExp = block.imexps.get(i + 1);
                    HashSet<Entry> nextActiveIn = expActiveIn.get(nextExp);
                    HashSet<Entry> activeUse = expActiveUse.get(exp);
                    HashSet<Entry> activeDef = expActiveDef.get(exp);
                    HashSet<Entry> activeIn = expActiveIn.get(exp);
                    HashSet<Entry> activeOut = expActiveOut.get(exp);
                    int a = 10;
                    int b = a;
                }
            }
        }

        // 到达定义分析，对每条语句
        for (IMExp exp : this.imExps) {
            expArriveGen.put(exp, new HashSet<>());
            expArriveKill.put(exp, new HashSet<>());
            if (!(exp.item1 instanceof VarItem)) {
                continue;   // 非赋值类语句直接跳过, 其gen和kill均为空
            }

            Entry define = ((VarItem) exp.item1).entry;

            if (define.name.contains("_TEMPnym")) {
                continue;
            }
            // #TRADEOFF 为了性能，只对非临时变量进行到达定义分析，这使得我们必须在删除时仅删除对非临时变量赋值对语句
            // 因为临时变量的赋值情况我们并不知道，因此即使优化器发现其没有被引用，也不代表其确实没被引用

            this.expArriveGen.get(exp).add(exp);    // 每条语句的gen集合就是自己本身
            for (IMExp otherExp : this.imExps) {
                if (otherExp.equals(exp)) {
                    continue;   // 自己本身不算冲突
                }
                if (!(otherExp.item1 instanceof VarItem)) {
                    continue;   // 非赋值类语句直接跳过
                }
                Entry otherDefine = ((VarItem) otherExp.item1).entry;
                if (otherDefine.equals(define)) {
                    // 如果定义的对象一致，则加入kill集合
                    this.expArriveKill.get(exp).add(otherExp);
                }
            }
        }

        //到达定义分析，对每个基本块的gen和kill进行初始化
        for (BasicBlock block : this.blocks) {
            HashSet<IMExp> killExps = new HashSet<>();
            for (IMExp exp : block.imexps) {
                killExps = union(killExps, expArriveKill.get(exp));
            }
            block.arriveKill = killExps;

            HashSet<IMExp> genExps = new HashSet<>();
            for (int i = block.imexps.size() - 1; i >= 0; i--) {
                HashSet<IMExp> part = new HashSet<>();
                part.addAll(expArriveGen.get(block.imexps.get(i)));
                for (int k = i + 1; k < block.imexps.size(); k++) {
                    part = diff(part, expArriveKill.get(block.imexps.get(k)));
                }
                genExps = union(genExps, part);
            }

            block.arriveGen = genExps;
        }

        // 到达定义分析，迭代计算每个基本块的gen和kill集合
        for (BasicBlock block : this.blocks) {
            block.arriveOut = new HashSet<>();
        }
        while (true) {
            Boolean changed = false;
            for (BasicBlock block : this.blocks) {
                ArrayList<BasicBlock> preBlocks = block.pre;
                block.arriveIn = new HashSet<>();
                for (BasicBlock preBlock : preBlocks) {
                    block.arriveIn = union(block.arriveIn, preBlock.arriveOut);
                }
                HashSet<IMExp> tmp = union(block.arriveGen, diff(block.arriveIn, block.arriveKill));
                if (!setCmp(tmp, block.arriveOut)) {
                    block.arriveOut = tmp;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }

        // 到达定义分析，为每条语句获得UD链信息
        for (BasicBlock block : this.blocks) {
            for (int i = 0; i < block.imexps.size(); i++) {
                IMExp exp = block.imexps.get(i);
                if (i == 0) {
                    expArriveIn.put(exp, block.arriveIn);
                } else {
                    IMExp preExp = block.imexps.get(i - 1);
                    expArriveIn.put(exp, expArriveOut.get(preExp));
                }
                expArriveOut.put(exp, union(expArriveGen.get(exp), diff(expArriveIn.get(exp), expArriveKill.get(exp))));
            }
        }

        // 可用表达式分析，获取所有可用表达式
        HashSet<ValidExp> validExps = new HashSet<>();
        for (IMExp exp : this.imExps) {
            if (exp instanceof AddExp ||
                    exp instanceof SubExp ||
                    exp instanceof MulExp ||
                    exp instanceof DivExp ||
                    exp instanceof ModExp) {
                validExps.add(new ValidExp(exp));
            }
        }

        // 可用表达式分析，生成e_gen和e_kill
        for (BasicBlock block : this.blocks) {
            for (int i = 0; i < block.imexps.size(); i++) {
                IMExp exp = block.imexps.get(i);
                if (!(exp.item1 instanceof VarItem)) {
                    continue;
                }
                Entry assign = ((VarItem) exp.item1).entry; // block块中这条语句为assign这个对象进行了赋值
                for (ValidExp validExp : validExps) {
                    // 找到含有assign的可用表达式validExp
                    if (validExp.in(assign)) {
                        // 检查validExp是否没有在block块中该语句后被重新计算
                        Boolean calculatedAgain = false;
                        for (int k = i + 1; k < block.imexps.size(); k++) {
                            IMExp otherExp = block.imexps.get(k);
                            if (otherExp instanceof AddExp ||
                                    otherExp instanceof SubExp ||
                                    otherExp instanceof MulExp ||
                                    otherExp instanceof DivExp ||
                                    otherExp instanceof ModExp) {
                                ValidExp otherValidExp = new ValidExp(otherExp);
                                if (otherValidExp.equals(validExp)) {
                                    // 重新计算了
                                    calculatedAgain = true;
                                    break;
                                }
                            }
                        }
                        if (!calculatedAgain) {
                            // 如果没有重新计算，那么加入e_kill
                            block.validKill.add(validExp);
                        }
                    }
                }

                if (!(exp instanceof AddExp ||
                        exp instanceof SubExp ||
                        exp instanceof MulExp ||
                        exp instanceof DivExp ||
                        exp instanceof ModExp)) {
                    continue;   // 如果该赋值语句不是一个计算语句，比如AssignByGetInt之类的，那么它仅具有注销能力，而不能计入可用表达式
                }

                ValidExp toAddGen = new ValidExp(exp);
                // 检查该可用表达式的参与对象有没有在block块中这条语句之后被改变
                Boolean assignAgain = false;
                for (int k = i + 1; k < block.imexps.size(); k++) {
                    IMExp otherExp = block.imexps.get(k);
                    if (!(otherExp.item1 instanceof VarItem)) {
                        continue;   // 如果不是赋值语句就跳过
                    }
                    Entry reassign = ((VarItem) otherExp.item1).entry;
                    if (toAddGen.in(reassign)) {
                        assignAgain = true;
                        break;
                    }
                }
                if (!assignAgain) {
                    // 如果没有被改动，那么加入e_gen
                    block.validGen.add(toAddGen);
                }
            }
        }

        // 第一个基本块的in和out永不改变
        blocks.get(0).validIn = new HashSet<>();
        blocks.get(0).validOut = blocks.get(0).validGen;
        for (int i = 1; i < blocks.size(); i++) {
            blocks.get(i).validOut = diff(validExps, blocks.get(i).validKill);
        }
        Boolean change = true;
        while (change) {
            change = false;
            for (int i = 1; i < blocks.size(); i++) {
                BasicBlock curBlock = blocks.get(i);
                curBlock.validIn = validExps;
                for (BasicBlock preBlock : curBlock.pre) {
                    curBlock.validIn = cross(curBlock.validIn, preBlock.validOut);
                }

                HashSet<ValidExp> tmp = union(curBlock.validGen, diff(curBlock.validIn, curBlock.validKill));
                if (!setCmp(tmp, curBlock.validOut)) {
                    change = true;
                    curBlock.validOut = tmp;
                }
            }
        }


        // 赋值表达式分析，获取所有赋值表达式
        HashSet<AssignByVarExp> assignExps = new HashSet<>();
        for (IMExp exp : this.imExps) {
            if (exp instanceof AssignByVarExp) {
                assignExps.add((AssignByVarExp) exp);
            }
        }

        // 赋值表达式分析，生成e_gen和e_kill
        for (BasicBlock block : this.blocks) {
            for (int i = 0; i < block.imexps.size(); i++) {
                IMExp exp = block.imexps.get(i);
                if (!(exp instanceof AssignByVarExp)) {
                    // 如果不是赋值语句就跳过
                    continue;
                }
                // 检查s: x:=y中y是否在s后被赋值
                Boolean valid = true;
                for (int k = i + 1; k < block.imexps.size(); k++) {
                    IMExp laterExp = block.imexps.get(k);
                    if (!(laterExp.item1 instanceof VarItem)) {
                        continue;
                    }
                    Entry assigned = ((VarItem) laterExp.item1).entry;
                    if (exp.item2.equals(assigned)) {
                        //  y在后续被赋值，那么无效
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    block.assignGen.add((AssignByVarExp) exp);
                }

                for (AssignByVarExp assignExp : assignExps) {
                    // 对所有可能的赋值语句s
                    if (block.imexps.contains(assignExp)) {
                        // 如果s在该语句块内，则跳过
                        continue;
                    }
                    // 如果s不在该语句块block中，检查其x或y是否被赋值过
                    for (int k = 0; k < block.imexps.size(); k++) {
                        IMExp laterExp = block.imexps.get(k);
                        if (!(laterExp.item1 instanceof VarItem)) {
                            continue;
                        }
                        Entry assigned = ((VarItem) laterExp.item1).entry;
                        if (((VarItem) assignExp.item1).entry.equals(assigned) ||
                                ((VarItem) assignExp.item2).entry.equals(assigned)) {
                            // 如果x或者y被赋值过，则加入c_kill
                            block.assignKill.add(assignExp);
                        }
                    }
                }
            }
        }

        blocks.get(0).assignIn = new HashSet<>();
        blocks.get(0).assignOut = blocks.get(0).assignGen;
        for (int i = 1; i < blocks.size(); i++) {
            blocks.get(i).assignOut = diff(assignExps, blocks.get(i).assignKill);
        }
        Boolean assignAnalyzeChanged = true;
        while (assignAnalyzeChanged) {
            assignAnalyzeChanged = false;
            for (int i = 1; i < blocks.size(); i++) {
                BasicBlock curBlock = blocks.get(i);
                curBlock.assignIn = assignExps;
                for (BasicBlock preBlock : curBlock.pre) {
                    curBlock.assignIn = cross(curBlock.assignIn, preBlock.assignOut);
                }

                HashSet<AssignByVarExp> tmp = union(curBlock.assignGen, diff(curBlock.assignIn, curBlock.assignKill));
                if (!setCmp(tmp, curBlock.assignOut)) {
                    assignAnalyzeChanged = true;
                    curBlock.assignOut = tmp;
                }
            }
        }
    }

    private void genBasicBlocks() {
        ArrayList<IMExp> buf = new ArrayList<>();   // 缓存区
        for (int i = 0; i < this.imExps.size(); i++) {
            if (isEnterExp(i) && !buf.isEmpty()) {
                BasicBlock toCreate = new BasicBlock(buf, this, "BK_" + Config.getTmpNameSed());
                this.blocks.add(toCreate);
                for (IMExp exp : buf) {
                    this.exp2block.put(exp, toCreate);
                }
                buf = new ArrayList<>();
            }

            buf.add(this.imExps.get(i));
        }

        BasicBlock toCreate = new BasicBlock(buf, this, "BK_" + Config.getTmpNameSed());
        this.blocks.add(toCreate);
        for (IMExp exp : buf) {
            this.exp2block.put(exp, toCreate);
        }
    }

    private void flowBasicBlocks() {
        for (int i = 0; i < this.blocks.size(); i++) {
            BasicBlock block = this.blocks.get(i);
            IMExp lastExp = block.getLastExp();
            if (lastExp instanceof ConJumpExp || lastExp instanceof ConNotJumpExp || lastExp instanceof OPTBranchExp) {
                Label toLabel = ((LabelItem) lastExp.item1).labelName;
                BasicBlock toBlock = findLabel(toLabel);
                if (i != this.blocks.size() - 1) {
                    buildFlow(block, this.blocks.get(i + 1));
                }
                buildFlow(block, toBlock);
            } else if (lastExp instanceof JumpExp) {
                Label toLabel = ((LabelItem) lastExp.item1).labelName;
                BasicBlock toBlock = findLabel(toLabel);
                buildFlow(block, toBlock);
            } else if (lastExp instanceof CallExp) {
                //  函数调用是跳转到当前函数之外的基本块中，也就是不属于当前流图
                //  由于函数终究会返回，因此我们直接让其和下一个基本块连接
                if (i != this.blocks.size() - 1) {
                    buildFlow(block, this.blocks.get(i + 1));
                }
            } else if (lastExp instanceof ReturnExp) {
                // 同样，由于函数返回是跳转到当前函数之外的基本块中，也就是不属于当前流图
                // 但是返回就是结束，没有后继基本块
            } else {
                if (i != this.blocks.size() - 1) {
                    buildFlow(block, this.blocks.get(i + 1));
                }
            }
        }
    }

    private Boolean isEnterExp(Integer index) {
        if (index == 0) {
            return true;    // 如果是第一条语句
        }
        IMExp curExp = this.imExps.get(index);
        IMExp preExp = this.imExps.get(index - 1);
        if (isTransExp(preExp)) return true;    //  如果是跳转语句的后一句
        if (curExp instanceof LabelExp) return true; // 如果是标签语句
        return false;
    }

    private Boolean isTransExp(IMExp tocheck) {
        return tocheck instanceof JumpExp ||
                tocheck instanceof ConJumpExp ||
                tocheck instanceof ConNotJumpExp ||
                tocheck instanceof ReturnExp ||
                tocheck instanceof CallExp ||
                tocheck instanceof OPTBranchExp;
    }

    private void buildFlow(BasicBlock from, BasicBlock to) {
        from.post.add(to);
        to.pre.add(from);
    }

    private BasicBlock findLabel(Label label) {
        for (BasicBlock block : this.blocks) {
            for (IMExp exp : block.dump()) {
                if (exp instanceof LabelExp && ((LabelItem) exp.item1).labelName.equals(label)) {
                    return block;
                }
            }
        }
        return null;
    }

    private static <T> HashSet<T> diff(HashSet<T> a, HashSet<T> b) {
        HashSet<T> ret = new HashSet<>();
        ret.addAll(a);
        ret.removeAll(b);
        return ret;
    }

    private static <T> HashSet<T> union(HashSet<T> a, HashSet<T> b) {
        HashSet<T> ret = new HashSet<>();
        ret.addAll(a);
        ret.addAll(b);
        return ret;
    }

    private static <T> Boolean setCmp(HashSet<T> a, HashSet<T> b) {
        if (a.size() != b.size()) {
            return false;
        }

        for (T e : a) {
            if (!b.contains(e)) {
                return false;
            }
        }

        return true;
    }

    private static <T> HashSet<T> cross(HashSet<T> a, HashSet<T> b) {
        HashSet<T> ret = new HashSet<>();
        for (T t : a) {
            if (b.contains(t)) {
                ret.add(t);
            }
        }
        return ret;
    }
}
