package improve.component.optimizers;

import component.datatype.IntType;
import global.Config;
import imcode.imexp.*;
import imcode.imitem.IMItemFac;
import imcode.imitem.IMItemType;
import imcode.imitem.VarItem;
import improve.component.BasicBlock;
import improve.component.ValidExp;
import symbolstruct.Region;
import symbolstruct.entries.Entry;
import symbolstruct.entries.VarEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GlobalExpOptimizer extends Optimizer {
    Region belongRegion;

    public GlobalExpOptimizer(ArrayList<IMExp> exps, ArrayList<BasicBlock> blocks,
                              HashMap<IMExp, BasicBlock> exp2block, Region region) {
        super(exps, blocks, exp2block);
        this.belongRegion = region;
        optimize();
    }

    @Override
    protected void optimize() {
        HashSet<ValidExp> validExps = new HashSet<>();
        for (IMExp exp : this.inExps) {
            if (exp instanceof AddExp ||
                    exp instanceof SubExp ||
                    exp instanceof MulExp ||
                    exp instanceof DivExp ||
                    exp instanceof ModExp) {
                validExps.add(new ValidExp(exp));
            }
        }

        for (IMExp exp : this.inExps) {
            if (!(exp instanceof AddExp ||
                    exp instanceof SubExp ||
                    exp instanceof MulExp ||
                    exp instanceof DivExp ||
                    exp instanceof ModExp)) {
                continue;   // 跳过不涉及计算的语句
            }

            ValidExp validExp = new ValidExp(exp);
            BasicBlock inBlock = exp2block.get(exp);

            if (!inBlock.validIn.contains(validExp)) {
                continue;
            }

            Boolean valid = true;
            for (IMExp otherExp : inBlock.imexps) {
                if (otherExp.equals(exp)) {
                    break;
                }

                if (!(otherExp.item1 instanceof VarItem)) {
                    continue;
                }

                Entry assigned = ((VarItem) otherExp.item1).entry;
                if (validExp.in(assigned)) {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                break;
            }

            Entry newEntry = new VarEntry("GLB_EXP_OPT_" + Config.getTmpNameSed(), new IntType());
            newEntry.in = this.belongRegion.scope;
            this.belongRegion.frame.insertEntry(newEntry);

            HashMap<BasicBlock, Boolean> isBlockDone = new HashMap<>();
            for (BasicBlock preBlk : inBlock.pre) {
                travelAndReplace(exp, isBlockDone, validExp, preBlk, newEntry);
            }

            Entry toAssign = ((VarItem) exp.item1).entry;
            for (int i = 0; i < inBlock.imexps.size(); i++) {
                if (inBlock.imexps.get(i).equals(exp)) {
                    inBlock.imexps.set(i, IMFac.gen(IMExpType.AssignByVar, toAssign, newEntry));
                }
            }
        }

        for (BasicBlock blk : this.blocks) {
            this.optExps.addAll(blk.imexps);
        }
    }

    private void travelAndReplace(IMExp fromExp, HashMap<BasicBlock, Boolean> isBlockDone,
                                  ValidExp toReplace, BasicBlock blk, Entry entry) {
        if (isBlockDone.getOrDefault(blk, false) == true) {
            return;
        }

        isBlockDone.put(blk, true);

        for (int i = blk.imexps.size() - 1; i >= 0; i--) {
            IMExp exp = blk.imexps.get(i);

            if(exp.equals(fromExp)) {
                continue;
            }

            if (!(exp instanceof AddExp ||
                    exp instanceof SubExp ||
                    exp instanceof MulExp ||
                    exp instanceof DivExp ||
                    exp instanceof ModExp)) {
                continue;   // 跳过不涉及计算的语句
            }
            ValidExp validExp = new ValidExp(exp);
            if (validExp.equals(toReplace)) {
                Entry oldAssigned = ((VarItem) exp.item1).entry;
                exp.item1 = IMItemFac.gen(IMItemType.Var, entry);
                IMExp newExp = IMFac.gen(IMExpType.AssignByVar, oldAssigned, entry);
                // 新增表达式不要忘记维护好相关的数据结构
                blk.imexps.add(i + 1, newExp);
                this.exp2block.put(newExp, blk);
                return;
            }
        }

        for (BasicBlock preBlk : blk.pre) {
            travelAndReplace(fromExp, isBlockDone, toReplace, preBlk, entry);
        }
    }
}

