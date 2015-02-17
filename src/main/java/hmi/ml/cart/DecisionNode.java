package hmi.ml.cart;

import hmi.ml.cart.LeafNode.FeatureVectorLeafNode;
import hmi.ml.cart.LeafNode.IntArrayLeafNode;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;

/**
 * A decision node that determines the next Node to go to in the CART. All
 * decision nodes inherit from this class
 */
public abstract class DecisionNode extends Node {

    public abstract String getNodeDefinition();

    public abstract Type getDecisionNodeType();

    public abstract Node getNextNode(FeatureVector featureVector);

    public enum Type {
        BinaryByteDecisionNode, BinaryShortDecisionNode, BinaryFloatDecisionNode, ByteDecisionNode, ShortDecisionNode
    };

    protected boolean TRACE = false;
    // for debugging:
    protected FeatureDefinition featureDefinition;

    protected Node[] children;

    protected int featureIndex;

    // the feature name
    protected String feature;

    // remember last added child
    protected int lastChild;

    // the total number of data in the leaves below this node
    protected int nData;

    // unique index
    protected int uniqueDecisionNodeId;

    public DecisionNode(String feature, int numChildren, FeatureDefinition featureDefinition) {
        this.feature = feature;
        this.featureIndex = featureDefinition.getFeatureIndex(feature);
        children = new Node[numChildren];
        isRoot = false;
        // for trace and getDecisionPath():
        this.featureDefinition = featureDefinition;
    }

    public DecisionNode(int featureIndex, int numChildren, FeatureDefinition featureDefinition) {
        this.featureIndex = featureIndex;
        this.feature = featureDefinition.getFeatureName(featureIndex);
        children = new Node[numChildren];
        isRoot = false;
        this.featureDefinition = featureDefinition;
    }

    public DecisionNode(int numChildren, FeatureDefinition featureDefinition) {
        children = new Node[numChildren];
        isRoot = false;
        this.featureDefinition = featureDefinition;
    }

    @Override
    public boolean isDecisionNode() {
        return true;
    }

