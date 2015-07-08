package hmi.ml.feature;

import java.util.Arrays;
import java.util.List;

/**
 * A feature definition object represents the "meaning" of feature vectors. It
 * consists of a list of byte-valued, short-valued and continuous features by
 * name and index position in the feature vector; the respective possible
 * feature values (and corresponding byte and short codes); and, optionally, the
 * weights and, for continuous features, weighting functions for each feature.
 */
public class FeatureDefinition {
    public static final String BYTEFEATURES = "ByteValuedFeatureProcessors";
    public static final String SHORTFEATURES = "ShortValuedFeatureProcessors";
    public static final String CONTINUOUSFEATURES = "ContinuousFeatureProcessors";
    public static final String FEATURESIMILARITY = "FeatureSimilarity";
    public static final char WEIGHT_SEPARATOR = '|';
    public static final String EDGEFEATURE = "edge";
    public static final String EDGEFEATURE_START = "start";
    public static final String EDGEFEATURE_END = "end";
    public static final String NULLVALUE = "0";

    int numByteFeatures;
    int numShortFeatures;
    int numContinuousFeatures;
    float[] featureWeights;
    CLInt featureNames;
    CLByte[] byteFeatureValues;
    CLShort[] shortFeatureValues;
    String[] floatWeightFuncts; // for continuous features only
    float[][][] similarityMatrices = null;

    public FeatureDefinition() {
    }

    public int getNumberOfFeatures() {
        return numByteFeatures + numShortFeatures + numContinuousFeatures;
    }

    public int getNumberOfByteFeatures() {
        return numByteFeatures;
    }

    public int getNumberOfShortFeatures() {
        return numShortFeatures;
    }

    public int getNumberOfContinuousFeatures() {
        return numContinuousFeatures;
    }

    public float getWeight(int featureIndex) {
        return featureWeights[featureIndex];
    }

    public float[] getFeatureWeights() {
        return featureWeights;
    }

    public String getWeightFunctionName(int featureIndex) {
        return floatWeightFuncts[featureIndex - numByteFeatures - numShortFeatures];
    }

    // //////////////////// META-INFORMATION METHODS ///////////////////////

    public String getFeatureName(int index) {
        return featureNames.get(index);
    }

    public String[] getFeatureNameArray(int[] index) {
        String[] ret = new String[index.length];
        for (int i = 0; i < index.length; i++) {
            ret[i] = getFeatureName(index[i]);
        }
        return (ret);
    }

    public String[] getFeatureNameArray() {
        String[] names = new String[getNumberOfFeatures()];
        for (int i = 0; i < names.length; i++) {
            names[i] = getFeatureName(i);
        }
        return (names);
    }

    public String[] getByteFeatureNameArray() {
        String[] byteFeatureNames = new String[numByteFeatures];
        for (int i = 0; i < numByteFeatures; i++) {
            assert isByteFeature(i);
            byteFeatureNames[i] = getFeatureName(i);
        }
        return byteFeatureNames;
    }

    public String[] getShortFeatureNameArray() {
        String[] shortFeatureNames = new String[numShortFeatures];
        for (int i = 0; i < numShortFeatures; i++) {
            int shortFeatureIndex = numByteFeatures + i;
            assert isShortFeature(shortFeatureIndex);
            shortFeatureNames[i] = getFeatureName(shortFeatureIndex);
        }
        return shortFeatureNames;
    }

    public String[] getContinuousFeatureNameArray() {
        String[] continuousFeatureNames = new String[numContinuousFeatures];
        for (int i = 0; i < numContinuousFeatures; i++) {
            int continuousFeatureIndex = numByteFeatures + numShortFeatures + i;
            assert isContinuousFeature(continuousFeatureIndex);
            continuousFeatureNames[i] = getFeatureName(continuousFeatureIndex);
        }
        return continuousFeatureNames;
    }

