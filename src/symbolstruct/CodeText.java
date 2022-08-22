package symbolstruct;

import component.LineContainer;

public class CodeText {
    private LineContainer dataText;
    private LineContainer textText;
    private static CodeText instance;

    private CodeText(){
        this.dataText = new LineContainer();
        this.textText = new LineContainer();
    }

    private static CodeText getInstance(){
        if(instance == null){
            instance = new CodeText();
        }
        return instance;
    }

    /**
     * 向.text区写入一行
     * @param in
     */
    public static void textNLine(String in) {
        CodeText.getInstance().textText.addLine(in);
    }

    /**
     * 向.data区写入一行
     * @param in
     */
    public static void dataNLine(String in) {
        CodeText.getInstance().dataText.addLine(in);
    }

    public static String dumpText() {
        return CodeText.getInstance().textText.dump();
    }

    public static String dumpData() {
        return CodeText.getInstance().dataText.dump();
    }
}
