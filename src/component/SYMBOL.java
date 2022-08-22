package component;

/**
 * component.SYMBOL 词法类型枚举类
 *
 * @author neumy
 * @version jdk1.8.0
 */
public enum SYMBOL {
    UNKNOWN,
    IDENFR,
    INTCON,
    STRCON,
    MAINTK,
    CONSTTK,
    INTTK,
    BREAKTK,
    CONTINUETK,
    IFTK,
    ELSETK,
    NOT,
    AND,
    OR,
    WHILETK,
    GETINTTK,
    PRINTFTK,
    RETURNTK,
    PLUS,
    MINU,
    VOIDTK,
    MULT,
    DIV,
    MOD,
    LSS,  // <
    LEQ,  // <=
    GRE,  // >
    GEQ,  // >=
    EQL, // ==
    NEQ, // !=
    ASSIGN,  // =
    SEMICN,  // ;
    COMMA,  // ,
    LPARENT,  // (
    RPARENT,  // )
    LBRACK,  // [
    RBRACK,  // ]
    LBRACE,  // {
    RBRACE,  // }
    IGNORE  // 注释
}
