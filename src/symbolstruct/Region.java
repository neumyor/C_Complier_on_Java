package symbolstruct;

import imcode.imexp.IMExp;
import symbolstruct.entries.Entry;

import java.util.ArrayList;

public abstract class Region {
    public ArrayList<IMExp> imexps;
    public Scope scope;
    public final String name;
    public Frame frame;

    protected Region(String name, Scope scope) {
        this.name = name;
        this.scope = scope;
        this.imexps = new ArrayList<>();
        this.frame = null;
    }

    public abstract void gen();

    public abstract ArrayList<Entry> getEntries();

    public abstract void insertEntry(Entry newEntry);
}
