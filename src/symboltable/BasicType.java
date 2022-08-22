package symboltable;

/**
 * BasicType
 * 作为SymbolItem对象成员
 * 表明该符号项的宏观类型
 * 比如一个返回值为int的函数，可以看做一个int值
 */
public enum BasicType {
    INT,
    VOID,
    STR,
    UNK,
}
