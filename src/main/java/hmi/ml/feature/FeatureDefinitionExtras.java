package hmi.ml.feature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FeatureDefinitionExtras extends FeatureDefinition {

    public FeatureDefinition subset(String[] featureNamesToDrop) {
        // construct a list of indices for the features to be dropped:
        List<Integer> featureIndicesToDrop = new ArrayList<Integer>();
        for (String featureName : featureNamesToDrop) {
            int featureIndex;
            try {
                featureIndex = getFeatureIndex(featureName);
                featureIndicesToDrop.add(featureIndex);
            } catch (IllegalArgumentException e) {
                System.err.println("WARNING: feature " + featureName + " not found in FeatureDefinition; ignoring.");
            }
        }

        // create a new FeatureDefinition by way of a byte array:
        FeatureDefinition subDefinition = null;
        try {
            ByteArrayOutputStream toMemory = new ByteArrayOutputStream();
            DataOutput output = new DataOutputStream(toMemory);
            FeatureIO.writeBinaryTo(this, output, featureIndicesToDrop);

            byte[] memory = toMemory.toByteArray();

            ByteArrayInputStream fromMemory = new ByteArrayInputStream(memory);
            DataInput input = new DataInputStream(fromMemory);

            subDefinition = FeatureIO.process(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // make sure that subDefinition really is a subset of this
        assert this.contains(subDefinition);

        return subDefinition;
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

    public FeatureVector readFeatureVector(int currentUnitIndex, DataInput input) throws IOException {
        byte[] bytes = new byte[numByteFeatures];
        input.readFully(bytes);
        short[] shorts = new short[numShortFeatures];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = input.readShort();
        }
        float[] floats = new float[numContinuousFeatures];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = input.readFloat();
        }
        return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
    }

    public FeatureVector readFeatureVector(int currentUnitIndex, ByteBuffer bb) throws IOException {
        byte[] bytes = new byte[numByteFeatures];
        bb.get(bytes);
        short[] shorts = new short[numShortFeatures];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = bb.getShort();
        }
        float[] floats = new float[numContinuousFeatures];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = bb.getFloat();
        }
        return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
    }

    public FeatureVector createEdgeFeatureVector(int unitIndex, boolean start) {
        int edgeFeature = getFeatureIndex(EDGEFEATURE);
        assert edgeFeature < numByteFeatures; // we can assume this is
                                              // byte-valued
        byte edge;
        if (start)
            edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_START);
        else
            edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_END);
        byte[] bytes = new byte[numByteFeatures];
        short[] shorts = new short[numShortFeatures];
        float[] floats = new float[numContinuousFeatures];
        for (int i = 0; i < numByteFeatures; i++) {
            bytes[i] = getFeatureValueAsByte(i, NULLVALUE);
        }
        for (int i = 0; i < numShortFeatures; i++) {
            shorts[i] = getFeatureValueAsShort(numByteFeatures + i, NULLVALUE);
        }
        for (int i = 0; i < numContinuousFeatures; i++) {
            floats[i] = 0;
        }
        bytes[edgeFeature] = edge;
        return new FeatureVector(bytes, shorts, floats, unitIndex);
    }

    public String toFeatureString(FeatureVector fv) {
        if (numByteFeatures != fv.getNumberOfByteFeatures() || numShortFeatures != fv.getNumberOfShortFeatures()
                || numContinuousFeatures != fv.getNumberOfContinuousFeatures())
            throw new IllegalArgumentException("Feature vector '" + fv + "' is inconsistent with feature definition");
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < numByteFeatures; i++) {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(getFeatureValueAsString(i, fv.getByteFeature(i)));
        }
        for (int i = numByteFeatures; i < numByteFeatures + numShortFeatures; i++) {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(getFeatureValueAsString(i, fv.getShortFeature(i)));
        }
        for (int i = numByteFeatures + numShortFeatures; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(fv.getContinuousFeature(i));
        }
        return buf.toString();
    }

    public void writeTo(PrintWriter out, boolean writeWeights) {
        out.println("ByteValuedFeatureProcessors");
        for (int i = 0; i < numByteFeatures; i++) {
            if (writeWeights) {
                out.print(featureWeights[i] + " | ");
            }
            out.print(getFeatureName(i));
            for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
                out.print(" ");
                String val = getFeatureValueAsString(i, v);
                out.print(val);
            }
            out.println();
        }
        out.println("ShortValuedFeatureProcessors");
        for (int i = 0; i < numShortFeatures; i++) {
            if (writeWeights) {
                out.print(featureWeights[numByteFeatures + i] + " | ");
            }
            out.print(getFeatureName(numByteFeatures + i));
            for (int v = 0, vmax = getNumberOfValues(numByteFeatures + i); v < vmax; v++) {
                out.print(" ");
                String val = getFeatureValueAsString(numByteFeatures + i, v);
                out.print(val);
            }
            out.println();
        }
        out.println("ContinuousFeatureProcessors");
        for (int i = 0; i < numContinuousFeatures; i++) {
            if (writeWeights) {
                out.print(featureWeights[numByteFeatures + numShortFeatures + i]);
                out.print(" ");
                out.print(floatWeightFuncts[i]);
                out.print(" | ");
            }

            out.print(getFeatureName(numByteFeatures + numShortFeatures + i));
            out.println();
        }

    }

    public void generateAllDotDescForWagon(PrintWriter out) {
        generateAllDotDescForWagon(out, null);
    }

    public void generateAllDotDescForWagon(PrintWriter out, Set<String> featuresToIgnore) {
        out.println("(");
        out.println("(occurid cluster)");
        for (int i = 0, n = getNumberOfFeatures(); i < n; i++) {
            out.print("( ");
            String featureName = getFeatureName(i);
            out.print(featureName);
            if (featuresToIgnore != null && featuresToIgnore.contains(featureName)) {
                out.print(" ignore");
            }
            if (i < numByteFeatures + numShortFeatures) { // list values
                for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
                    out.print("  ");
                    // Print values surrounded by double quotes, and make sure
                    // any double quotes in the value are preceded by a
                    // backslash -- otherwise, we get problems e.g. for
                    // sentence_punc
                    String val = getFeatureValueAsString(i, v);
                    if (val.indexOf('"') != -1) {
                        StringBuilder buf = new StringBuilder();
                        for (int c = 0; c < val.length(); c++) {
                            char ch = val.charAt(c);
                            if (ch == '"')
                                buf.append("\\\"");
                            else
                                buf.append(ch);
                        }
                        val = buf.toString();
                    }
                    out.print("\"" + val + "\"");
                }

                out.println(" )");
            } else { // float feature
                out.println(" float )");
            }

        }
        out.println(")");
    }

    public void generateFeatureWeightsFile(PrintWriter out) {
        out.println("# This file lists the features and their weights to be used for\n"
                + "# creating the features file.\n"
                + "# The same file can also be used to override weights in a run-time system.\n"
                + "# Three sections are distinguished: Byte-valued, Short-valued, and\n" + "# Continuous features.\n"
                + "#\n" + "# Lines starting with '#' are ignored; they can be used for comments\n"
                + "# anywhere in the file. Empty lines are also ignored.\n"
                + "# Entries must have the following form:\n" + "# \n"
                + "# <weight definition> | <feature definition>\n" + "# \n"
                + "# For byte and short features, <weight definition> is simply the \n"
                + "# (float) number representing the weight.\n"
                + "# For continuous features, <weight definition> is the\n"
                + "# (float) number representing the weight, followed by an optional\n"
                + "# weighting function including arguments.\n" + "#\n"
                + "# The <feature definition> is the feature name, which in the case of\n"
                + "# byte and short features is followed by the full list of feature values.\n" + "#\n"
                + "# Note that the feature definitions must be identical between this file\n"
                + "# and all unit feature files for individual database utterances.\n"
                + "# THIS FILE WAS GENERATED AUTOMATICALLY");
        out.println();
        out.println("ByteValuedFeatureProcessors");
        List<String> getValuesOf10 = new ArrayList<String>();
        getValuesOf10.add("phone");
        getValuesOf10.add("ph_vc");
        getValuesOf10.add("prev_phone");
        getValuesOf10.add("next_phone");
        getValuesOf10.add("stressed");
        getValuesOf10.add("syl_break");
        getValuesOf10.add("prev_syl_break");
        getValuesOf10.add("next_is_pause");
        getValuesOf10.add("prev_is_pause");
        List<String> getValuesOf5 = new ArrayList<String>();
        getValuesOf5.add("cplace");
        getValuesOf5.add("ctype");
        getValuesOf5.add("cvox");
        getValuesOf5.add("vfront");
        getValuesOf5.add("vheight");
        getValuesOf5.add("vlng");
        getValuesOf5.add("vrnd");
        getValuesOf5.add("vc");
        for (int i = 0; i < numByteFeatures; i++) {
            String featureName = getFeatureName(i);
            if (getValuesOf10.contains(featureName)) {
                out.print("10 | " + featureName);
            } else {
                boolean found = false;
                for (String match : getValuesOf5) {
                    if (featureName.matches(".*" + match)) {
                        out.print("5 | " + featureName);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    out.print("0 | " + featureName);
                }
            }
            for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
                String val = getFeatureValueAsString(i, v);
                out.print(" " + val);
            }
            out.print("\n");
        }
        out.println("ShortValuedFeatureProcessors");
        for (int i = numByteFeatures; i < numShortFeatures; i++) {
            String featureName = getFeatureName(i);
            out.print("0 | " + featureName);
            for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
                String val = getFeatureValueAsString(i, v);
                out.print(" " + val);
            }
            out.print("\n");
        }
        out.println("ContinuousFeatureProcessors");
        for (int i = numByteFeatures; i < numByteFeatures + numContinuousFeatures; i++) {
            String featureName = getFeatureName(i);
            out.println("0 linear | " + featureName);
        }
        out.flush();
        out.close();
    }
}
