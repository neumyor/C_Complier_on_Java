package symbolstruct;

import imcode.imexp.IMExp;
import improve.component.FlowGraph;
import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.Entry;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class GlobalRegion extends Region{

    public GlobalRegion(Scope scope) {
        super("#GLOBAL_REGION#", scope);
    }

    @Override
    public void gen() {
        FlowGraph flowGraph = new FlowGraph(this);
    }

    @Override
    public ArrayList<Entry> getEntries() {
        return (ArrayList<Entry>) scope.getNowEntries().stream().filter(e -> e.isGlobal())
                .filter(e -> e instanceof AbsVarEntry).collect(Collectors.toList());
    }

    @Override
    public void insertEntry(Entry newEntry) {

    }
}
