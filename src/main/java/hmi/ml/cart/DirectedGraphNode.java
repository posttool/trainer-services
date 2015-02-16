package hmi.ml.cart;

import hmi.ml.feature.FeatureVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A type of node that can be at the same time a decision node and a leaf node,
 * and that can have more than one parent. Other than tree nodes, thus, directed
 * graph nodes are not necessarily contained in a strict tree structure;
 * furthermore, each node can potentially carry data.
 *
 */
public class DirectedGraphNode extends Node {

	private DecisionNode decisionNode;
	private Node leafNode;

	private Map<Node, Integer> parentToIdx = new HashMap<Node, Integer>();
	private List<Node> parents = new ArrayList<Node>();
	private int uniqueID;

	/**
     * 
     */
	public DirectedGraphNode(DecisionNode decisionNode, Node leafNode) {
		setDecisionNode(decisionNode);
		setLeafNode(leafNode);
	}

	public DecisionNode getDecisionNode() {
		return decisionNode;
	}

	@Override
	public boolean isDirectedGraphNode() {
		return true;
	}

	public void setDecisionNode(DecisionNode newNode) {
		this.decisionNode = newNode;
		if (newNode != null)
			newNode.setParent(this, 0);
	}

	public Node getLeafNode() {
		return leafNode;
	}

	public void setLeafNode(Node newNode) {
		if (!(newNode == null || newNode instanceof DirectedGraphNode || newNode instanceof LeafNode)) {
			throw new IllegalArgumentException("Only leaf nodes and directed graph nodes allowed as leafNode");
		}
		this.leafNode = newNode;
		if (newNode != null)
			newNode.setParent(this, 0);
	}

	@Override
	public void setParent(Node node, int nodeIndex) {
		parents.add(node);
		parentToIdx.put(node, nodeIndex);
	}

	/**
	 * Get a parent node of this node. DirectedGraphNodes can have more than one
	 * node.
	 * 
	 * @return the first parent, or null if there is no parent.
	 */
	public Node getParent() {
		if (parents.isEmpty())
			return null;
		return parents.get(0);
	}

	/**
	 * Get the index of this node in the parent returned by getParent().
	 * 
	 * @return the index in the parent's children array, or 0 if there is no
	 *         parent.
	 */
	public int getNodeIndex() {
		Node firstParent = getParent();
		if (firstParent != null)
			return parentToIdx.get(firstParent);
		return 0;
	}

	public List<Node> getParents() {
		return parents;
	}

	/**
	 * Return this node's index in the given parent's array of children.
	 * 
	 * @param aParent
	 * @return
	 * @throws IllegalArgumentException
	 *             if parent is not a parent of this node.
	 */
	public int getNodeIndex(Node aParent) {
		if (!parentToIdx.containsKey(aParent))
			throw new IllegalArgumentException("The given node is not a parent of this node");
		return parentToIdx.get(aParent);
	}

	/**
	 * Remove the given node from the list of parents.
	 * 
	 * @param aParent
	 * @throws IllegalArgumentException
	 *             if parent is not a parent of this node.
	 */
	public void removeParent(Node aParent) {
		if (!parentToIdx.containsKey(aParent))
			throw new IllegalArgumentException("The given node is not a parent of this node");
		parentToIdx.remove(aParent);
		parents.remove(aParent);
	}

	@Override
	protected void fillData(Object target, int pos, int len) {
		if (leafNode != null)
			leafNode.fillData(target, pos, len);
	}

	@Override
	public Object getAllData() {
		if (leafNode != null)
			return leafNode.getAllData();
		else if (decisionNode != null)
			return decisionNode.getAllData();
		return null;
	}

	@Override
	public int getNumberOfData() {
		if (leafNode != null)
			return leafNode.getNumberOfData();
		else if (decisionNode != null)
			return decisionNode.getNumberOfData();
		return 0;
	}

	@Override
	public int getNumberOfNodes() {
		if (decisionNode != null)
			return decisionNode.getNumberOfNodes();
		return 0;
	}

	public Node getNextNode(FeatureVector fv) {
		if (decisionNode != null) {
			Node next = decisionNode.getNextNode(fv);
			if (next != null)
				return next;
		}
		return leafNode;
	}

	public int getUniqueGraphNodeID() {
		return uniqueID;
	}

	public void setUniqueGraphNodeID(int id) {
		this.uniqueID = id;
	}

	public String getDecisionPath() {
		StringBuilder ancestorInfo = new StringBuilder();
		if (getParents().size() == 0)
			ancestorInfo.append("null");
		for (Node mum : getParents()) {
			if (ancestorInfo.length() > 0) {
				ancestorInfo.append(" or\n");
			}
			if (mum.isDecisionNode()) {
				ancestorInfo.append(((DecisionNode) mum).getDecisionPath(getNodeIndex()));
			} else {
				ancestorInfo.append(mum.getDecisionPath());
			}
		}
		return ancestorInfo + " - " + toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DGN");
		sb.append(uniqueID);
		if (parentToIdx.size() > 1) {
			sb.append(" (").append(parentToIdx.size()).append(" parents)");
		}
		return sb.toString();
	}
}
