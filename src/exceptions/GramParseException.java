package exceptions;


import component.GUNIT;
import component.Token;

public class GramParseException extends Exception {
    String mess;
    private GramParseException from;
    private Token curToken;
    private int level;

    public GramParseException(String mess) {
        this.from = null;
        this.mess = mess;
    }

    public GramParseException(GramParseException e, Token curToken, int level, GUNIT curGUnit) {
        this.from = e;
        this.curToken = curToken;
        this.level = level;
        this.mess = "Fail to execute grammar parse for LINE:" + curToken.getLine() +
                " VALUE:" + curToken.getValue() + "\tin LEVEL: " + level + "\tGUNIT: "+curGUnit;
    }

    @Override
    public String getMessage() {
        return this.mess;
    }

    public GramParseException getFrom() {
        return from;
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < level; i++) {
            temp.append(' ');
        }
        temp.append(curToken.getLine() + " " + curToken.getValue() + " in level:" + level + "\t" + mess +"\n");
        return temp.toString();
    }

    public Token getCurToken() {
        return curToken;
    }
}
