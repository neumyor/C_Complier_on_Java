package ast;

import component.Token;

import java.util.ArrayList;

public interface Node {
    void addChild(Node child);
    void setParent(Node parent);
    ArrayList<Node> getChildren();
    Node getParent();
    Boolean isCorrect();
    Integer getLevel();
}