    public String getFeatureName() {
        return feature;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public FeatureDefinition getFeatureDefinition() {
        return featureDefinition;
    }

    public void addChild(Node child) {
        if (lastChild > children.length - 1) {
            throw new RuntimeException("Can not add child number " + (lastChild + 1) + ", since node has only "
                    + children.length + " children!");
        }
        children[lastChild] = child;
        if (child != null) {
            child.setParent(this, lastChild);
        }
        lastChild++;
        // does it hurt performance to countData() here?
        // TODO profile while reading large file
    }

    public Node getChild(int index) {
        if (index > children.length - 1 || index < 0) {
            return null;
        }
        return children[index];
    }

    public void replaceChild(Node newChild, int index) {
        if (index > children.length - 1 || index < 0) {
            throw new RuntimeException("Can not replace child number " + index + ", since child index goes from 0 to "
                    + (children.length - 1) + "!");
        }
        children[index] = newChild;
        newChild.setParent(this, index);
    }

    public boolean hasMoreChildren(int index) {
        return (index > -1 && index < children.length);
    }

    public Object getAllData() {
        LeafNode firstLeaf = new NodeIterator<LeafNode>(this, true, false, false).next();
        if (firstLeaf == null)
            return null;
        Object result;
        if (firstLeaf instanceof IntArrayLeafNode) {
            result = new int[nData];
        } else if (firstLeaf instanceof FeatureVectorLeafNode) {
            result = new FeatureVector[nData];
        } else {
            return null;
        }
        fillData(result, 0, nData);
        return result;
    }

    protected void fillData(Object target, int pos, int total) {
        // assert pos + total <= target.length;
        for (int i = 0; i < children.length; i++) {
            if (children[i] == null)
                continue;
            int len = children[i].getNumberOfData();
            children[i].fillData(target, pos, len);
            pos += len;
        }
    }

    public int getNumberOfNodes() {
        int nNodes = 1; // this node
        for (int i = 0; i < children.length; i++) {
            if (children[i] != null)
                nNodes += children[i].getNumberOfNodes();
        }
        return nNodes;
    }

    public int getNumberOfData() {
        return nData;
    }

    public int getNumberOfChildren() {
        return children.length;
    }

    /**
     * Set the number of candidates correctly, by counting while walking down
     * the tree. This needs to be done once for the entire tree.
     * 
     */
    public void countData() {
        nData = 0;
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof DecisionNode)
                ((DecisionNode) children[i]).countData();
            if (children[i] != null) {
                nData += children[i].getNumberOfData();
            }
        }
    }

    public String toString() {
        return "dn" + uniqueDecisionNodeId;
    }

    /**
     * Get the path leading to the child with the given index. This will
     * recursively go up to the root node.
     */
    public abstract String getDecisionPath(int childIdx);

    public void setUniqueDecisionNodeId(int id) {
        this.uniqueDecisionNodeId = id;
    }

    public int getUniqueDecisionNodeId() {
        return uniqueDecisionNodeId;
    }

    // DECISION NODES

    public static class BinaryByteDecisionNode extends DecisionNode {

        private byte value;

        public BinaryByteDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = featureDefinition.getFeatureValueAsByte(feature, value);
        }

        public BinaryByteDecisionNode(int featureIndex, byte value, FeatureDefinition featureDefinition) {
            super(featureIndex, 2, featureDefinition);
            this.value = value;
        }

        public BinaryByteDecisionNode(int uniqueId, FeatureDefinition featureDefinition) {
            super(2, featureDefinition);
            // System.out.println("adding decision node: " + uniqueId);
            this.uniqueDecisionNodeId = uniqueId;
        }

        public void setFeatureAndFeatureValue(String feature, String value) {
            this.feature = feature;
            this.featureIndex = featureDefinition.getFeatureIndex(feature);
            this.value = featureDefinition.getFeatureValueAsByte(feature, value);
        }

        public byte getCriterionValueAsByte() {
            return value;
        }

        public String getCriterionValueAsString() {
            return featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = children[0];
            } else {
                returnNode = children[1];
            }
            if (TRACE) {
                System.out.print("    " + feature + ": "
                        + featureDefinition.getFeatureValueAsString(featureIndex, value) + " == "
                        + featureDefinition.getFeatureValueAsString(featureIndex, val));
                if (val == value)
                    System.out.println(" YES ");
                else
                    System.out.println(" NO ");
            }
            return returnNode;
        }

        public String getDecisionPath(int childIdx) {
            String thisNodeInfo;
            if (childIdx == 0)
                thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            else
                thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            if (parent == null)
                return thisNodeInfo;
            else if (parent.isDecisionNode())
                return ((DecisionNode) parent).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
            else
                return parent.getDecisionPath() + " - " + thisNodeInfo;
        }

        public String getNodeDefinition() {
            return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

        public Type getDecisionNodeType() {
            return Type.BinaryByteDecisionNode;
        }

    }

    public static class BinaryShortDecisionNode extends DecisionNode {

        private short value;

        public BinaryShortDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = featureDefinition.getFeatureValueAsShort(feature, value);
        }

        public BinaryShortDecisionNode(int featureIndex, short value, FeatureDefinition featureDefinition) {
            super(featureIndex, 2, featureDefinition);
            this.value = value;
        }

        public short getCriterionValueAsShort() {
            return value;
        }

        public String getCriterionValueAsString() {
            return featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = children[0];
            } else {
                returnNode = children[1];
            }
            if (TRACE) {
                System.out.print(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
                if (val == value)
                    System.out.print(" == ");
                else
                    System.out.print(" != ");
                System.out.println(featureDefinition.getFeatureValueAsString(featureIndex, value));
            }
            return returnNode;
        }

        public String getDecisionPath(int childIdx) {
            String thisNodeInfo;
            if (childIdx == 0)
                thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            else
                thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            if (parent == null)
                return thisNodeInfo;
            else if (parent.isDecisionNode())
                return ((DecisionNode) parent).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
            else
                return parent.getDecisionPath() + " - " + thisNodeInfo;
        }

        public String getNodeDefinition() {
            return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

        public Type getDecisionNodeType() {
            return Type.BinaryShortDecisionNode;
        }

    }

    public static class BinaryFloatDecisionNode extends DecisionNode {

        private float value;
        private boolean isByteFeature;

        public BinaryFloatDecisionNode(int featureIndex, float value, FeatureDefinition featureDefinition) {
            this(featureDefinition.getFeatureName(featureIndex), value, featureDefinition);
        }

        public BinaryFloatDecisionNode(String feature, float value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = value;
            // check for pseudo-floats:
            // TODO: clean this up:
            if (featureDefinition.isByteFeature(featureIndex))
                isByteFeature = true;
            else
                isByteFeature = false;
        }

        public float getCriterionValueAsFloat() {
            return value;
        }

        public String getCriterionValueAsString() {
            return String.valueOf(value);
        }

        public Node getNextNode(FeatureVector featureVector) {
            float val;
            if (isByteFeature)
                val = (float) featureVector.getByteFeature(featureIndex);
            else
                val = featureVector.getContinuousFeature(featureIndex);
            Node returnNode;
            if (val < value) {
                returnNode = children[0];
            } else {
                returnNode = children[1];
            }
            if (TRACE) {
                System.out.print(feature + ": " + val);
                if (val < value)
                    System.out.print(" < ");
                else
                    System.out.print(" >= ");
                System.out.println(value);
            }

            return returnNode;
        }

        public String getDecisionPath(int childIdx) {
            String thisNodeInfo;
            if (childIdx == 0)
                thisNodeInfo = feature + "<" + value;
            else
                thisNodeInfo = feature + ">=" + value;
            if (parent == null)
                return thisNodeInfo;
            else if (parent.isDecisionNode())
                return ((DecisionNode) parent).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
            else
                return parent.getDecisionPath() + " - " + thisNodeInfo;
        }

        public String getNodeDefinition() {
            return feature + " < " + value;
        }

        public Type getDecisionNodeType() {
            return Type.BinaryFloatDecisionNode;
        }

    }

    public static class ByteDecisionNode extends DecisionNode {

        public ByteDecisionNode(String feature, int numChildren, FeatureDefinition featureDefinition) {
            super(feature, numChildren, featureDefinition);
        }

        public ByteDecisionNode(int featureIndex, int numChildren, FeatureDefinition featureDefinition) {
            super(featureIndex, numChildren, featureDefinition);
        }

        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            if (TRACE) {
                System.out.println(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
            }
            return children[val];
        }

        public String getDecisionPath(int childIdx) {
            String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, childIdx);
            if (parent == null)
                return thisNodeInfo;
            else if (parent.isDecisionNode())
                return ((DecisionNode) parent).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
            else
                return parent.getDecisionPath() + " - " + thisNodeInfo;
        }

        public String getNodeDefinition() {
            return feature + " isByteOf " + children.length;
        }

        public Type getDecisionNodeType() {
            return Type.ByteDecisionNode;
        }

    }

    public static class ShortDecisionNode extends DecisionNode {

        public ShortDecisionNode(String feature, int numChildren, FeatureDefinition featureDefinition) {
            super(feature, numChildren, featureDefinition);
        }

        public ShortDecisionNode(int featureIndex, int numChildren, FeatureDefinition featureDefinition) {
            super(featureIndex, numChildren, featureDefinition);
        }

        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            if (TRACE) {
                System.out.println(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
            }
            return children[val];
        }

        public String getDecisionPath(int childIdx) {
            String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, childIdx);
            if (parent == null)
                return thisNodeInfo;
            else if (parent.isDecisionNode())
                return ((DecisionNode) parent).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
            else
                return parent.getDecisionPath() + " - " + thisNodeInfo;
        }

        public String getNodeDefinition() {
            return feature + " isShortOf " + children.length;
        }

        public Type getDecisionNodeType() {
            return Type.ShortDecisionNode;
        }

    }

}
