package component.graph;

import mips.register.Register;
import symbolstruct.entries.Entry;

import java.util.HashSet;
import java.util.Objects;

public class Node {
    public HashSet<Node> connects;
    public Entry entry;
    public Register reg;

    public Node(Entry entry) {
        this.connects = new HashSet<>();
        this.entry = entry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(entry, node.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry);
    }
}
