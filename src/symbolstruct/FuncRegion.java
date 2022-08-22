package symbolstruct;

import imcode.imexp.IMExp;
import improve.component.FlowGraph;
import symbolstruct.entries.Entry;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 负责目标代码中各个函数的转化
 */
public class FuncRegion extends Region{

    public FuncRegion(String name, Scope scope) {
        super(name, scope);
        this.imexps = new ArrayList<>();
    }

    public void gen() {
        ArrayList<Entry> param = scope.getParams();
        // 获取当前函数根Scope所声明的形式参数对应的符号项们，这是由Scope::insertParaVar/Array 以及 Converter::FuncFParam支持的机制

        ArrayList<Entry> local = (ArrayList<Entry>) scope.dumpAllEntries().stream()
                .filter(e -> !param.contains(e))
                .collect(Collectors.toList());
        // 从当前函数根Scope及其子Scope中导出所有声明的符号项，并且过滤掉函数形参

        this.frame = new Frame(param, local);
        // 通过形参和局部变量构建其栈帧对象， 详见Frame

        FlowGraph flowGraph = new FlowGraph(this);
        //  将当前函数构造成流图，其同时会将流图中的东西导出到CodeText中
    }

    @Override
    public ArrayList<Entry> getEntries() {
        return this.scope.dumpAllEntries();
    }

    @Override
    public void insertEntry(Entry newEntry) {

    }
}
