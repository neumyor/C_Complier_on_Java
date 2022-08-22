package symbolstruct;

import component.datatype.ArrayType;
import component.datatype.Datatype;
import component.datatype.IntType;
import component.narray.NArray;
import global.Error;
import imcode.imexp.IMExp;
import symbolstruct.entries.*;

import java.util.ArrayList;

/**
 * Scoper
 * 专用于管理Scope的结构，
 */
public class Symer {
    private Scope root;
    private Scope cur;

    public Symer() {
        this.root = null;
        this.cur = null;
    }

    public void pushScope() {
        Scope temp;
        if (this.root == null) {
            temp = new Scope(0);
            temp.pre = null;
            this.root = temp;
        } else {
            temp = new Scope(this.cur.level + 1);
            temp.pre = this.cur;
            this.cur.posts.add(temp);
        }
        this.cur = temp;
    }

    public void quitScope() {
        this.cur = this.cur.pre;
    }

    public Entry findVar(String name) {
        return this.cur.findVar(name);
    }

    public Entry findFunc(String name) {
        return this.root.findFunc(name);    // 只有全局块，也就是root块才会有函数声明
    }

    public Entry insertVar(String name, Datatype type) {
        Entry ret = new VarEntry(name, type);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry allocSpace(String name, Integer size) {
        Entry ret = new SpaceEntry(name, size);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry insertArray(String name, Datatype type) {
        Entry ret = new ArrayEntry(name, type);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry insertFunc(String name, Datatype type, ArrayList<AbsVarEntry> params) {
        Entry ret = new FuncEntry(name, type, params);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry insertConstVar(String name, Datatype type, Integer value) {
        Entry ret = new ConstVarEntry(name, type, value);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry insertConstArray(String name, Datatype type, NArray value) {
        Entry ret = new ConstArrayEntry(name, type, value);
        this.cur.insertEntry(ret);
        return ret;
    }

    public Entry insertParaVar(String name, Datatype type) {
        Entry ret = new VarEntry(name, type);
        this.cur.insertEntry(ret);
        this.cur.registerPara(ret);
        return ret;
    }

    public Entry insertParaArray(String name, Datatype type) {
        Entry ret = new ArrayEntry(name, type);
        this.cur.insertEntry(ret);
        this.cur.registerPara(ret);
        return ret;
    }

    public Scope getCur() {
        return cur;
    }
}
