package improve.component.optimizers;

import imcode.imexp.*;
import imcode.imitem.IMItemFac;
import imcode.imitem.IMItemType;
import imcode.imitem.VarItem;
import improve.component.BasicBlock;
import symbolstruct.entries.ConstFeature;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.ConstVarEntry;
import symbolstruct.entries.Entry;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstOptimizer extends Optimizer {
    public ConstOptimizer(ArrayList<IMExp> exps, ArrayList<BasicBlock> blocks, HashMap<IMExp, BasicBlock> flowGraph) {
        super(exps, blocks, flowGraph);
        this.optimize();
    }

    @Override
    protected void optimize() {
        for (int i = 0; i < this.blocks.size(); i++) {
            BasicBlock curBlock = this.blocks.get(i);

            // 正序遍历，进行常量传播
            for (int j = 0; j < curBlock.imexps.size(); j++) {
                IMExp curExp = curBlock.imexps.get(j);
                if (!(curExp.item1 instanceof VarItem)) {
                    continue;
                }
                Entry toReplace = ((VarItem) curExp.item1).entry;
                Entry canReplaceBy = extractReplaceEntry(curExp);
                if (canReplaceBy != null) {
                    Boolean stopFlag = false;
                    for (int k = j + 1; k < curBlock.imexps.size(); k++) {
                        IMExp toExp = curBlock.imexps.get(k);
                        if (stopFlag) {
                            break;
                        }
                        if (toExp.item1 instanceof VarItem && toReplace.equals(((VarItem) toExp.item1).entry)) {
                            stopFlag = true;
                        }
                        replaceCertainEntry(toExp, toReplace, canReplaceBy);
                    }
                }
            }

            // 逆序遍历，删除常量传播后无用的代码
            for (int j = curBlock.imexps.size() - 1; j >= 0; j--) {
                IMExp curExp = curBlock.imexps.get(j);

                // 仅考虑被赋值变量是变量类型的语句
                if (!(curExp.item1 instanceof VarItem)) {
                    continue;
                }

                if (curExp instanceof AssignByGetInt || curExp instanceof AssignToAddrExp) {
                    continue;
                }

                // 如果是被赋值变量是全局变量，那么不能删
                if (((VarItem) curExp.item1).entry.isGlobal() &&
                        !((VarItem) curExp.item1).entry.name.contains("_TEMPnym")) {
                    continue;
                }

                // 如果out集合中含有该被赋值变量，那么说明它不能删
                if (curBlock.activeOut.contains(((VarItem) curExp.item1).entry)) {
                    continue;
                }

                // 如果有该基本块内含有该赋值变量，那么说明它不能删
                Boolean canRemove = true;
                for (int k = j + 1; k < curBlock.imexps.size(); k++) {
                    IMExp toExp = curBlock.imexps.get(k);
                    if (curExp.item1.equals(toExp.item2) ||
                            curExp.item1.equals(toExp.item3)) {
                        canRemove = false;
                        break;
                    }
                }

                // 执行删除
                if (canRemove) {
                    // 删除表达式不要忘记维护好相关数据结构
                    curBlock.imexps.remove(j);
                    this.exp2block.remove(curExp);
                    this.inExps.remove(curExp);
                }
            }
        }


        for (int i = 0; i < this.blocks.size(); i++) {
            this.optExps.addAll(this.blocks.get(i).imexps);
        }
    }


    private void replaceCertainEntry(IMExp exp, Entry replaced, Entry by) {
        if (exp.item2 != null && exp.item2 instanceof VarItem && ((VarItem) exp.item2).entry.equals(replaced)) {
            exp.item2 = IMItemFac.gen(IMItemType.Var, by);
        }
        if (exp.item3 != null && exp.item3 instanceof VarItem && ((VarItem) exp.item3).entry.equals(replaced)) {
            exp.item3 = IMItemFac.gen(IMItemType.Var, by);
        }
    }

    private Entry extractReplaceEntry(IMExp exp) {
        if (exp instanceof AssignByVarExp) {
            Entry toCheck = ((VarItem) exp.item2).entry;
            if (toCheck instanceof ConstVarEntry || toCheck instanceof ConstValueEntry) {
                Entry constEntry = new ConstValueEntry((Integer) ((ConstFeature) toCheck).getValue());
                return constEntry;
            } else {
                return toCheck;
            }
        } else if (exp instanceof AddExp) {
            Entry opA = ((VarItem) exp.item2).entry;
            Entry opB = ((VarItem) exp.item3).entry;
            if ((opA instanceof ConstVarEntry || opA instanceof ConstValueEntry) &&
                    (opB instanceof ConstVarEntry || opB instanceof ConstValueEntry)) {
                Integer result = ((Integer) ((ConstFeature) opA).getValue()) + ((Integer) ((ConstFeature) opB).getValue());
                Entry constEntry = new ConstValueEntry(result);
                return constEntry;
            }
        } else if (exp instanceof SubExp) {
            Entry opA = ((VarItem) exp.item2).entry;
            Entry opB = ((VarItem) exp.item3).entry;
            if ((opA instanceof ConstVarEntry || opA instanceof ConstValueEntry) &&
                    (opB instanceof ConstVarEntry || opB instanceof ConstValueEntry)) {
                Integer result = ((Integer) ((ConstFeature) opA).getValue()) - ((Integer) ((ConstFeature) opB).getValue());
                Entry constEntry = new ConstValueEntry(result);
                return constEntry;
            }
        } else if (exp instanceof MulExp) {
            Entry opA = ((VarItem) exp.item2).entry;
            Entry opB = ((VarItem) exp.item3).entry;
            if ((opA instanceof ConstVarEntry || opA instanceof ConstValueEntry) &&
                    (opB instanceof ConstVarEntry || opB instanceof ConstValueEntry)) {
                Integer result = ((Integer) ((ConstFeature) opA).getValue()) * ((Integer) ((ConstFeature) opB).getValue());
                Entry constEntry = new ConstValueEntry(result);
                return constEntry;
            }
        } else if (exp instanceof DivExp) {
            Entry opA = ((VarItem) exp.item2).entry;
            Entry opB = ((VarItem) exp.item3).entry;
            if ((opA instanceof ConstVarEntry || opA instanceof ConstValueEntry) &&
                    (opB instanceof ConstVarEntry || opB instanceof ConstValueEntry)) {
                Integer result = ((Integer) ((ConstFeature) opA).getValue()) / ((Integer) ((ConstFeature) opB).getValue());
                Entry constEntry = new ConstValueEntry(result);
                return constEntry;
            }
        } else if (exp instanceof ModExp) {
            Entry opA = ((VarItem) exp.item2).entry;
            Entry opB = ((VarItem) exp.item3).entry;
            if ((opA instanceof ConstVarEntry || opA instanceof ConstValueEntry) &&
                    (opB instanceof ConstVarEntry || opB instanceof ConstValueEntry)) {
                Integer result = ((Integer) ((ConstFeature) opA).getValue()) % ((Integer) ((ConstFeature) opB).getValue());
                Entry constEntry = new ConstValueEntry(result);
                return constEntry;
            }
        } else if (exp instanceof BeqExp) {

        }
        return null;
    }
}
