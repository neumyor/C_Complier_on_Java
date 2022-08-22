package ast;

import component.GUNIT;
import component.Token;

import java.util.ArrayList;

public class BranchNode implements Node {
    private ArrayList<Node> children;
    private Node parent;
    private Integer level;
    private GUNIT value;
    private Boolean correct;

    public BranchNode(GUNIT gunit) {
        this.children = new ArrayList<>();
        this.level = -1;
        this.parent = null;
        this.value = gunit;
        this.correct = null;
    }

    @Override
    public void addChild(Node child) {
        this.children.add(child);
        child.setParent(this);
    }

    @Override
    public void setParent(Node parent) {
        this.parent = parent;
        this.level = parent.getLevel() + 1;
    }

    @Override
    public ArrayList<Node> getChildren() {
        return this.children;
    }

    @Override
    public Node getParent() {
        return this.parent;
    }

    public GUNIT getGUnit() {
        return value;
    }

    @Override
    public Boolean isCorrect() {
        return correct;
    }

    @Override
    public Integer getLevel() {
        return level;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
