package hmi.ml.cart;

import java.util.Properties;

import weka.classifiers.trees.j48.C45PruneableClassifierTreeWithUnary;
import weka.classifiers.trees.j48.TreeConverter;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;

/**
 * Classification and Regression Tree
 * 
 * @see C45PruneableClassifierTreeWithUnary
 * @see TreeConverter
 *
 */
public class CART extends DirectedGraph {

    public CART() {
    }

    public CART(FeatureDefinition featDef) {
        super(featDef);
    }

    public CART(Node rootNode, FeatureDefinition featDef) {
        super(rootNode, featDef);
    }

    public CART(Node rootNode, FeatureDefinition featDef, Properties properties) {
        super(rootNode, featDef, properties);
    }

    // public Node interpretToNode(Target target, int minNumberOfData) {
    // return interpretToNode(target.getFeatureVector(), minNumberOfData);
    // }

    /**
     * Passes the given item through this CART and returns the leaf Node, or the
     * Node it stopped walking down.
     */
    public Node interpretToNode(FeatureVector featureVector, int minNumberOfData) {
        Node currentNode = rootNode;
        Node prevNode = null;

        while (currentNode != null && currentNode.getNumberOfData() > minNumberOfData
                && !(currentNode instanceof LeafNode)) {
            // while we have not reached the bottom,
            // get the next node based on the features of the target
            prevNode = currentNode;
            currentNode = ((DecisionNode) currentNode).getNextNode(featureVector);
            //System.out.println(currentNode.toString() );
            // decision.findFeature(item) + "' => "+ nodeIndex);
        }
        // Now usually we will have gone down one level too far
        if (currentNode == null || currentNode.getNumberOfData() < minNumberOfData && prevNode != null) {
            currentNode = prevNode;
        }

        assert currentNode.getNumberOfData() >= minNumberOfData || currentNode == rootNode;

        assert minNumberOfData > 0 || (currentNode instanceof LeafNode);
        return currentNode;

    }

    // public Object interpret(Target target, int minNumberOfData) {
    //
    // // get the indices from the leaf node
    // Object result = this.interpretToNode(target,
    // minNumberOfData).getAllData();
    //
    // return result;
    //
    // }

    public static Node replaceLeafByCart(CART cart, LeafNode leaf) {
        DecisionNode parent = (DecisionNode) leaf.getParent();
        Node newNode = cart.getRootNode();
        parent.replaceChild(newNode, leaf.getNodeIndex());
        newNode.setIsRoot(false);
        return newNode;
    }

}
