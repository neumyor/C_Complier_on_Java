import ast.Node;
import component.Token;
import global.Config;
import global.Logger;
import symbolstruct.CodeRegion;

import java.util.List;

/**
 * Compiler 入口主类
 */
public class Compiler {
    public static void main(String[] args) {
        LexParser lexParser = new LexParser(Config.inputFilePath);
        List<Token> tokenList = lexParser.parse();
        tokenList.stream().forEach(token -> Logger.GetLogger().LexParserLog(token.getSymbol(), token.getValue()));
        GrammarParser gramParser = new GrammarParser(tokenList);
        Node astRoot = gramParser.parse();
//        Visitor visitor = new Visitor(astRoot);
//        visitor.visit();
        Converter converter = new Converter(astRoot);
        CodeRegion codeRegion = converter.visit();
        Logger.GetLogger().DumpError();
    }
}
