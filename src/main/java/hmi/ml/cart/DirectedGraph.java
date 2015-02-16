package hmi.ml.cart;

import java.util.Iterator;
import java.util.Properties;

import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;

/**
 * A directed graph is a layered structure of nodes, in which there are
 * parent-child relationships between the node. There is a single root node.
 * Each node can have multiple children and/or multiple parents. Three types of
 * nodes are allowed: DirectedGraphNode (which can have multiple parents, a leaf
 * and a decision node), LeafNodes (which carry data), and DecisionNodes (which
 * can have multiple children).
 */
public class DirectedGraph {

	protected Node rootNode;

	// knows the index numbers and types of the features used in DecisionNodes
	protected FeatureDefinition featDef;

	protected Properties properties;


	public DirectedGraph() {
	}


	public DirectedGraph(FeatureDefinition featDef) {
		this(null, featDef);
	}


	public DirectedGraph(Node rootNode, FeatureDefinition featDef) {
		this(rootNode, featDef, null);
	}


	public DirectedGraph(Node rootNode, FeatureDefinition featDef, Properties properties) {
		this.rootNode = rootNode;
		this.featDef = featDef;
		this.properties = properties;
	}

	// public Object interpret(Target t) {
	// return interpret(t.getFeatureVector());
	// }

	/**
	 * Walk down the graph as far as possible according to the features in fv,
	 * and return the data in the leaf node found there.
	 * 
	 * @param fv
	 *            a feature vector which must be consistent with the graph's
	 *            feature definition. (@see #getFeatureDefinition()).
	 * @return the most specific non-null leaf node data that can be retrieved,
	 *         or null if there is no non-null leaf node data along the fv's
	 *         path.
	 */
	public Object interpret(FeatureVector fv) {
		return interpret(rootNode, fv);
	}

	/**
	 * Follow the directed graph down to the most specific leaf with data,
	 * starting from node n. This is recursively calling itself.
	 * 
	 */
	protected Object interpret(Node n, FeatureVector fv) {
		if (n == null)
			return null;
		else if (n.isLeafNode()) {
			return n.getAllData();
		} else if (n.isDecisionNode()) {
			Node next = ((DecisionNode) n).getNextNode(fv);
			return interpret(next, fv);
		} else if (n.isDirectedGraphNode()) {
			DirectedGraphNode g = (DirectedGraphNode) n;
			Object data = interpret(g.getDecisionNode(), fv);
			if (data != null) { 
				return data;
			}
			return interpret(g.getLeafNode(), fv);
		}
		throw new IllegalArgumentException("Unknown node type: " + n.getClass());
	}

	/**
	 * Return an iterator which returns all nodes in the tree exactly once.
	 * Search is done in a depth-first way.
	 */
	public Iterator<Node> getNodeIterator() {
		return new NodeIterator<Node>(this, true, true, true);
	}

	/**
	 * Return an iterator which returns all leaf nodes in the tree exactly once.
	 * Search is done in a depth-first way.
	 */
	public Iterator<LeafNode> getLeafNodeIterator() {
		return new NodeIterator<LeafNode>(this, true, false, false);
	}

	/**
	 * Return an iterator which returns all decision nodes in the tree exactly
	 * once. Search is done in a depth-first way.
	 */
	public Iterator<DecisionNode> getDecisionNodeIterator() {
		return new NodeIterator<DecisionNode>(this, false, true, false);
	}

	/**
	 * Return an iterator which returns all directed graph nodes in the tree
	 * exactly once. Search is done in a depth-first way.
	 * 
	 * @return
	 */
	public Iterator<DirectedGraphNode> getDirectedGraphNodeIterator() {
		return new NodeIterator<DirectedGraphNode>(this, false, false, true);
	}

	/**
	 * A representation of the corresponding node iterator that can be used in
	 * extended for() statements.
	 */
	public Iterable<Node> getNodes() {
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return getNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in
	 * extended for() statements.
	 */
	public Iterable<LeafNode> getLeafNodes() {
		return new Iterable<LeafNode>() {
			public Iterator<LeafNode> iterator() {
				return getLeafNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in
	 * extended for() statements.
	 */
	public Iterable<DecisionNode> getDecisionNodes() {
		return new Iterable<DecisionNode>() {
			public Iterator<DecisionNode> iterator() {
				return getDecisionNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in
	 * extended for() statements.
	 */
	public Iterable<DirectedGraphNode> getDirectedGraphNodes() {
		return new Iterable<DirectedGraphNode>() {
			public Iterator<DirectedGraphNode> iterator() {
				return getDirectedGraphNodeIterator();
			}
		};
	}

	public Properties getProperties() {
		return properties;
	}


	public Node getRootNode() {
		return rootNode;
	}


	public void setRootNode(Node rNode) {
		rootNode = rNode;
	}

	public FeatureDefinition getFeatureDefinition() {
		return featDef;
	}


	public int getNumNodes() {
		if (rootNode == null)
			return 0;
		return rootNode.getNumberOfNodes();
	}

	public String toString() {
		return this.rootNode.toString("");
	}

}
