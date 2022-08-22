package improve.component.regpool;

import global.Config;
import imcode.imexp.PrintVarExp;
import improve.component.BasicBlock;
import mips.register.Register;
import symbolstruct.CodeText;
import symbolstruct.FuncRegion;
import symbolstruct.Region;
import symbolstruct.entries.Entry;

import java.util.HashMap;
import java.util.LinkedList;

public class LocRegPool {
    private HashMap<Entry, Register> entry2reg;
    private HashMap<Register, Boolean> isFree;
    private LinkedList<Entry> queue;
    private Region region;

    LocRegPool(Region region) {
        this.region = region;
        this.entry2reg = new HashMap<>();
        this.isFree = new HashMap<>();
        for (Register register : Config.getLocReg()) {
            isFree.put(register, true);
        }
        this.queue = new LinkedList<>();
    }

    Register find(Entry entry) {
        if (this.entry2reg.get(entry) == null) {
            return this.put(entry);
        }
        return this.entry2reg.get(entry);
    }

    Register findNoLoad(Entry entry) {
        if (this.entry2reg.get(entry) == null) {
            return this.putNoLoad(entry);
        }
        return this.entry2reg.get(entry);
    }

    public void save(BasicBlock inBlock) {
        for (Entry entry : this.entry2reg.keySet()) {
            // 如果这个entry在后继基本块不会被访问，而且不是全局变量，那么我们不将其存入内存
            if ((!inBlock.activeOut.contains(entry)) && (!entry.isGlobal())) continue;
            CodeText.textNLine(String.format("# Local Register Pool save %s To memory", entry.name));
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("sw %s %s", this.entry2reg.get(entry), entry.name));
            } else {
                CodeText.textNLine(String.format("sw %s %d($sp)", this.entry2reg.get(entry), region.frame.offsetMap.get(entry)));
            }
        }
    }

    public void init(BasicBlock inBlock) {
        this.entry2reg = new HashMap<>();
        this.isFree = new HashMap<>();
        for (Register register : Config.getLocReg()) {
            isFree.put(register, true);
        }
        this.queue = new LinkedList<>();
    }

    private Register putNoLoad(Entry entry) {
        for (Register register : Config.getLocReg()) {
            if (isFree.get(register)) {
                this.entry2reg.put(entry, register);
                this.isFree.put(register, false);
                this.queue.add(entry);
                return register;
            }
        }

        Entry toRemove = this.queue.poll();
        Register toFree = this.entry2reg.get(toRemove);
        this.entry2reg.remove(toRemove);

        CodeText.textNLine(String.format("# Local Register Pool save %s To memory", toRemove.name));
        if (toRemove.isGlobal()) {
            CodeText.textNLine(String.format("sw %s %s", toFree, toRemove.name));
        } else {
            CodeText.textNLine(String.format("sw %s %d($sp)", toFree, region.frame.offsetMap.get(toRemove)));
        }

        this.entry2reg.put(entry, toFree);
        this.queue.add(entry);
        return toFree;
    }

    private Register put(Entry entry) {
        for (Register register : Config.getLocReg()) {
            if (isFree.get(register)) {
                CodeText.textNLine(String.format("# Local Register Pool load %s From memory", entry.name));
                if (entry.isGlobal()) {
                    CodeText.textNLine(String.format("lw %s %s", register, entry.name));
                } else {
                    CodeText.textNLine(String.format("lw %s %d($sp)", register, region.frame.offsetMap.get(entry)));
                }
                this.entry2reg.put(entry, register);
                this.isFree.put(register, false);
                this.queue.add(entry);
                return register;
            }
        }

        Entry toRemove = this.queue.poll();
        Register toFree = this.entry2reg.get(toRemove);
        this.entry2reg.remove(toRemove);

        CodeText.textNLine(String.format("# Local Register Pool save %s To memory", toRemove.name));
        if (toRemove.isGlobal()) {
            CodeText.textNLine(String.format("sw %s %s", toFree, toRemove.name));
        } else {
            CodeText.textNLine(String.format("sw %s %d($sp)", toFree, region.frame.offsetMap.get(toRemove)));
        }

        this.entry2reg.put(entry, toFree);
        this.queue.add(entry);

        if (entry.isGlobal()) {
            CodeText.textNLine(String.format("lw %s %s", toFree, entry.name));
        } else {
            CodeText.textNLine(String.format("lw %s %d($sp)", toFree, region.frame.offsetMap.get(entry)));
        }
        return toFree;
    }
}
