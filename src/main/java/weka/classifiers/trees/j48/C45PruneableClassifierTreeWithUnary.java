package weka.classifiers.trees.j48;

import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ModelSelection;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;

/**
 * This Class is a wrapper for C45PruneableClassifierTree that enables the
 * UNARY_CLASS capability.
 */

public class C45PruneableClassifierTreeWithUnary extends C45PruneableClassifierTree {

	private static final long serialVersionUID = 6760898924357943461L;

	public C45PruneableClassifierTreeWithUnary(ModelSelection toSelectLocModel, boolean pruneTree, float cf,
			boolean raiseTree, boolean cleanup, boolean collapseTree) throws Exception {
		super(toSelectLocModel, pruneTree, cf, raiseTree, cleanup, collapseTree);
	}

	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);
		result.enable(Capability.UNARY_CLASS); 

		// class
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);

		// instances
		result.setMinimumNumberInstances(0);

		return result;
	}

}
