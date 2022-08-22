package improve.component.optimizers;

import imcode.imexp.IMExp;
import improve.component.BasicBlock;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Optimizer {
    protected ArrayList<IMExp> inExps;
    protected ArrayList<IMExp> optExps;
    protected ArrayList<BasicBlock> blocks;
    public HashMap<IMExp, BasicBlock> exp2block;

    protected Optimizer(ArrayList<IMExp> exps, ArrayList<BasicBlock> blocks, HashMap<IMExp, BasicBlock> exp2block) {
        this.exp2block = exp2block;
        this.blocks = blocks;
        this.inExps = exps;
        this.optExps = new ArrayList<>();
    }

    protected abstract void optimize();

    public ArrayList<IMExp> dump() {
        return this.optExps;
    }
}
