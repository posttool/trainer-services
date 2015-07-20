package hmi.ml.cart;


import java.util.ArrayList;
import java.util.List;

public class DecisionNodeX extends Node {
    int id;
    String feature;
    String value;
    List<Node> children;

    private DecisionNodeX() {
        id = -111111111;
    }

    public DecisionNodeX(int id) {
        this.id = id;
        this.children = new ArrayList<>();
        children.add(new DecisionNodeX());
        children.add(new DecisionNodeX());
    }

    public int getId() {
        return id;
    }

    public int getNumberOfChildren() {
        if (children == null)
            return 0;
        return children.size();
    }

    public Node getChild(int i) {
        return children.get(i);
    }

    public void replaceChild(Node newChild, int index) {
        Node c = children.get(index);
        children.add(index, newChild);
        children.remove(c);
        newChild.setParent(this, index);
    }

    public void setFeatureAndFeatureValue(String a, String b) {
        this.feature = a;
        this.value = b;
    }

    public void countData() {
    }

    public int getNumberOfNodes() {
        return 0;
    }

    public int getNumberOfData() {
        return 0;
    }

    public Object getAllData() {
        return null;
    }

    protected void fillData(Object target, int pos, int len) {

    }
}
