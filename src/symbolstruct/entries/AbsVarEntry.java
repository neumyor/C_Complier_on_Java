package symbolstruct.entries;

import component.datatype.Datatype;

import java.util.Objects;

/**
 * 变量符号项父类
 * 包含数组变量和普通变量
 */
public abstract class AbsVarEntry extends Entry{

    public Integer size;    // 该符号表项所应该对应在栈帧中的长度，单位为字节

    protected AbsVarEntry(String name, Datatype datatype) {
        super(name, datatype);
    }
}
