package improve.component.optimizers;

import imcode.imexp.AssignByVarExp;
import imcode.imexp.IMExp;
import imcode.imitem.IMItemFac;
import imcode.imitem.IMItemType;
import imcode.imitem.VarItem;
import improve.component.BasicBlock;
import symbolstruct.entries.Entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SeekOptimizer extends Optimizer {

    private HashMap<IMExp, HashSet<IMExp>> expArriveIn;

    public SeekOptimizer(ArrayList<IMExp> exps, ArrayList<BasicBlock> blocks,
                         HashMap<IMExp, BasicBlock> map,
                         HashMap<IMExp, HashSet<IMExp>> expArriveIn) {
        super(exps, blocks, map);
        this.expArriveIn = expArriveIn;
        optimize();
    }

    @Override
    protected void optimize() {
        for (BasicBlock blk : this.blocks) {
            for (int i = 0; i < blk.imexps.size(); i++) {
                IMExp exp = blk.imexps.get(i);
                if (!(exp instanceof AssignByVarExp)) {
                    continue;
                }

                // exp: x:=y
                Entry x = ((VarItem) exp.item1).entry;
                Entry y = ((VarItem) exp.item2).entry;

                ArrayList<IMExp> references = new ArrayList<>();

                // 试图寻找所有其引用
                for (IMExp otherExp : this.inExps) {

                    // 跳过没有引用该变量x的语句
                    if (!((otherExp.item2 instanceof VarItem && ((VarItem) otherExp.item2).entry.equals(x)) ||
                            (otherExp.item3 instanceof VarItem && ((VarItem) otherExp.item3).entry.equals(x)))) {
                        continue;
                    }

                    if (!expArriveIn.getOrDefault(otherExp, new HashSet<>()).contains(exp)) {
                        continue;
                    }

                    references.add(otherExp);
                }

                // 检查所有引用是否符合要求
                Boolean removable = true;
                for (int k = 0; k < references.size(); k++) {
                    IMExp reference = references.get(k);
                    BasicBlock refBlock = exp2block.get(reference);

                    // 如果赋值语句exp和引用语句otherExp处在同一个基本块
                    if (refBlock.equals(blk)) {
                        Boolean valid = true;
                        Integer assignIndex = null;
                        Integer referenceIndex = null;
                        for (int g = 0; g < refBlock.imexps.size(); g++) {
                            IMExp potentialAssignExp = refBlock.imexps.get(g);
                            if (potentialAssignExp.equals(exp)) assignIndex = g;
                            if (potentialAssignExp.equals(reference)) referenceIndex = g;
                        }

                        // 如果赋值语句在引用之前，那么需要保证两者之间不存在新的对x和y的赋值
                        if (assignIndex < referenceIndex) {
                            for (int g = assignIndex + 1; g < referenceIndex; g++) {
                                IMExp potentialAssignExp = refBlock.imexps.get(g);
                                // 跳过非赋值语句
                                if (!(potentialAssignExp.item1 instanceof VarItem)) {
                                    continue;
                                }
                                Entry assigned = ((VarItem) potentialAssignExp.item1).entry;
                                if ((assigned.equals(x) || assigned.equals(y))) {
                                    valid = false;
                                    break;
                                }
                            }

                            if(!valid) {
                                removable = false;
                                break;
                            } else {
                                continue;
                            }
                        } else {
                            // 如果引用语句在赋值语句之前（这是可能的，考虑基本块的循环），则只考虑从基本块开始计算的路径
                            // 则说明需要赋值语句处于该循环块的入口才行，类似其他非同一基本块的情况
                            // 啥也不敢，以跳出IF;
                        }
                    }

                    if (!refBlock.assignIn.contains(exp)) {
                        removable = false;
                        break;
                    }

                    Boolean valid = true;
                    for (int g = 0; g < refBlock.imexps.size(); g++) {
                        IMExp potentialAssignExp = refBlock.imexps.get(g);

                        //当检查到当前引用语句reference时退出
                        if (potentialAssignExp.equals(reference)) {
                            break;
                        }

                        // 跳过非赋值类语句
                        if (!(potentialAssignExp.item1 instanceof VarItem)) {
                            continue;
                        }

                        // 如果在reference之前有语句对x或y做出赋值
                        Entry assigned = ((VarItem) potentialAssignExp.item1).entry;
                        if (assigned.equals(x) || assigned.equals(y)) {
                            valid = false;
                            break;
                        }
                    }

                    if (!valid) {
                        removable = false;
                        break;
                    }
                }

                if (removable && !((VarItem) exp.item1).entry.isGlobal() &&
                        !((VarItem) exp.item1).entry.name.contains("_TEMPnym")) {
                    // #TRADEOFF 我们仅删除对非临时变量且非全局变量的赋值语句，
                    // 前者是因为我们的到达定义分析仅针对了非临时变量，
                    // 后者是因为我们不确定其他函数是否引用全白变量
                    // 删除表达式不要忘了维护好相关数据结构
                    blk.imexps.remove(i);
                    i--;
                    this.exp2block.remove(exp);
                    this.inExps.remove(exp);

                    for (IMExp reference : references) {
                        VarItem replace = (VarItem) IMItemFac.gen(IMItemType.Var, y);

                        if (reference.item2 instanceof VarItem && ((VarItem) reference.item2).entry.equals(x)) {
                            reference.item2 = replace;
                        }

                        if (reference.item3 instanceof VarItem && ((VarItem) reference.item3).entry.equals(x)) {
                            reference.item3 = replace;
                        }
                    }
                }
            }
        }

        for (BasicBlock blk : this.blocks) {
            this.optExps.addAll(blk.imexps);
        }
    }
}
