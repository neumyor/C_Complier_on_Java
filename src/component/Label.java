package component;

import java.util.Objects;

/**
 * Label 标签类
 * 你可以把他理解成String类这种全局的类
 * 把它放在component下感觉比较合理（笑
 */
public class Label {
    public String labelName;
    public Label(String name){
        this.labelName = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Objects.equals(labelName, label.labelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelName);
    }
}
