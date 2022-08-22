package component.narray;

public class NInt implements NArrayItem {
    private Integer value;

    public NInt(Integer value) {
        this.value = value;
    }

    @Override
    public Object instance() {
        return value;
    }
}
