package hmi.ml.cart;

public abstract class Node {

	/**
	 * Count all the nodes at and below this node. A leaf will return 1; the
	 * root node will report the total number of decision and leaf nodes in the
	 * tree.
	 * 
	 * @return
	 */
	public abstract int getNumberOfNodes();

	/**
	 * Count all the data available at and below this node. The meaning of this
	 * depends on the type of nodes; for example, when IntArrayLeafNodes are
	 * used, it is the total number of ints that are saved in all leaf nodes
	 * below the current node.
	 * 
	 * @return an int counting the data below the current node, or -1 if such a
	 *         concept is not meaningful.
	 */
	public abstract int getNumberOfData();

	/**
	 * Get all the data at or below this node. The type of data returned depends
	 * on the type of nodes; for example, when IntArrayLeafNodes are used, one
	 * int[] is returned which contains all int values in all leaf nodes below
	 * the current node.
	 * 
	 * @return an object containing all data below the current node, or null if
	 *         such a concept is not meaningful.
	 */
	public abstract Object getAllData();

	/**
	 * Write this node's data into the target object at pos, making sure that
	 * exactly len data are written. The type of data written depends on the
	 * type of nodes; for example, when IntArrayLeafNodes are used, target would
	 * be an int[].
	 * 
	 * @param array
	 *            the object to write to, usually an array.
	 * @param pos
	 *            the position in the target at which to start writing
	 * @param len
	 *            the amount of data items to write, usually equals
	 *            getNumberOfData().
	 */

	protected abstract void fillData(Object target, int pos, int len);

	protected boolean isRoot;
	protected Node parent;
	protected int nodeIndex;

	public void setParent(Node node, int nodeIndex) {
		this.parent = node;
		this.nodeIndex = nodeIndex;
	}

	public Node getParent() {
		return parent;
	}

	public int getNodeIndex() {
		return nodeIndex;
	}

	public void setIsRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public boolean isDecisionNode() {
		return false;
	}

	public boolean isLeafNode() {
		return false;
	}

	public boolean isDirectedGraphNode() {
		return false;
	}

	public Node getRootNode() {
		if (isRoot) {
			assert parent == null;
			return this;
		} else {
			assert parent != null : " I am not root but I have no parent :-(";
			return parent.getRootNode();
		}
	}

	public String getDecisionPath() {
		String ancestorInfo;
		if (parent == null)
			ancestorInfo = "null";
		else if (parent.isDecisionNode()) {
			ancestorInfo = ((DecisionNode) parent).getDecisionPath(getNodeIndex());
		} else {
			ancestorInfo = parent.getDecisionPath();
		}
		return ancestorInfo + " - " + toString();
	}

	public String toString(String prefix) {
		return prefix + this.toString();
	}

}