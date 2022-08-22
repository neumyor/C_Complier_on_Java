package improve.component.regpool;

import mips.register.Register;
import symbolstruct.CodeText;
import symbolstruct.Region;
import symbolstruct.entries.Entry;

import java.util.HashMap;
import java.util.HashSet;

public class GlbRegPool {
    private HashMap<Entry, Register> entry2reg;
    private HashMap<Entry, Boolean> isLoaded;
    private Region region;

    GlbRegPool(HashMap<Entry, Register> input, Region region) {
        this.region = region;
        this.entry2reg = input;
        this.isLoaded = new HashMap<>();
    }

    Register find(Entry entry) {
        return this.entry2reg.get(entry);
    }

    Register findNoLoad(Entry entry) {
        return this.entry2reg.get(entry);
    }

    public void save() {
        for (Entry entry : this.entry2reg.keySet()) {
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("# Global Register Pool save %s To memory", entry.name));
                CodeText.textNLine(String.format("sw %s %s", this.entry2reg.get(entry), entry.name));
            }   // 保存时只保留全局变量
        }
    }

    public void saveActive(HashSet<Entry> actives) {
        for (Entry entry : this.entry2reg.keySet()) {
            if (!actives.contains(entry)) {
                continue; // 跳过不活跃的变量
            }
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("# Global Register Pool save %s To memory", entry.name));
                CodeText.textNLine(String.format("sw %s %s", this.entry2reg.get(entry), entry.name));
            } else {
                CodeText.textNLine(String.format("# Global Register Pool save %s To memory", entry.name));
                CodeText.textNLine(String.format("sw %s %d($sp)", this.entry2reg.get(entry), region.frame.offsetMap.get(entry)));
            }
        }
    }

    public void saveGlb() {
        for (Entry entry : this.entry2reg.keySet()) {
            if (!entry.isGlobal()) {
                continue; // 跳过非全局的变量
            }
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("# Global Register Pool save %s To memory", entry.name));
                CodeText.textNLine(String.format("sw %s %s", this.entry2reg.get(entry), entry.name));
            } else {
                CodeText.textNLine(String.format("# Global Register Pool save %s To memory", entry.name));
                CodeText.textNLine(String.format("sw %s %d($sp)", this.entry2reg.get(entry), region.frame.offsetMap.get(entry)));
            }
        }
    }

    public void init() {
        CodeText.textNLine("\n# Global Register init");
        for (Entry entry : entry2reg.keySet()) {
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("# Global Register Pool load %s From memory", entry.name));
                CodeText.textNLine(String.format("lw %s %s", this.entry2reg.get(entry), entry.name));
            } else if (entry.isParam) {
                CodeText.textNLine(String.format("# Global Register Pool load %s From memory", entry.name));
                CodeText.textNLine(String.format("lw %s %d($sp)", this.entry2reg.get(entry), region.frame.offsetMap.get(entry)));
            }
            // 初始化时不加载非全局也非函数参数的变量
        }
    }

    public void loadActive(HashSet<Entry> actives) {
        for (Entry entry : entry2reg.keySet()) {
            if(!actives.contains(entry)) {
                continue;
            }
            if (entry.isGlobal()) {
                CodeText.textNLine(String.format("# Global Register Pool load %s From memory", entry.name));
                CodeText.textNLine(String.format("lw %s %s", this.entry2reg.get(entry), entry.name));
            } else {
                CodeText.textNLine(String.format("# Global Register Pool load %s From memory", entry.name));
                CodeText.textNLine(String.format("lw %s %d($sp)", this.entry2reg.get(entry), region.frame.offsetMap.get(entry)));
            }
        }
    }
}
