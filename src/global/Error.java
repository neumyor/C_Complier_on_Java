package global;

import component.SYMBOL;

/**
 * Error是工具类，用于提供所有错误处理的方法
 * 其可以调用Logger打印日志，也可以同时向控制台进行输出
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class Error {
    public static void lexError(int line, SYMBOL symbol) {
        String message = "LexParseError happened when parsing " + symbol.toString() + " in line " + line;
        System.out.println(message);
    }

    public static void grammarError(int line, String abStr) {
        String message = "GrammarParseError happened when parsing \"" + abStr + "\" in line " + line;
        System.out.println(message);
    }

    public static String formatStringError(int line) {
        return (line + " a");
    }

    public static void warning(String message) {
        System.out.println("[Warning]: " + message);throw new RuntimeException("[Warning]: " + message);
    }

    public static String duplicatedDefinitionError(int line) {
        return (line + " b");
    }

    public static String undefinedError(int line) {
        return (line + " c");
    }

    public static String parameterAmountError(int line) {
        return (line + " d");
    }

    public static String parameterTypeError(int line) {
        return (line + " e");
    }

    public static String voidFuncButReturnError(int line) {
        return (line + " f");
    }

    public static String intFuncButNoReturnError(int line) {
        return (line + " g");
    }

    public static String modifiedConstError(int line) {
        return (line + " h");
    }

    public static String semiconMissingError(int line) {
        return (line + " i");
    }

    public static String leftParentMissingError(int line) {
        return (line + " j");
    }

    public static String leftBraketMissingError(int line) {
        return (line + " k");
    }

    public static String printfExpAmountError(int line) {
        return (line + " l");
    }

    public static String breakContinueError(int line) {
        return (line + " m");
    }
}
