package component;

/**
 * component.GUNIT 语法分析的解析单元，对应文法中的所有非终结符
 * <p>
 * 其中PureChar是用于检查多可能的终结符所特设的，比如乘除模运算符有三种。
 * 这种结构虽然在文法中没有定义为非终结符，但是本程序将其等用于新的非终结符进行解析。
 * 输出调试信息时会针对这个类型进行处理，以保证不会输出到日志中
 * <p>
 */
public enum GUNIT {
    /* 忽略了 BlockItem, BType, Decl三个非终结符 */
    CompUnit,
    ConstDecl,
    VarDecl,
    ConstDef,
    ConstInitVal,
    VarDef,
    InitVal,
    FuncDef,
    MainFuncDef,
    FuncType,
    FuncFParams,
    FuncFParam,
    Block,
    Stmt,
    Exp,
    Cond,
    LVal,
    PrimaryExp,
    Number,
    UnaryExp,
    UnaryOp,
    FuncRParams,
    MulExp,
    AddExp,
    RelExp,
    EqExp,
    LAndExp,
    LOrExp,
    ConstExp,
    // 以下都是不需要输出的GUnit类型
    BlockItem,
    BType,
    Decl,
}
