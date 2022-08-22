import component.SYMBOL;
import component.Token;
import global.Error;

import java.util.ArrayList;
import java.util.List;

/**
 * LexParser 词法分析器
 * 其parse方法返回一个Token List
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class LexParser {
    private final FileReader reader;
    private final List<Token> tokenList;

    public LexParser(String inputFilePath) {
        this.reader = new FileReader(inputFilePath);
        this.tokenList = new ArrayList<>();
    }

    public List<Token> parse() {
        while (true) {
            SYMBOL symbol = SYMBOL.IGNORE;
            reader.clearToken();
            /* 清除无关空白符 */
            while (reader.isSpace() || reader.isNewlineN() || reader.isNewlineR() || reader.isTab()) {
                // 假设仅计 \n
                if (reader.isNewlineN()) {
                    reader.pushLine();
                }
                reader.getChar();
            }

            /* 枚举优先级：保留字 - 标识符 - 常整数 - 分界符 - 字符串 */
            /* 保留字与标识符 */
            if (reader.isLetter()) {
                while (reader.isLetter() || reader.isDigit()) {
                    reader.catToken();
                    reader.getChar();
                }
                reader.retract();
                SYMBOL resultValue = reserve(reader.getToken().toString());
                if (resultValue == SYMBOL.UNKNOWN)
                    symbol = SYMBOL.IDENFR;
                else
                    symbol = resultValue;
            }
            /* 常整数, 不允许前置零 */
            else if (reader.isDigit()) {
                // 非零整数
                if (reader.isNZeroDigit()) {
                    while (reader.isDigit()) {
                        reader.catToken();
                        reader.getChar();
                    }
                    reader.retract();
                    symbol = SYMBOL.INTCON;
                }
                // 零
                else {
                    reader.catToken();
                    reader.getChar();
                    if (!reader.isDigit()) {
                        reader.retract();
                        symbol = SYMBOL.INTCON;
                    } else
                        Error.lexError(reader.getCurLine(), SYMBOL.INTCON);
                }
            }
            /* 双分界符 */
            else if (reader.isLss()) {
                // < <=
                reader.catToken();
                reader.getChar();
                if (reader.isEqu()) {
                    reader.catToken();
                    symbol = SYMBOL.LEQ;
                } else {
                    reader.retract();
                    symbol = SYMBOL.LSS;
                }
            } else if (reader.isGre()) {
                // > >=
                reader.catToken();
                reader.getChar();
                if (reader.isEqu()) {
                    reader.catToken();
                    symbol = SYMBOL.GEQ;
                } else {
                    reader.retract();
                    symbol = SYMBOL.GRE;
                }
            } else if (reader.isEqu()) {
                // = ==
                reader.catToken();
                reader.getChar();
                if (reader.isEqu()) {
                    reader.catToken();
                    symbol = SYMBOL.EQL;
                } else {
                    reader.retract();
                    symbol = SYMBOL.ASSIGN;
                }
            } else if (reader.isExclamation()) {
                // ! !=
                reader.catToken();
                reader.getChar();
                if (reader.isEqu()) {
                    reader.catToken();
                    symbol = SYMBOL.NEQ;
                } else {
                    reader.retract();
                    symbol = SYMBOL.NOT;
                }
            } else if (reader.isAnd()) {
                // &&
                reader.catToken();
                reader.getChar();
                if (reader.isAnd()) {
                    reader.catToken();
                    symbol = SYMBOL.AND;
                } else {
                    Error.lexError(reader.getCurLine(), SYMBOL.UNKNOWN);
                }
            } else if (reader.isOr()) {
                // for ||
                reader.catToken();
                reader.getChar();
                if (reader.isOr()) {
                    reader.catToken();
                    symbol = SYMBOL.OR;
                } else {
                    Error.lexError(reader.getCurLine(), SYMBOL.UNKNOWN);
                }
            }
            /* 单分界符号 */
            else if (reader.isPlus()) {
                reader.catToken();
                symbol = SYMBOL.PLUS;
            } else if (reader.isMinus()) {
                reader.catToken();
                symbol = SYMBOL.MINU;
            } else if (reader.isMulti()) {
                reader.catToken();
                symbol = SYMBOL.MULT;
            } else if (reader.isDiv()) {
                reader.catToken();
                reader.getChar();
                // 进入单行注释
                if (reader.isDiv()) {
                    // 仅计 \n
                    while (!reader.isNewlineN()) {
                        reader.getChar();
                    }
                    reader.pushLine();   // 进行换行处理
                    symbol = SYMBOL.IGNORE;
                }
                // 进入多行注释
                else if (reader.isMulti()) {
                    reader.getChar();
                    while (true) {
                        while (!reader.isMulti()) {
                            // 仅计 \n
                            if (reader.isNewlineN()) {
                                reader.pushLine();
                            }
                            reader.getChar();
                        }
                        reader.getChar();
                        if (reader.isDiv()) {
                            break;
                        } else {
                            continue;
                        }
                    }
                    symbol = SYMBOL.IGNORE;
                    // 多行注释的最后一个换行符留到了下一次循环进行处理
                } else {
                    reader.retract();
                    symbol = SYMBOL.DIV;
                }
            } else if (reader.isMod()) {
                reader.catToken();
                symbol = SYMBOL.MOD;
            } else if (reader.isSemi()) {
                reader.catToken();
                symbol = SYMBOL.SEMICN;
            } else if (reader.isComma()) {
                reader.catToken();
                symbol = SYMBOL.COMMA;
            } else if (reader.isLeftParent()) {
                reader.catToken();
                symbol = SYMBOL.LPARENT;
            } else if (reader.isRightParent()) {
                reader.catToken();
                symbol = SYMBOL.RPARENT;
            } else if (reader.isLeftBracket()) {
                reader.catToken();
                symbol = SYMBOL.LBRACK;
            } else if (reader.isRightBracket()) {
                reader.catToken();
                symbol = SYMBOL.RBRACK;
            } else if (reader.isLeftBrace()) {
                reader.catToken();
                symbol = SYMBOL.LBRACE;
            } else if (reader.isRightBrace()) {
                reader.catToken();
                symbol = SYMBOL.RBRACE;
            }
            /* 字符串 */
            else if (reader.isDouQuotation()) {
                reader.catToken();
                reader.getChar();
                while (!reader.isDouQuotation()) {
                    reader.catToken();
                    reader.getChar();
                }
                if (reader.isDouQuotation()) {
                    reader.catToken();
                    symbol = SYMBOL.STRCON;
                } else {
                    Error.lexError(reader.getCurLine(), SYMBOL.STRCON);
                }
            } else if (reader.isEOT()) {
                break;
            } else {
                Error.lexError(reader.getCurLine(), SYMBOL.UNKNOWN);
            }

            if (symbol != SYMBOL.IGNORE) {
                this.tokenList.add(new Token(symbol, reader.getToken().toString(), reader.getCurLine()));
            }

            reader.getChar();
        }
        return this.tokenList;
    }

    public SYMBOL reserve(String value) {
        if (value.equals("const")) return SYMBOL.CONSTTK;
        else if (value.equals("int")) return SYMBOL.INTTK;
        else if (value.equals("void")) return SYMBOL.VOIDTK;
        else if (value.equals("main")) return SYMBOL.MAINTK;
        else if (value.equals("if")) return SYMBOL.IFTK;
        else if (value.equals("else")) return SYMBOL.ELSETK;
        else if (value.equals("while")) return SYMBOL.WHILETK;
        else if (value.equals("printf")) return SYMBOL.PRINTFTK;
        else if (value.equals("return")) return SYMBOL.RETURNTK;
        else if (value.equals("break")) return SYMBOL.BREAKTK;
        else if (value.equals("continue")) return SYMBOL.CONTINUETK;
        else if (value.equals("getint")) return SYMBOL.GETINTTK;
        else return SYMBOL.UNKNOWN;
    }
}
