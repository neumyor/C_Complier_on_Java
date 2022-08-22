package imcode.imitem;

/**
 * 四元式的操作数类型
 */
public enum IMItemType {
    Var,        // 变量类型
    Func,       // 函数类型
    Str,        // 字符串类型
    Int,        // 常数
    Label,
    Array,
    NPos,       // 数组中元素位置
    Space,      // 空间元素，即指代栈上的一段空间
}
