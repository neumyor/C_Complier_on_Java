package improve.component;

import imcode.imexp.AssignByVarExp;
import imcode.imexp.IMExp;
import imcode.imitem.VarItem;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * 基本块
 */
public class BasicBlock {
    public ArrayList<IMExp> imexps;   // 每个基本块包含多条中间代码
    public FlowGraph flowGraph;  // 每个基本块从属于一个流图
    public String name;    // 每个基本块分配一个名称，来对其进行唯一标识

    public ArrayList<BasicBlock> pre;   // 前继基本块

    public ArrayList<BasicBlock> post;  // 后继基本块

    //活跃变量
    public HashSet<Entry> activeDef;
    public HashSet<Entry> activeUse;
    public HashSet<Entry> activeIn;
    public HashSet<Entry> activeOut;

    // 到达定义
    public HashSet<IMExp> arriveIn;
    public HashSet<IMExp> arriveOut;
    public HashSet<IMExp> arriveGen;
    public HashSet<IMExp> arriveKill;

    // 可用表达式
    public HashSet<ValidExp> validIn;
    public HashSet<ValidExp> validOut;
    public HashSet<ValidExp> validGen;
    public HashSet<ValidExp> validKill;

    //赋值表达式
    public HashSet<AssignByVarExp> assignIn;
    public HashSet<AssignByVarExp> assignOut;
    public HashSet<AssignByVarExp> assignGen;
    public HashSet<AssignByVarExp> assignKill;

    protected BasicBlock(ArrayList<IMExp> exps, FlowGraph graph, String name) {
        this.imexps = exps;
        this.flowGraph = graph;
        this.pre = new ArrayList<>();
        this.post = new ArrayList<>();

        this.activeDef = new HashSet<>();
        this.activeUse = new HashSet<>();
        this.activeIn = new HashSet<>();
        this.activeOut = new HashSet<>();

        this.arriveIn = new HashSet<>();
        this.arriveOut = new HashSet<>();
        this.arriveGen= new HashSet<>();
        this.arriveKill = new HashSet<>();

        this.validOut = new HashSet<>();
        this.validIn = new HashSet<>();
        this.validGen = new HashSet<>();
        this.validKill = new HashSet<>();

        this.assignIn = new HashSet<>();
        this.assignOut = new HashSet<>();
        this.assignGen = new HashSet<>();
        this.assignKill = new HashSet<>();

        this.name = name;
    }

    public ArrayList<IMExp> dump() {
        return this.imexps;
    }

    public IMExp getLastExp() {
        if (this.imexps.size() == 0) {
            return null;
        }
        return this.imexps.get(this.imexps.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name + ":\n");

        builder.append("pre:");

        for(BasicBlock block:this.pre) {
            builder.append(block.name+" ");
        }
        builder.append("\n");


        builder.append("post:");

        for(BasicBlock block:this.post) {
            builder.append(block.name+" ");
        }

        builder.append("\n");

        for (IMExp exp : imexps) {
            builder.append(exp + "\n");
        }

        builder.append("active in:[");
        for (Entry e : activeIn) {
            builder.append(e.name + " ");
        }
        builder.append("]\n");

        builder.append("active out:[");
        for (Entry e : activeOut) {
            builder.append(e.name + " ");
        }
        builder.append("]\n");

        builder.append("active def:[");
        for (Entry e : activeDef) {
            builder.append(e.name + " ");
        }
        builder.append("]\n");

        builder.append("active use:[");
        for (Entry e : activeUse) {
            builder.append(e.name + " ");
        }
        builder.append("]\n");

        builder.append("valid in:[");
        for (ValidExp e : validIn) {
            builder.append(e.exp + " ");
        }
        builder.append("]\n");

        builder.append("valid out:[");
        for (ValidExp e : validOut) {
            builder.append(e.exp + " ");
        }
        builder.append("]\n");

        builder.append("valid gen:[");
        for (ValidExp e : validGen) {
            builder.append(e.exp + " ");
        }
        builder.append("]\n");

        builder.append("valid kill:[");
        for (ValidExp e : validKill) {
            builder.append(e.exp + " ");
        }
        builder.append("]\n");

        builder.append("assign in:[");
        for (IMExp e : assignIn) {
            builder.append(e + " ");
        }
        builder.append("]\n");

        builder.append("assign out:[");
        for (IMExp e : assignOut) {
            builder.append(e + " ");
        }
        builder.append("]\n");

        builder.append("assign gen:[");
        for (IMExp e : assignGen) {
            builder.append(e + " ");
        }
        builder.append("]\n");

        builder.append("assign kill:[");
        for (IMExp e : assignKill) {
            builder.append(e + " ");
        }
        builder.append("]\n");
        
        
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock block = (BasicBlock) o;
        return this.name.equals((block).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, flowGraph);
    }
}
