package imcode.imexp;import symbolstruct.FuncRegion;

/**
 * 四元式类型
 */
public enum IMExpType {
    ParaDef,

    AssignByVarAddr,

    AssignByVar,         // assign a variable by a variable
    AssignByInt,         // assign a variable by a variable
    AssignByGetInt, // assign a variable by getint
    AssignByRet,    // assign a variable by func ret

    AssignToAddr,   // 向指定地址写入
    AssignFromAddr, // 从指定地址读取

    PushPara,       // before call a func, we push the required paras into it
    Call,           // call a func
    Return,

    Label,
    ConJump,
    ConNotJump,
    Jump,

    PrintVar,
    PrintStr,

    Add,
    Sub,
    Mul,
    Div,
    Mod,

    Bgt,Bge,Beq,Bne,

    And,
    Or,
}

