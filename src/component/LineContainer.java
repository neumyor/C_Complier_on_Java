package component;

import java.util.ArrayList;

public class LineContainer {
    private ArrayList<String> contain;

    public LineContainer() {
        this.contain = new ArrayList<>();
    }

    public void addLine(String l) {
        this.contain.add(l + "\n");
    }

    public String dump() {
        StringBuilder ret = new StringBuilder();
        for (String l : contain) {
            ret.append(l);
        }
        return ret.toString();
    }
}