    public String getFeatureNames() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0, n = getNumberOfFeatures(); i < n; i++) {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(featureNames.get(i));
        }
        return buf.toString();
    }

    public boolean hasFeature(String name) {
        return featureNames.contains(name);
    }

    public boolean hasFeatureValue(String featureName, String featureValue) {
        return hasFeatureValue(getFeatureIndex(featureName), featureValue);
    }

    public boolean hasFeatureValue(int featureIndex, String featureValue) {
        if (featureIndex < 0) {
            return false;
        }
        if (featureIndex < numByteFeatures) {
            return byteFeatureValues[featureIndex].contains(featureValue);
        }
        if (featureIndex < numByteFeatures + numShortFeatures) {
            return shortFeatureValues[featureIndex - numByteFeatures].contains(featureValue);
        }
        return false;
    }

    public boolean isByteFeature(String featureName) {
        try {
            int index = getFeatureIndex(featureName);
            return isByteFeature(index);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isByteFeature(int index) {
        return 0 <= index && index < numByteFeatures;
    }

    public boolean isShortFeature(String featureName) {
        try {
            int index = getFeatureIndex(featureName);
            return isShortFeature(index);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isShortFeature(int index) {
        index -= numByteFeatures;
        return 0 <= index && index < numShortFeatures;
    }

    public boolean isContinuousFeature(String featureName) {
        try {
            int index = getFeatureIndex(featureName);
            return isContinuousFeature(index);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isContinuousFeature(int index) {
        index -= numByteFeatures;
        index -= numShortFeatures;
        return 0 <= index && index < numContinuousFeatures;
    }

    public boolean hasSimilarityMatrix(int featureIndex) {

        if (featureIndex >= this.getNumberOfByteFeatures()) {
            return false;
        }
        if (this.similarityMatrices != null && this.similarityMatrices[featureIndex] != null) {
            return true;
        }
        return false;
    }

    public boolean hasSimilarityMatrix(String featureName) {
        return hasSimilarityMatrix(this.getFeatureIndex(featureName));
    }

    public float getSimilarity(int featureIndex, byte i, byte j) {
        if (!hasSimilarityMatrix(featureIndex)) {
            throw new RuntimeException("the given feature index  ");
        }
        return this.similarityMatrices[featureIndex][i][j];
    }

    public int getFeatureIndex(String featureName) {
        return featureNames.get(featureName);
    }

    public int[] getFeatureIndexArray(String[] featureName) {
        int[] ret = new int[featureName.length];
        for (int i = 0; i < featureName.length; i++) {
            ret[i] = getFeatureIndex(featureName[i]);
        }
        return (ret);
    }

    public int getNumberOfValues(int featureIndex) {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].getNumberOfValues();
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].getNumberOfValues();
        throw new IndexOutOfBoundsException("Feature no. " + featureIndex
                + " is not a byte-valued or short-valued feature");
    }

    public String[] getPossibleValues(int featureIndex) {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].getStringValues();
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].getStringValues();
        throw new IndexOutOfBoundsException("Feature no. " + featureIndex
                + " is not a byte-valued or short-valued feature");
    }

    public String getFeatureValueAsString(int featureIndex, int value) {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].get((byte) value);
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get((short) value);
        throw new IndexOutOfBoundsException("Feature no. " + featureIndex
                + " is not a byte-valued or short-valued feature");
    }

    public String getFeatureValueAsString(String featureName, FeatureVector fv) {
        int i = getFeatureIndex(featureName);
        return getFeatureValueAsString(i, fv.getFeatureAsInt(i));
    }

    public byte getFeatureValueAsByte(String featureName, String value) {
        int featureIndex = getFeatureIndex(featureName);
        return getFeatureValueAsByte(featureIndex, value);
    }

    public byte getFeatureValueAsByte(int featureIndex, String value) {
        if (featureIndex >= numByteFeatures)
            throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a byte-valued feature");
        try {
            return byteFeatureValues[featureIndex].get(value);
        } catch (IllegalArgumentException iae) {
            StringBuilder message = new StringBuilder("Illegal value '" + value + "' for feature "
                    + getFeatureName(featureIndex) + "; Legal values are:\n");
            for (String v : getPossibleValues(featureIndex)) {
                message.append(" " + v);
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public short getFeatureValueAsShort(String featureName, String value) {
        int featureIndex = getFeatureIndex(featureName);
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature '" + featureName + "' is not a short-valued feature");
    }

    public short getFeatureValueAsShort(int featureIndex, String value) {
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a short-valued feature");
    }

    public boolean featureEquals(FeatureDefinition other) {
        if (numByteFeatures != other.numByteFeatures || numShortFeatures != other.numShortFeatures
                || numContinuousFeatures != other.numContinuousFeatures)
            return false;
        // Compare the feature names and values for byte and short features:
        for (int i = 0; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
            if (!getFeatureName(i).equals(other.getFeatureName(i)))
                return false;
        }
        // Compare the values for byte and short features:
        for (int i = 0; i < numByteFeatures + numShortFeatures; i++) {
            if (getNumberOfValues(i) != other.getNumberOfValues(i))
                return false;
            for (int v = 0, n = getNumberOfValues(i); v < n; v++) {
                if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * An extension of the previous method.
     */
    public String featureEqualsAnalyse(FeatureDefinition other) {
        if (numByteFeatures != other.numByteFeatures) {
            return ("The number of BYTE features differs: " + numByteFeatures + " versus " + other.numByteFeatures);
        }
        if (numShortFeatures != other.numShortFeatures) {
            return ("The number of SHORT features differs: " + numShortFeatures + " versus " + other.numShortFeatures);
        }
        if (numContinuousFeatures != other.numContinuousFeatures) {
            return ("The number of CONTINUOUS features differs: " + numContinuousFeatures + " versus " + other.numContinuousFeatures);
        }
        // Compare the feature names and values for byte and short features:
        for (int i = 0; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
            if (!getFeatureName(i).equals(other.getFeatureName(i))) {
                return ("The feature name differs at position [" + i + "]: " + getFeatureName(i) + " versus " + other
                        .getFeatureName(i));
            }
        }
        // Compare the values for byte and short features:
        for (int i = 0; i < numByteFeatures + numShortFeatures; i++) {
            if (getNumberOfValues(i) != other.getNumberOfValues(i)) {
                return ("The number of values differs at position [" + i + "]: " + getNumberOfValues(i) + " versus " + other
                        .getNumberOfValues(i));
            }
            for (int v = 0, n = getNumberOfValues(i); v < n; v++) {
                if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v))) {
                    return ("The feature value differs at position [" + i + "] for feature value [" + v + "]: "
                            + getFeatureValueAsString(i, v) + " versus " + other.getFeatureValueAsString(i, v));
                }
            }
        }
        return "";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeatureDefinition))
            return false;
        FeatureDefinition other = (FeatureDefinition) obj;
        if (featureWeights == null) {
            if (other.featureWeights != null)
                return false;
            // Both are null
        } else { // featureWeights != null
            if (other.featureWeights == null)
                return false;
            // Both != null
            if (featureWeights.length != other.featureWeights.length)
                return false;
            for (int i = 0; i < featureWeights.length; i++) {
                if (featureWeights[i] != other.featureWeights[i])
                    return false;
            }
            assert floatWeightFuncts != null;
            assert other.floatWeightFuncts != null;
            if (floatWeightFuncts.length != other.floatWeightFuncts.length)
                return false;
            for (int i = 0; i < floatWeightFuncts.length; i++) {
                if (floatWeightFuncts[i] == null) {
                    if (other.floatWeightFuncts[i] != null)
                        return false;
                    // Both are null
                } else { // != null
                    if (other.floatWeightFuncts[i] == null)
                        return false;
                    // Both != null
                    if (!floatWeightFuncts[i].equals(other.floatWeightFuncts[i]))
                        return false;
                }
            }
        }
        return featureEquals(other);
    }

    public boolean contains(FeatureDefinition other) {
        List<String> thisByteFeatures = Arrays.asList(this.getByteFeatureNameArray());
        List<String> otherByteFeatures = Arrays.asList(other.getByteFeatureNameArray());
        if (!thisByteFeatures.containsAll(otherByteFeatures)) {
            return false;
        }
        for (String commonByteFeature : otherByteFeatures) {
            String[] thisByteFeaturePossibleValues = this.getPossibleValues(this.getFeatureIndex(commonByteFeature));
            String[] otherByteFeaturePossibleValues = other.getPossibleValues(other.getFeatureIndex(commonByteFeature));
            if (!Arrays.equals(thisByteFeaturePossibleValues, otherByteFeaturePossibleValues)) {
                return false;
            }
        }
        List<String> thisShortFeatures = Arrays.asList(this.getShortFeatureNameArray());
        List<String> otherShortFeatures = Arrays.asList(other.getShortFeatureNameArray());
        if (!thisShortFeatures.containsAll(otherShortFeatures)) {
            return false;
        }
        for (String commonShortFeature : otherShortFeatures) {
            String[] thisShortFeaturePossibleValues = this.getPossibleValues(this.getFeatureIndex(commonShortFeature));
            String[] otherShortFeaturePossibleValues = other.getPossibleValues(other
                    .getFeatureIndex(commonShortFeature));
            if (!Arrays.equals(thisShortFeaturePossibleValues, otherShortFeaturePossibleValues)) {
                return false;
            }
        }
        List<String> thisContinuousFeatures = Arrays.asList(this.getContinuousFeatureNameArray());
        List<String> otherContinuousFeatures = Arrays.asList(other.getContinuousFeatureNameArray());
        if (!thisContinuousFeatures.containsAll(otherContinuousFeatures)) {
            return false;
        }
        return true;
    }

    /**
     * Compares two feature vectors in terms of how many discrete features they
     * have in common. WARNING: this assumes that the feature vectors are issued
     * from the same FeatureDefinition; only the number of features is checked
     * for compatibility.
     */
    public static int diff(FeatureVector v1, FeatureVector v2) {

        int ret = 0;

        /* Byte valued features */
        if (v1.byteValuedDiscreteFeatures.length < v2.byteValuedDiscreteFeatures.length) {
            throw new RuntimeException("v1 and v2 don't have the same number of byte-valued features: ["
                    + v1.byteValuedDiscreteFeatures.length + "] versus [" + v2.byteValuedDiscreteFeatures.length + "].");
        }
        for (int i = 0; i < v1.byteValuedDiscreteFeatures.length; i++) {
            if (v1.byteValuedDiscreteFeatures[i] == v2.byteValuedDiscreteFeatures[i])
                ret++;
        }

        /* Short valued features */
        if (v1.shortValuedDiscreteFeatures.length < v2.shortValuedDiscreteFeatures.length) {
            throw new RuntimeException("v1 and v2 don't have the same number of short-valued features: ["
                    + v1.shortValuedDiscreteFeatures.length + "] versus [" + v2.shortValuedDiscreteFeatures.length
                    + "].");
        }
        for (int i = 0; i < v1.shortValuedDiscreteFeatures.length; i++) {
            if (v1.shortValuedDiscreteFeatures[i] == v2.shortValuedDiscreteFeatures[i])
                ret++;
        }

        /* TODO: would checking float-valued features make sense ? (Code below.) */
        /* float valued features */
        /*
         * if ( v1.continuousFeatures.length < v2.continuousFeatures.length ) {
         * throw new RuntimeException(
         * "v1 and v2 don't have the same number of continuous features: [" +
         * v1.continuousFeatures.length + "] versus [" +
         * v2.continuousFeatures.length + "]." ); } float epsilon = 1.0e-6f;
         * float d = 0.0f; for ( int i = 0; i < v1.continuousFeatures.length;
         * i++ ) { d = ( v1.continuousFeatures[i] > v2.continuousFeatures[i] ?
         * (v1.continuousFeatures[i] - v2.continuousFeatures[i]) :
         * (v2.continuousFeatures[i] - v1.continuousFeatures[i]) ); // => this
         * avoids Math.abs() if ( d < epsilon ) ret++; }
         */

        return (ret);
    }

    public FeatureVector toFeatureVector(int unitIndex, byte[] bytes, short[] shorts, float[] floats) {
        if (!((numByteFeatures == 0 && bytes == null || numByteFeatures == bytes.length)
                && (numShortFeatures == 0 && shorts == null || numShortFeatures == shorts.length) && (numContinuousFeatures == 0
                && floats == null || numContinuousFeatures == floats.length))) {
            throw new IllegalArgumentException("Expected " + numByteFeatures + " bytes (got "
                    + (bytes == null ? "0" : bytes.length) + "), " + numShortFeatures + " shorts (got "
                    + (shorts == null ? "0" : shorts.length) + "), " + numContinuousFeatures + " floats (got "
                    + (floats == null ? "0" : floats.length) + ")");
        }
        return new FeatureVector(bytes, shorts, floats, unitIndex);
    }

    public FeatureVector toFeatureVector(int unitIndex, String featureString) {
        String[] featureValues = featureString.split("\\s+");
        if (featureValues.length != numByteFeatures + numShortFeatures + numContinuousFeatures)
            throw new IllegalArgumentException("Expected "
                    + (numByteFeatures + numShortFeatures + numContinuousFeatures) + " features, got "
                    + featureValues.length);
        byte[] bytes = new byte[numByteFeatures];
        short[] shorts = new short[numShortFeatures];
        float[] floats = new float[numContinuousFeatures];
        for (int i = 0; i < numByteFeatures; i++) {
            bytes[i] = Byte.parseByte(featureValues[i]);
        }
        for (int i = 0; i < numShortFeatures; i++) {
            shorts[i] = Short.parseShort(featureValues[numByteFeatures + i]);
        }
        for (int i = 0; i < numContinuousFeatures; i++) {
            floats[i] = Float.parseFloat(featureValues[numByteFeatures + numShortFeatures + i]);
        }
        return new FeatureVector(bytes, shorts, floats, unitIndex);
    }

}
