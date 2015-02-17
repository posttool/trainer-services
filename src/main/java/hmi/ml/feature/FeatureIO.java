package hmi.ml.feature;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureIO {
    public static FeatureDefinition process(BufferedReader input, boolean readWeights) throws IOException {
        FeatureDefinition fd = new FeatureDefinition();
        // Section BYTEFEATURES
        String line = input.readLine();
        if (line == null)
            throw new IOException("Could not read from input");
        while (line.matches("^\\s*#.*") || line.matches("\\s*")) {
            line = input.readLine();
        }
        if (!line.trim().equals(FeatureDefinition.BYTEFEATURES)) {
            throw new IOException("Unexpected input: expected '" + FeatureDefinition.BYTEFEATURES + "', read '" + line
                    + "'");
        }
        List<String> byteFeatureLines = new ArrayList<String>();
        while (true) {
            line = input.readLine();
            if (line == null)
                throw new IOException("Could not read from input");
            line = line.trim();
            if (line.equals(FeatureDefinition.SHORTFEATURES))
                break; // Found end of section
            byteFeatureLines.add(line);
        }
        // Section SHORTFEATURES
        List<String> shortFeatureLines = new ArrayList<String>();
        while (true) {
            line = input.readLine();
            if (line == null)
                throw new IOException("Could not read from input");
            line = line.trim();
            if (line.equals(FeatureDefinition.CONTINUOUSFEATURES))
                break; // Found end of section
            shortFeatureLines.add(line);
        }
        // Section CONTINUOUSFEATURES
        List<String> continuousFeatureLines = new ArrayList<String>();
        boolean readFeatureSimilarity = false;
        while ((line = input.readLine()) != null) { // it's OK if we hit the end
                                                    // of the file now
            line = line.trim();
            // if (line.equals(FEATURESIMILARITY) || line.equals("")) break; //
            // Found end of section
            if (line.equals(FeatureDefinition.FEATURESIMILARITY)) {
                // readFeatureSimilarityMatrices(input);
                readFeatureSimilarity = true;
                break;
            } else if (line.equals("")) { // empty line: end of section
                break;
            }
            continuousFeatureLines.add(line);
        }
        fd.numByteFeatures = byteFeatureLines.size();
        fd.numShortFeatures = shortFeatureLines.size();
        fd.numContinuousFeatures = continuousFeatureLines.size();
        int total = fd.numByteFeatures + fd.numShortFeatures + fd.numContinuousFeatures;
        fd.featureNames = new CLInt(total);
        fd.byteFeatureValues = new CLByte[fd.numByteFeatures];
        fd.shortFeatureValues = new CLShort[fd.numShortFeatures];
        float sumOfWeights = 0; // for normalisation of weights
        if (readWeights) {
            fd.featureWeights = new float[total];
            fd.floatWeightFuncts = new String[fd.numContinuousFeatures];
        }

        for (int i = 0; i < fd.numByteFeatures; i++) {
            line = byteFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(FeatureDefinition.WEIGHT_SEPARATOR);
                if (seppos == -1)
                    throw new IOException("Weight separator '" + FeatureDefinition.WEIGHT_SEPARATOR
                            + "' not found in line '" + line + "'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos + 1).trim();
                // The weight definition is simply the float number:
                fd.featureWeights[i] = Float.parseFloat(weightDef);
                sumOfWeights += fd.featureWeights[i];
                if (fd.featureWeights[i] < 0)
                    throw new IOException("Negative weight found in line '" + line + "'");
            } else {
                featureDef = line;
            }
            // Now featureDef is a String in which the feature name and all
            // feature values
            // are separated by white space.
            String[] nameAndValues = featureDef.split("\\s+", 2);
            fd.featureNames.set(i, nameAndValues[0]); // the feature name
            fd.byteFeatureValues[i] = new CLByte(nameAndValues[1].split("\\s+"));
        }

        for (int i = 0; i < fd.numShortFeatures; i++) {
            line = shortFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(FeatureDefinition.WEIGHT_SEPARATOR);
                if (seppos == -1)
                    throw new IOException("Weight separator '" + FeatureDefinition.WEIGHT_SEPARATOR
                            + "' not found in line '" + line + "'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos + 1).trim();
                // The weight definition is simply the float number:
                fd.featureWeights[fd.numByteFeatures + i] = Float.parseFloat(weightDef);
                sumOfWeights += fd.featureWeights[fd.numByteFeatures + i];
                if (fd.featureWeights[fd.numByteFeatures + i] < 0)
                    throw new IOException("Negative weight found in line '" + line + "'");
            } else {
                featureDef = line;
            }
            // Now featureDef is a String in which the feature name and all
            // feature values
            // are separated by white space.
            String[] nameAndValues = featureDef.split("\\s+", 2);
            fd.featureNames.set(fd.numByteFeatures + i, nameAndValues[0]);
            fd.shortFeatureValues[i] = new CLShort(nameAndValues[1].split("\\s+"));
        }

        for (int i = 0; i < fd.numContinuousFeatures; i++) {
            line = continuousFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(FeatureDefinition.WEIGHT_SEPARATOR);
                if (seppos == -1)
                    throw new IOException("Weight separator '" + FeatureDefinition.WEIGHT_SEPARATOR
                            + "' not found in line '" + line + "'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos + 1).trim();
                // The weight definition is the float number plus a definition
                // of a weight function:
                String[] weightAndFunction = weightDef.split("\\s+", 2);
                fd.featureWeights[fd.numByteFeatures + fd.numShortFeatures + i] = Float
                        .parseFloat(weightAndFunction[0]);
                sumOfWeights += fd.featureWeights[fd.numByteFeatures + fd.numShortFeatures + i];
                if (fd.featureWeights[fd.numByteFeatures + fd.numShortFeatures + i] < 0)
                    throw new IOException("Negative weight found in line '" + line + "'");
                try {
                    fd.floatWeightFuncts[i] = weightAndFunction[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    // System.out.println( "weightDef string was: '" + weightDef
                    // + "'." );
                    // System.out.println( "Splitting part 1: '" +
                    // weightAndFunction[0] + "'." );
                    // System.out.println( "Splitting part 2: '" +
                    // weightAndFunction[1] + "'." );
                    throw new RuntimeException("The string [" + weightDef + "] appears to be a badly formed"
                            + " weight plus weighting function definition.");
                }
            } else {
                featureDef = line;
            }
            // Now featureDef is the feature name
            // or the feature name followed by the word "float"
            if (featureDef.endsWith("float")) {
                String[] featureDefSplit = featureDef.split("\\s+", 2);
                fd.featureNames.set(fd.numByteFeatures + fd.numShortFeatures + i, featureDefSplit[0]);
            } else {
                fd.featureNames.set(fd.numByteFeatures + fd.numShortFeatures + i, featureDef);
            }
        }
        // Normalize weights to sum to one:
        if (readWeights) {
            for (int i = 0; i < total; i++) {
                fd.featureWeights[i] /= sumOfWeights;
            }
        }

        // read feature similarities here, if any
        if (readFeatureSimilarity) {
            readFeatureSimilarityMatrices(fd, input);
        }
        return fd;
    }

    private static void readFeatureSimilarityMatrices(FeatureDefinition fd, BufferedReader input) throws IOException {

        String line = null;

        fd.similarityMatrices = new float[fd.getNumberOfByteFeatures()][][];
        for (int i = 0; i < fd.getNumberOfByteFeatures(); i++) {
            fd.similarityMatrices[i] = null;
        }

        while ((line = input.readLine()) != null) {

            if ("".equals(line)) {
                return;
            }

            String[] featureUniqueValues = line.trim().split("\\s+");
            String featureName = featureUniqueValues[0];

            if (!fd.isByteFeature(featureName)) {
                throw new RuntimeException(
                        "Similarity matrix support is for bytefeatures only, but not for other feature types...");
            }

            int featureIndex = fd.getFeatureIndex(featureName);
            int noUniqValues = featureUniqueValues.length - 1;
            fd.similarityMatrices[featureIndex] = new float[noUniqValues][noUniqValues];

            for (int i = 1; i <= noUniqValues; i++) {

                Arrays.fill(fd.similarityMatrices[featureIndex][i - 1], 0);
                String featureValue = featureUniqueValues[i];

                String matLine = input.readLine();
                if (matLine == null) {
                    throw new RuntimeException("Feature definition file is having unexpected format...");
                }

                String[] lines = matLine.trim().split("\\s+");
                if (!featureValue.equals(lines[0])) {
                    throw new RuntimeException("Feature definition file is having unexpected format...");
                }
                if (lines.length != i) {
                    throw new RuntimeException("Feature definition file is having unexpected format...");
                }
                for (int j = 1; j < i; j++) {
                    float similarity = (new Float(lines[j])).floatValue();
                    fd.similarityMatrices[featureIndex][i - 1][j - 1] = similarity;
                    fd.similarityMatrices[featureIndex][j - 1][i - 1] = similarity;
                }

            }
        }

    }

    public static FeatureDefinition process(DataInput input) throws IOException {
        FeatureDefinition fd = new FeatureDefinition();
        // Section BYTEFEATURES
        fd.numByteFeatures = input.readInt();
        fd.byteFeatureValues = new CLByte[fd.numByteFeatures];
        // Initialise global arrays to byte feature length first;
        // we have no means of knowing how many short or continuous
        // features there will be, so we need to resize later.
        // This will happen automatically for featureNames, but needs
        // to be done by hand for featureWeights.
        fd.featureNames = new CLInt(fd.numByteFeatures);
        fd.featureWeights = new float[fd.numByteFeatures];
        // There is no need to normalise weights here, because
        // they have already been normalized before the binary
        // file was written.
        for (int i = 0; i < fd.numByteFeatures; i++) {
            fd.featureWeights[i] = input.readFloat();
            String featureName = input.readUTF();
            fd.featureNames.set(i, featureName);
            byte numberOfValuesEncoded = input.readByte(); // attention: this is
                                                           // an unsigned byte
            int numberOfValues = numberOfValuesEncoded & 0xFF;
            fd.byteFeatureValues[i] = new CLByte(numberOfValues);
            for (int b = 0; b < numberOfValues; b++) {
                String value = input.readUTF();
                fd.byteFeatureValues[i].set((byte) b, value);
            }
        }
        // Section SHORTFEATURES
        fd.numShortFeatures = input.readInt();
        if (fd.numShortFeatures > 0) {
            fd.shortFeatureValues = new CLShort[fd.numShortFeatures];
            // resize weight array:
            float[] newWeights = new float[fd.numByteFeatures + fd.numShortFeatures];
            System.arraycopy(fd.featureWeights, 0, newWeights, 0, fd.numByteFeatures);
            fd.featureWeights = newWeights;

            for (int i = 0; i < fd.numShortFeatures; i++) {
                fd.featureWeights[fd.numByteFeatures + i] = input.readFloat();
                String featureName = input.readUTF();
                fd.featureNames.set(fd.numByteFeatures + i, featureName);
                short numberOfValues = input.readShort();
                fd.shortFeatureValues[i] = new CLShort(numberOfValues);
                for (short s = 0; s < numberOfValues; s++) {
                    String value = input.readUTF();
                    fd.shortFeatureValues[i].set(s, value);
                }
            }
        }
        // Section CONTINUOUSFEATURES
        fd.numContinuousFeatures = input.readInt();
        fd.floatWeightFuncts = new String[fd.numContinuousFeatures];
        if (fd.numContinuousFeatures > 0) {
            // resize weight array:
            float[] newWeights = new float[fd.numByteFeatures + fd.numShortFeatures + fd.numContinuousFeatures];
            System.arraycopy(fd.featureWeights, 0, newWeights, 0, fd.numByteFeatures + fd.numShortFeatures);
            fd.featureWeights = newWeights;
        }
        for (int i = 0; i < fd.numContinuousFeatures; i++) {
            fd.featureWeights[fd.numByteFeatures + fd.numShortFeatures + i] = input.readFloat();
            fd.floatWeightFuncts[i] = input.readUTF();
            String featureName = input.readUTF();
            fd.featureNames.set(fd.numByteFeatures + fd.numShortFeatures + i, featureName);
        }
        return fd;
    }

    public static FeatureDefinition process(ByteBuffer bb) throws IOException {
        FeatureDefinition fd = new FeatureDefinition();
        // Section BYTEFEATURES
        fd.numByteFeatures = bb.getInt();
        fd.byteFeatureValues = new CLByte[fd.numByteFeatures];
        // Initialize global arrays to byte feature length first;
        // we have no means of knowing how many short or continuous
        // features there will be, so we need to resize later.
        // This will happen automatically for featureNames, but needs
        // to be done by hand for featureWeights.
        fd.featureNames = new CLInt(fd.numByteFeatures);
        fd.featureWeights = new float[fd.numByteFeatures];
        // There is no need to normalize weights here, because
        // they have already been normalized before the binary
        // file was written.
        for (int i = 0; i < fd.numByteFeatures; i++) {
            fd.featureWeights[i] = bb.getFloat();
            String featureName = readUTF(bb);
            fd.featureNames.set(i, featureName);
            byte numberOfValuesEncoded = bb.get(); // attention: this is an
                                                   // unsigned byte
            int numberOfValues = numberOfValuesEncoded & 0xFF;
            fd.byteFeatureValues[i] = new CLByte(numberOfValues);
            for (int b = 0; b < numberOfValues; b++) {
                String value = readUTF(bb);
                fd.byteFeatureValues[i].set((byte) b, value);
            }
        }
        // Section SHORTFEATURES
        fd.numShortFeatures = bb.getInt();
        if (fd.numShortFeatures > 0) {
            fd.shortFeatureValues = new CLShort[fd.numShortFeatures];
            // resize weight array:
            float[] newWeights = new float[fd.numByteFeatures + fd.numShortFeatures];
            System.arraycopy(fd.featureWeights, 0, newWeights, 0, fd.numByteFeatures);
            fd.featureWeights = newWeights;

            for (int i = 0; i < fd.numShortFeatures; i++) {
                fd.featureWeights[fd.numByteFeatures + i] = bb.getFloat();
                String featureName = readUTF(bb);
                fd.featureNames.set(fd.numByteFeatures + i, featureName);
                short numberOfValues = bb.getShort();
                fd.shortFeatureValues[i] = new CLShort(numberOfValues);
                for (short s = 0; s < numberOfValues; s++) {
                    String value = readUTF(bb);
                    fd.shortFeatureValues[i].set(s, value);
                }
            }
        }
        // Section CONTINUOUSFEATURES
        fd.numContinuousFeatures = bb.getInt();
        fd.floatWeightFuncts = new String[fd.numContinuousFeatures];
        if (fd.numContinuousFeatures > 0) {
            // resize weight array:
            float[] newWeights = new float[fd.numByteFeatures + fd.numShortFeatures + fd.numContinuousFeatures];
            System.arraycopy(fd.featureWeights, 0, newWeights, 0, fd.numByteFeatures + fd.numShortFeatures);
            fd.featureWeights = newWeights;
        }
        for (int i = 0; i < fd.numContinuousFeatures; i++) {
            fd.featureWeights[fd.numByteFeatures + fd.numShortFeatures + i] = bb.getFloat();
            fd.floatWeightFuncts[i] = readUTF(bb);
            String featureName = readUTF(bb);
            fd.featureNames.set(fd.numByteFeatures + fd.numShortFeatures + i, featureName);
        }
        return fd;
    }

    public static void writeBinaryTo(FeatureDefinition fd, DataOutput out) throws IOException {
        // TODO to avoid duplicate code, replace this with writeBinaryTo(out,
        // List<Integer>()) or some such

        // Section BYTEFEATURES
        out.writeInt(fd.numByteFeatures);
        for (int i = 0; i < fd.numByteFeatures; i++) {
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
            } else {
                out.writeFloat(0);
            }
            out.writeUTF(fd.getFeatureName(i));

            int numValues = fd.getNumberOfValues(i);
            byte numValuesEncoded = (byte) numValues; // an unsigned byte
            out.writeByte(numValuesEncoded);
            for (int b = 0; b < numValues; b++) {
                String value = fd.getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section SHORTFEATURES
        out.writeInt(fd.numShortFeatures);
        for (int i = fd.numByteFeatures; i < fd.numByteFeatures + fd.numShortFeatures; i++) {
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
            } else {
                out.writeFloat(0);
            }
            out.writeUTF(fd.getFeatureName(i));
            short numValues = (short) fd.getNumberOfValues(i);
            out.writeShort(numValues);
            for (short b = 0; b < numValues; b++) {
                String value = fd.getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section CONTINUOUSFEATURES
        out.writeInt(fd.numContinuousFeatures);
        for (int i = fd.numByteFeatures + fd.numShortFeatures; i < fd.numByteFeatures + fd.numShortFeatures
                + fd.numContinuousFeatures; i++) {
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
                out.writeUTF(fd.floatWeightFuncts[i - fd.numByteFeatures - fd.numShortFeatures]);
            } else {
                out.writeFloat(0);
                out.writeUTF("");
            }
            out.writeUTF(fd.getFeatureName(i));
        }
    }

    public static void writeBinaryTo(FeatureDefinition fd, DataOutput out, List<Integer> featuresToDrop)
            throws IOException {
        // how many features of each type are to be dropped
        int droppedByteFeatures = 0;
        int droppedShortFeatures = 0;
        int droppedContinuousFeatures = 0;
        for (int f : featuresToDrop) {
            if (f < fd.numByteFeatures) {
                droppedByteFeatures++;
            } else if (f < fd.numByteFeatures + fd.numShortFeatures) {
                droppedShortFeatures++;
            } else if (f < fd.numByteFeatures + fd.numShortFeatures + fd.numContinuousFeatures) {
                droppedContinuousFeatures++;
            }
        }
        // Section BYTEFEATURES
        out.writeInt(fd.numByteFeatures - droppedByteFeatures);
        for (int i = 0; i < fd.numByteFeatures; i++) {
            if (featuresToDrop.contains(i)) {
                continue;
            }
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
            } else {
                out.writeFloat(0);
            }
            out.writeUTF(fd.getFeatureName(i));

            int numValues = fd.getNumberOfValues(i);
            byte numValuesEncoded = (byte) numValues; // an unsigned byte
            out.writeByte(numValuesEncoded);
            for (int b = 0; b < numValues; b++) {
                String value = fd.getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section SHORTFEATURES
        out.writeInt(fd.numShortFeatures - droppedShortFeatures);
        for (int i = fd.numByteFeatures; i < fd.numByteFeatures + fd.numShortFeatures; i++) {
            if (featuresToDrop.contains(i)) {
                continue;
            }
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
            } else {
                out.writeFloat(0);
            }
            out.writeUTF(fd.getFeatureName(i));
            short numValues = (short) fd.getNumberOfValues(i);
            out.writeShort(numValues);
            for (short b = 0; b < numValues; b++) {
                String value = fd.getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section CONTINUOUSFEATURES
        out.writeInt(fd.numContinuousFeatures - droppedContinuousFeatures);
        for (int i = fd.numByteFeatures + fd.numShortFeatures; i < fd.numByteFeatures + fd.numShortFeatures
                + fd.numContinuousFeatures; i++) {
            if (featuresToDrop.contains(i)) {
                continue;
            }
            if (fd.featureWeights != null) {
                out.writeFloat(fd.featureWeights[i]);
                out.writeUTF(fd.floatWeightFuncts[i - fd.numByteFeatures - fd.numShortFeatures]);
            } else {
                out.writeFloat(0);
                out.writeUTF("");
            }
            out.writeUTF(fd.getFeatureName(i));
        }
    }

    public static String readUTF(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
        int utflen = readUnsignedShort(bb);
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        bb.get(bytearr);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127)
                break;
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                /* 0xxxxxxx */
                count++;
                chararr[chararr_count++] = (char) c;
                break;
            case 12:
            case 13:
                /* 110x xxxx 10xx xxxx */
                count += 2;
                if (count > utflen)
                    throw new UTFDataFormatException("malformed input: partial character at end");
                char2 = (int) bytearr[count - 1];
                if ((char2 & 0xC0) != 0x80)
                    throw new UTFDataFormatException("malformed input around byte " + count);
                chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                break;
            case 14:
                /* 1110 xxxx 10xx xxxx 10xx xxxx */
                count += 3;
                if (count > utflen)
                    throw new UTFDataFormatException("malformed input: partial character at end");
                char2 = (int) bytearr[count - 2];
                char3 = (int) bytearr[count - 1];
                if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                    throw new UTFDataFormatException("malformed input around byte " + (count - 1));
                chararr[chararr_count++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
                break;
            default:
                /* 10xx xxxx, 1111 xxxx */
                throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }

    public static int readUnsignedShort(ByteBuffer bb) throws BufferUnderflowException {
        int ch1 = bb.get() & 0xFF; // convert byte to unsigned byte
        int ch2 = bb.get() & 0xFF; // convert byte to unsigned byte
        return (ch1 << 8) + (ch2 << 0);
    }
}
