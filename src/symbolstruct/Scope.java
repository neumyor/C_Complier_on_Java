package symbolstruct;

import global.Error;
import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.Entry;
import symbolstruct.entries.FuncEntry;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Scope 作用域
 * 实际上是一个树节点
 * 存储了当前节点中所有的符号表项
 */
public class Scope {
    protected String name;
    protected Scope pre;
    protected ArrayList<Scope> posts;
    protected final Integer level;
    private static Integer counter = 0;

    private ArrayList<Entry> entries;
    private ArrayList<Entry> params;

    public Scope(Integer level) {
        this.posts = new ArrayList<>();
        this.pre = null;
        this.level = level;
        this.name = String.valueOf(Scope.counter++);
        this.entries = new ArrayList<>();
        this.params = new ArrayList<>();
    }

    protected void registerPara(Entry para) {
        para.isParam = true;
        this.params.add(para);
    }

    protected ArrayList<Entry> getParams() {
        return this.params;
    }

    public ArrayList<Entry> dumpAllEntries() {
        ArrayList<Entry> ret = new ArrayList<>();
        ret.addAll(this.entries);
        for (Scope child : this.posts) {
            ret.addAll(child.dumpAllEntries());
        }
        return ret;
    }

    public ArrayList<Entry> getNowEntries() {
        return entries;
    }

    protected Entry findVar(String name) {
        Scope tmpBlock = this;
        while (tmpBlock != null) {
            ArrayList<Entry> tmpList = tmpBlock.entries;
            for (Entry entry : tmpList) {
                if (entry.name.equals(name) && entry instanceof AbsVarEntry) {
                    return entry;
                }
            }
            tmpBlock = tmpBlock.pre;
        }
        Error.warning("Can't find the definition of " + name + " in " + this.name);
        return null;
    }

    protected Entry findFunc(String name) {
        Scope tmpBlock = this;
        while (tmpBlock != null) {
            ArrayList<Entry> tmpList = tmpBlock.entries;
            for (Entry entry : tmpList) {
                if (entry.name.equals(name) && entry instanceof FuncEntry) {
                    return entry;
                }
            }
            tmpBlock = tmpBlock.pre;
        }
        Error.warning("Can't find the definition of " + name + " in " + this.name);
        return null;
    }

    protected void insertEntry(Entry entry) {
        this.entries.add(entry);
        entry.in = this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scope scope = (Scope) o;
        return Objects.equals(name, scope.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Integer getLevel() {
        return level;
    }
}
