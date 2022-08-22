package global;

import component.SYMBOL;
import imcode.imexp.IMExp;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Logger是全局日志记录，遵循单例模式
 * 管理所有具体日志信息的输入格式和文件
 *
 * @author neumy
 * @version jdk1.8.0
 */
public class Logger {
    private static Logger logger;
    private String LexParserLogPath;
    private FileWriter LexParserLogWriter;
    private String GramParserLogPath;
    private FileWriter GramParserLogWriter;
    private String ErrorLogPath;
    private FileWriter ErrorLogWriter;
    private String SymbolLogPath;
    private FileWriter SymbolLogWriter;
    private ArrayList<ErrorMessage> ErrorLogBuffer;
    private String MIPSLogPath;
    private FileWriter MIPSLogWriter;
    private String IMExpLogPath;
    private FileWriter IMExpLogWriter;

    class ErrorMessage {
        Integer line;
        String message;

        ErrorMessage(Integer a, String message) {
            this.line = a;
            this.message = message;

        }
    }

    public static Logger GetLogger() {
        if (logger == null) {
            logger = new Logger();
        }
        return logger;
    }

    private Logger() {
        try {
            this.LexParserLogPath = Config.LexParserLogPath;
            this.LexParserLogWriter = new FileWriter(new File(this.LexParserLogPath));
            this.GramParserLogPath = Config.GramParserLogPath;
            this.GramParserLogWriter = new FileWriter(new File(this.GramParserLogPath));
            this.SymbolLogPath = Config.SymbolLogPath;
            this.SymbolLogWriter = new FileWriter(new File(this.SymbolLogPath));
            this.ErrorLogPath = Config.ErrorLogPath;
            this.ErrorLogWriter = new FileWriter(new File(this.ErrorLogPath));
            this.ErrorLogBuffer = new ArrayList<>();
            this.MIPSLogPath = Config.MIPSLogPath;
            this.MIPSLogWriter = new FileWriter(new File(this.MIPSLogPath));
            this.IMExpLogPath = Config.IMExpLogPath;
            this.IMExpLogWriter = new FileWriter(new File((this.IMExpLogPath)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ErrorLog(String string) {
        Integer line = Integer.valueOf(string.split(" ")[0]);
        ErrorLogBuffer.add(new ErrorMessage(line, string));
    }

    public void DumpError() {
        ErrorLogBuffer.sort(new Comparator<ErrorMessage>() {
            @Override
            public int compare(ErrorMessage o1, ErrorMessage o2) {
                if (o1.line >= o2.line) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        ErrorMessage former = null;
        for (ErrorMessage message : ErrorLogBuffer) {
            if (former != null) {
                if (former.line == message.line) {
                    continue;
                }
            }
            try {
                this.ErrorLogWriter.write(message.message + "\n");
                this.ErrorLogWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            former = message;
        }
    }

    public void LexParserLog(SYMBOL d, String value) {
        try {
            this.LexParserLogWriter.write(d.toString() + " " + value + "\n");
            this.LexParserLogWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void GrammarLog(String str) {
        try {
            this.GramParserLogWriter.write(str + "\n");
            this.GramParserLogWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SymbolLog(String str) {
        try {
            this.SymbolLogWriter.write(str + "\n");
            this.SymbolLogWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void MIPSLog(String str) {
        try {
            this.MIPSLogWriter.write(str + "\n");
            this.MIPSLogWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void IMExpLogPath(String exp) {
        try {
            this.IMExpLogWriter.write(exp + "\n");
            this.IMExpLogWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
