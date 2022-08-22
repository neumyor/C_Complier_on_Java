import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * FileReader是用于读取文件的类，其中封装了对于 文件指针的管理 和 字符类型的判断
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class FileReader {
    private String inputFilePath;
    private StringBuffer token;
    private StringBuffer text;
    private Integer ptr;
    private Integer curLine;
    private Character curChar;

    /**
     * 初始化函数
     *
     * @param inputFilePath：源程序文件存储路径
     */
    public FileReader(String inputFilePath) {
        this.inputFilePath = inputFilePath;

        this.text = new StringBuffer();
        try {
            File file = new File(inputFilePath);
            Reader reader = new InputStreamReader(new FileInputStream(file));
            int tempchar;
            while ((tempchar = reader.read()) != -1) {
                this.text.append((char) tempchar);
            }
            this.text.append((char) 0);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.text.append('\n');
        this.text.append((char)0);
        this.ptr = 0;
        this.curLine = 1;
        this.token = new StringBuffer();
    }

    /**
     * 将行号加一
     */
    public void pushLine() {
        this.curLine++;
    }

    /**
     * 将行号后退一
     */
    public void retractLine() {
        this.curLine--;
        if (this.curLine < 0) throw new RuntimeException("retract line unavailable");
    }

    /**
     * 返回当前指针所在行
     *
     * @return 行号，用Integer表示
     */
    public Integer getCurLine() {
        return curLine;
    }

    public void catToken() {
        this.token.append(this.text.charAt(ptr));
    }

    public void clearToken() {
        this.token = new StringBuffer();
    }

    public void getChar() {
        this.ptr++;
        this.curChar = this.text.charAt(this.ptr);
    }

    public void retract() {
        this.ptr--;
        if (this.ptr < 0) throw new RuntimeException("retract ptr unavailable");
    }

    public StringBuffer getToken() {
        return this.token;
    }

    public StringBuffer getText() {
        return this.text;
    }

    /**
     * 当前ptr字符是否是空格
     *
     * @return 是或否
     */
    public Boolean isSpace() {
        int temp = (int) this.text.charAt(ptr);
        return (temp <= 15 && temp >= 11) || temp == 32;
    }

    /**
     * 当前ptr字符是否是换行符\n或\r
     *
     * @return 是或否
     */
    public Boolean isNewlineN() {
        return this.text.charAt(ptr) == '\n';
    }

    /**
     * 当前ptr字符是否是换行符\r
     *
     * @return 是或否
     */
    public Boolean isNewlineR() {
        return this.text.charAt(ptr) == '\r';
    }

    /**
     * 当前ptr字符是否是Tab
     *
     * @return 是或否
     */
    public Boolean isTab() {
        return this.text.charAt(ptr) == '\t';
    }

    /**
     * 当前ptr字符是否是字母
     *
     * @return 是或否
     */
    public Boolean isLetter() {
        return (this.text.charAt(ptr) >= 'a' && this.text.charAt(ptr) <= 'z')
                || (this.text.charAt(ptr) >= 'A' && (this.text.charAt(ptr) <= 'Z'))
                || this.text.charAt(ptr) == '_';
    }

    /**
     * 当前ptr字符是否是数字0~9
     *
     * @return 是或否
     */
    public Boolean isDigit() {
        return this.text.charAt(ptr) >= '0' && this.text.charAt(ptr) <= '9';
    }

    /**
     * 当前ptr字符是否是非零数字
     *
     * @return 是或否
     */
    public Boolean isNZeroDigit() {
        return this.text.charAt(ptr) >= '1' && this.text.charAt(ptr) <= '9';
    }

    /**
     * 当前ptr字符是否是逗号
     *
     * @return 是或否
     */
    public Boolean isComma() {
        return this.text.charAt(ptr) == ',';
    }

    /**
     * 当前ptr字符是否是分号
     *
     * @return 是或否
     */
    public Boolean isSemi() {
        return this.text.charAt(ptr) == ';';
    }

    /**
     * 当前ptr字符是否是等号
     *
     * @return 是或否
     */
    public Boolean isEqu() {
        return this.text.charAt(ptr) == '=';
    }

    /**
     * 当前ptr字符是否是加号
     *
     * @return 是或否
     */
    public Boolean isPlus() {
        return this.text.charAt(ptr) == '+';
    }

    /**
     * 当前ptr字符是否是减号
     *
     * @return 是或否
     */
    public Boolean isMinus() {
        return this.text.charAt(ptr) == '-';
    }

    /**
     * 当前ptr字符是否是除号
     *
     * @return 是或否
     */
    public Boolean isDiv() {
        return this.text.charAt(ptr) == '/';
    }

    /**
     * 当前ptr字符是否是乘号
     *
     * @return 是或否
     */
    public Boolean isMulti() {
        return this.text.charAt(ptr) == '*';
    }

    /**
     * 当前ptr字符是否是求模符号
     *
     * @return 是或否
     */
    public Boolean isMod() {
        return this.text.charAt(ptr) == '%';
    }

    /**
     * 当前ptr字符是否是&
     *
     * @return 是或否
     */
    public Boolean isAnd() {
        return this.text.charAt(ptr) == '&';
    }

    /**
     * 当前ptr字符是否是|
     *
     * @return 是或否
     */
    public Boolean isOr() {
        return this.text.charAt(ptr) == '|';
    }

    /**
     * 当前ptr字符是否是小于号
     *
     * @return 是或否
     */
    public Boolean isLss() {
        return this.text.charAt(ptr) == '<';
    }

    /**
     * 当前ptr字符是否是大于号
     *
     * @return 是或否
     */
    public Boolean isGre() {
        return this.text.charAt(ptr) == '>';
    }

    /**
     * 当前ptr字符是否是感叹号
     *
     * @return 是或否
     */
    public Boolean isExclamation() {
        return this.text.charAt(ptr) == '!';
    }

    /**
     * 当前ptr字符是否是左括号
     *
     * @return 是或否
     */
    public Boolean isLeftParent() {
        return this.text.charAt(ptr) == '(';
    }

    /**
     * 当前ptr字符是否是右括号
     *
     * @return 是或否
     */
    public Boolean isRightParent() {
        return this.text.charAt(ptr) == ')';
    }

    /**
     * 当前ptr字符是否是左中括号
     *
     * @return 是或否
     */
    public Boolean isLeftBracket() {
        return this.text.charAt(ptr) == '[';
    }

    /**
     * 当前ptr字符是否是右中括号
     *
     * @return 是或否
     */
    public Boolean isRightBracket() {
        return this.text.charAt(ptr) == ']';
    }

    /**
     * 当前ptr字符是否是左大括号
     *
     * @return 是或否
     */
    public Boolean isLeftBrace() {
        return this.text.charAt(ptr) == '{';
    }

    /**
     * 当前ptr字符是否是右大括号
     *
     * @return 是或否
     */
    public Boolean isRightBrace() {
        return this.text.charAt(ptr) == '}';
    }

    /**
     * 当前ptr字符是否是反斜杠
     *
     * @return 是或否
     */
    public Boolean isEscape() {
        return this.text.charAt(ptr) == '\\';
    }

    /**
     * 当前ptr字符是否是单引号
     *
     * @return 是或否
     */
    public Boolean isSinQuotation() {
        return this.text.charAt(ptr) == '\'';
    }

    /**
     * 当前ptr字符是否是双引号
     *
     * @return 是或否
     */
    public Boolean isDouQuotation() {
        return this.text.charAt(ptr) == '\"';
    }

    /**
     * 当前ptr字符是否是结束符号
     *
     * @return 是或否
     */
    public Boolean isEOT() {
        return (int) this.text.charAt(ptr) == 0;
    }
}
