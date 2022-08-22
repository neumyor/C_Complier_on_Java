package component;

/**
 * component.Token 词类
 * 管理当前词的内容（字符串形式）、词法类型和所在行
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class Token {
    private SYMBOL symbol;
    private String value;
    private Integer line;

    public Token(SYMBOL symbol, String value, Integer line) {
        this.symbol = symbol;
        this.value = value;
        this.line = line;
    }

    public Integer getLine() {
        return line.intValue();
    }

    public String getValue() {
        return value;
    }

    public SYMBOL getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return this.line + ": " +
                this.symbol + " " + this.value;
    }
}
