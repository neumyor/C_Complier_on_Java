package ast;

import component.SYMBOL;
import component.Token;

import java.util.ArrayList;

public class LeafNode implements Node {
    private Node parent;
    private Integer level;
    private Token value;
    private SYMBOL type;
    private Boolean correct;

    public LeafNode(SYMBOL type, Token token, Boolean isCorrect) {
        this.parent = null;
        this.level = -1;
        this.value = token;
        this.correct = isCorrect;
        this.type = type;
    }

    @Override
    public void addChild(Node child) {
        System.err.println("IN LeafNode: Try to add child to a leaf node!");
    }

    @Override
    public void setParent(Node parent) {
        this.parent = parent;
        this.level = parent.getLevel() + 1;
    }

    @Override
    public ArrayList<Node> getChildren() {
        System.err.println("IN LeafNode: Try to get child from a leaf node!");
        return null;
    }

    @Override
    public Node getParent() {
        return this.parent;
    }

    public Token getToken() {
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

    public SYMBOL getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.name() + " " + this.getToken().getValue();
    }
}
