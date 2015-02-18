package hmi.synth.target;

import hmi.ml.feature.FeatureVector;
import hmi.phone.Allophone;

public class DiphoneTarget extends Target {
    public final HalfPhoneTarget left;
    public final HalfPhoneTarget right;

    public DiphoneTarget(HalfPhoneTarget left, HalfPhoneTarget right) {
        super(null, null);
        this.name = left.name.substring(0, left.name.lastIndexOf("_")) + "-"
                + right.name.substring(0, right.name.lastIndexOf("_"));
        assert left.isRightHalf(); // the left half of this diphone must be the
                                   // right half of a phone
        assert right.isLeftHalf();
        this.left = left;
        this.right = right;
    }

    public FeatureVector getFeatureVector() {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }

    public void setFeatureVector(FeatureVector featureVector) {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }

    public float getTargetDurationInSeconds() {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }

    /**
     * Determine whether this target is a silence target
     * 
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence() {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }

    public Allophone getAllophone() {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }

}
