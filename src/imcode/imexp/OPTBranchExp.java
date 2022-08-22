package imcode.imexp;

import component.Label;
import imcode.imitem.LabelItem;
import imcode.imitem.VarItem;
import improve.component.regpool.RegPool;
import symbolstruct.CodeText;
import symbolstruct.entries.ConstValueEntry;
import symbolstruct.entries.Entry;

public class OPTBranchExp extends IMExp {
    private IMExp calExp;
    private IMExp jumpExp;

    public OPTBranchExp(IMExp calExp, IMExp jumpExp) {
        assert calExp instanceof BgeExp || calExp instanceof BgtExp || calExp instanceof BeqExp || calExp instanceof BneExp;
        assert jumpExp instanceof ConJumpExp || jumpExp instanceof ConNotJumpExp;
        this.calExp = calExp;
        this.jumpExp = jumpExp;

        this.item1 = jumpExp.item1;
        this.item2 = calExp.item2;
        this.item3 = calExp.item3;
    }

    @Override
    public void toCode(RegPool pool) {
        Entry calA = ((VarItem) calExp.item2).entry;
        Entry calB = ((VarItem) calExp.item3).entry;
        Label label = ((LabelItem) jumpExp.item1).labelName;
        if (jumpExp instanceof ConJumpExp) {
            if (calExp instanceof BgtExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue > bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("blt %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("bgt %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("bgt %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BgeExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue >= bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("ble %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("bge %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("bge %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BeqExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue == bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("beq %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("beq %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("beq %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BneExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue != bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("bne %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("bne %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("bne %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            }
        } else if (jumpExp instanceof ConNotJumpExp) {
            if (calExp instanceof BgtExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue <= bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("bge %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("ble %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("ble %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BgeExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue < bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("bgt %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("blt %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("blt %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BeqExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue != bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("bne %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("bne %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("bne %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            } else if (calExp instanceof BneExp) {
                if (calA instanceof ConstValueEntry && calB instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    if (aValue == bValue) {
                        CodeText.textNLine(String.format("j %s", label.labelName));
                    }
                } else if (calA instanceof ConstValueEntry) {
                    Integer aValue = (Integer) ((ConstValueEntry) calA).getValue();
                    CodeText.textNLine(String.format("beq %s, %d, %s", pool.find(calB), aValue, label.labelName));
                } else if (calB instanceof ConstValueEntry) {
                    Integer bValue = (Integer) ((ConstValueEntry) calB).getValue();
                    CodeText.textNLine(String.format("beq %s, %d, %s", pool.find(calA), bValue, label.labelName));
                } else {
                    CodeText.textNLine(String.format("beq %s, %s, %s", pool.find(calA), pool.find(calB), label.labelName));
                }
            }
        }
    }

    @Override
    public String toString() {
        return jumpExp.getClass().getSimpleName() + " " + calExp.getClass().getSimpleName() + " "
                + ((VarItem)item2).entry.name + " "
                + ((VarItem)item3).entry.name + " "
                + ((LabelItem)item1).labelName.labelName;
    }
}
