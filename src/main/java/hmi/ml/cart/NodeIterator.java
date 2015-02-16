package hmi.ml.cart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NodeIterator<T extends Node> implements Iterator<T> {
    private Node root;
    private Node current;
    private boolean showLeafNodes;
    private boolean showDecisionNodes;
    private boolean showDirectedGraphNodes;
    private Set<Node> alreadySeen = new HashSet<Node>();

    private Map<Node, Node> parentToChild = new HashMap<Node, Node>();

    protected NodeIterator(DirectedGraph graph, boolean showLeafNodes, boolean showDecisionNodes,
            boolean showDirectedGraphNodes) {
        this(graph.getRootNode(), showLeafNodes, showDecisionNodes, showDirectedGraphNodes);
    }

    protected NodeIterator(Node rootNode, boolean showLeafNodes, boolean showDecisionNodes,
            boolean showDirectedGraphNodes) {
        this.root = rootNode;
        this.showLeafNodes = showLeafNodes;
        this.showDecisionNodes = showDecisionNodes;
        this.showDirectedGraphNodes = showDirectedGraphNodes;
        this.current = root;
        alreadySeen.add(current);
        if (!currentIsSuitable()) {
            nextSuitableNodeDepthFirst();
        }
    }

    public boolean hasNext() {
        return current != null;
    }

    public T next() {
        T ret = (T) current;
        // and already prepare the current one
        nextSuitableNodeDepthFirst();
        return ret;
    }

    private boolean currentIsSuitable() {
        return (current == null || showDecisionNodes && current.isDecisionNode() || showLeafNodes
                && current.isLeafNode() || showDirectedGraphNodes && current.isDirectedGraphNode());
    }

    private void nextSuitableNodeDepthFirst() {
        do {
            nextNodeDepthFirst();
        } while (!currentIsSuitable());
    }

    private void nextNodeDepthFirst() {
        if (current == null)
            return;
        if (current.isDecisionNode()) {
            DecisionNode dec = (DecisionNode) current;
            for (int i = 0; i < dec.getNumberOfChildren(); i++) {
                Node child = dec.getChild(i);
                if (child == null)
                    continue;
                parentToChild.put(child, dec);
                if (unseenNode(dec.getChild(i)))
                    return;
            }
        } else if (current.isDirectedGraphNode()) {
            // Graph nodes return leaf child first, then decision child
            DirectedGraphNode g = (DirectedGraphNode) current;
            Node leaf = g.getLeafNode();
            if (leaf != null) {
                parentToChild.put(leaf, g);
                if (unseenNode(leaf))
                    return;
            }
            Node dec = g.getDecisionNode();
            if (dec != null) {
                parentToChild.put(dec, g);
                if (unseenNode(dec))
                    return;
            }
        }
        // If we didn't find a suitable child, we need to:
        backtrace();
    }

    private void backtrace() {
        // Only go back to parents we have come from.
        // This has two effects:
        // 1. We cannot go beyond root node;
        // 2. we don't risk to leave the subgraph defined by root node
        // in cases where we enter into a multi-parent node from a not-first
        // parent
        // (in such cases, getParent() would return the first parent).
        current = parentToChild.get(current);
        nextNodeDepthFirst();
    }

    /**
     * Test whether the given node is unseen. If so, move current to it, and
     * remember it as a seen node.
     */
    private boolean unseenNode(Node candidate) {
        if (candidate != null && !alreadySeen.contains(candidate)) {
            current = candidate;
            alreadySeen.add(current);
            return true;
        }
        return false;

    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot remove nodes using this iterator");
    }

}
