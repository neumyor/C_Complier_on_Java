package symbolstruct;

import global.Logger;
import imcode.imexp.IMExp;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 整个程序的目标代码生成模块
 */
public class CodeRegion {
    public ArrayList<FuncRegion> regions;   // 多个函数模块
    public GlobalRegion glbRegion;          // 全局模块

    public HashMap<String, String> strMap;  // 字符表
    public Region curRegion;

    public CodeRegion() {
        this.regions = new ArrayList<>();
        this.strMap = new HashMap<>();
    }

    public void addRegion(FuncRegion region) {
        this.regions.add(region);
        this.curRegion = region;
    }

    public void addGlbRegion(GlobalRegion region) {
        this.glbRegion = region;
        this.curRegion = region;
    }

    public void addStr(String name, String value) {
        this.strMap.put(name, value);
    }

    public void gen() {
        glbRegion.gen();    // 在data区内声明全局变量并在text区进行初始化，一定要在字符表声明前进行，因为全局变量必须字对齐。如果先声明字符表会破坏这一约束。

        for (String e : strMap.keySet()) {
            CodeText.dataNLine(String.format("%s: .ascii \"%s\\0\"", e, strMap.get(e)));
        }
        // 在data区内声明字符表

        CodeText.textNLine("jal main");
        CodeText.textNLine("nop");
        CodeText.textNLine("li $v0, 10");
        CodeText.textNLine("syscall");
        // 跳转到main函数入口，并且在程序结束后正常退出

        for (FuncRegion region : regions) {
            region.gen();
        }
        // 初始化所有函数模块，详见FuncRegion

        Logger.GetLogger().MIPSLog(".data");
        Logger.GetLogger().MIPSLog(CodeText.dumpData());
        Logger.GetLogger().MIPSLog(".text");
        Logger.GetLogger().MIPSLog(CodeText.dumpText());
        // 将data和text区打印出来
    }

    public void addIMExp(IMExp e) {
        this.curRegion.imexps.add(e);
    }
}
