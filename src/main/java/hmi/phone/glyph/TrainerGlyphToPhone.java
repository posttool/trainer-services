package hmi.phone.glyph;

import hmi.ml.cart.CART;
import hmi.ml.cart.DecisionNode;
import hmi.ml.cart.LeafNode.StringAndFloatLeafNode;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;
import hmi.phone.Allophone;
import hmi.phone.AllophoneSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.C45PruneableClassifierTreeWithUnary;
import weka.classifiers.trees.j48.TreeConverter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class TrainerGlyphToPhone extends TrainerStringAlignment {

    protected AllophoneSet phSet;

    protected int context;
    protected boolean convertToLowercase;
    protected boolean considerStress;

    public TrainerGlyphToPhone(AllophoneSet aPhSet, boolean convertToLowercase, boolean considerStress, int context) {
        super();
        this.phSet = aPhSet;
        this.convertToLowercase = convertToLowercase;
        this.considerStress = considerStress;
        this.context = context;
    }

    public CART trainTree(int minLeafData) throws IOException {

        Map<String, List<String[]>> grapheme2align = new HashMap<String, List<String[]>>();
        for (String gr : this.graphemeSet) {
            grapheme2align.put(gr, new ArrayList<String[]>());
        }

        Set<String> phChains = new HashSet<String>();
        // for every alignment pair collect counts
        for (int i = 0; i < this.inSplit.size(); i++) {
            StringPair[] alignment = getAlignment(i);
            for (int inNr = 0; inNr < alignment.length; inNr++) {
                // System.err.println(alignment[inNr]);
                // quotation signs allow representation of empty string
                String outAlNr = "'" + alignment[inNr].getString2() + "'";
                if (outAlNr.length() > 5) // 5?
                    continue;
                phChains.add(outAlNr);

                // storing context and target
                String[] datapoint = new String[2 * context + 2];
                for (int ct = 0; ct < 2 * context + 1; ct++) {
                    int pos = inNr - context + ct;
                    if (pos >= 0 && pos < alignment.length) {
                        datapoint[ct] = alignment[pos].getString1();
                    } else {
                        datapoint[ct] = "null";
                    }
                }

                // set target
                datapoint[2 * context + 1] = outAlNr;

                // add datapoint
                grapheme2align.get(alignment[inNr].getString1()).add(datapoint);
            }
        }

        // for conversion need feature definition file
        FeatureDefinition fd = graphemeFeatureDef(phChains);

        int centerGrapheme = fd.getFeatureIndex("att" + (context + 1));

        DecisionNode.ByteDecisionNode root = new DecisionNode.ByteDecisionNode(centerGrapheme,
                fd.getNumberOfValues(centerGrapheme), fd);

        for (String gr : fd.getPossibleValues(centerGrapheme)) {
            System.out.println("      Training decision tree for: " + gr);

            ArrayList<Attribute> attributeDeclarations = new ArrayList<Attribute>();
            for (int att = 1; att <= context * 2 + 1; att++) {
                ArrayList<String> attVals = new ArrayList<String>();
                String featureName = "att" + att;
                for (String usableGrapheme : fd.getPossibleValues(fd.getFeatureIndex(featureName))) {
                    attVals.add(usableGrapheme);
                }
                attributeDeclarations.add(new Attribute(featureName, attVals));
            }

            List<String[]> datapoints = grapheme2align.get(gr);
            Set<String> graphSpecPh = new HashSet<String>();
            for (String[] dp : datapoints) {
                graphSpecPh.add(dp[dp.length - 1]);
            }

            ArrayList<String> targetVals = new ArrayList<String>();
            for (String phc : graphSpecPh) {// todo: use either fd of phChains
                targetVals.add(phc);
            }
            attributeDeclarations.add(new Attribute(GlyphToPhone.PREDICTED_STRING_FEATURENAME, targetVals));

            Instances data = new Instances(gr, attributeDeclarations, 0);
            for (String[] point : datapoints) {
                Instance currInst = new DenseInstance(data.numAttributes());
                currInst.setDataset(data);
                for (int i = 0; i < point.length; i++) {
                    currInst.setValue(i, point[i]);
                }
                data.add(currInst);
            }

            data.setClassIndex(data.numAttributes() - 1);

            // build the tree without using the J48 wrapper class
            // params are:
            // binary split selection with minimum x instances at the leaves,
            // tree is pruned, confidenced value, subtree raising,
            // cleanup, don't collapse
            C45PruneableClassifierTree decisionTree;
            try {
                decisionTree = new C45PruneableClassifierTreeWithUnary(
                        new BinC45ModelSelection(minLeafData, data, false), true, 0.25f, true, true, false);
                decisionTree.buildClassifier(data);
            } catch (Exception e) {
                throw new RuntimeException("couldn't train decisiontree using weka: ", e);
            }

            CART t = TreeConverter.c45toStringCART(decisionTree, fd, data);
            root.addChild(t.getRootNode());
        }
        root.countData();//duh

        Properties props = new Properties();
        props.setProperty("lowercase", String.valueOf(convertToLowercase));
        props.setProperty("stress", String.valueOf(considerStress));
        props.setProperty("context", String.valueOf(context));

        return new CART(root, fd, props);
    }

    private FeatureDefinition graphemeFeatureDef(Set<String> phChains) throws IOException {

        String lineBreak = System.getProperty("line.separator");

        StringBuilder fdString = new StringBuilder("ByteValuedFeatureProcessors");
        fdString.append(lineBreak);

        // add attribute features
        for (int att = 1; att <= context * 2 + 1; att++) {
            fdString.append("att").append(att);
            for (String gr : this.graphemeSet) {
                fdString.append(" ").append(gr);
            }
            fdString.append(lineBreak);
        }
        fdString.append("ShortValuedFeatureProcessors").append(lineBreak);

        // add class features
        fdString.append(GlyphToPhone.PREDICTED_STRING_FEATURENAME);
        for (String ph : phChains) {
            fdString.append(" ").append(ph);
        }

        fdString.append(lineBreak);

        fdString.append("ContinuousFeatureProcessors").append(lineBreak);

        BufferedReader featureReader = new BufferedReader(new StringReader(fdString.toString()));

        return new FeatureDefinition(featureReader, false);
    }

    /**
     * 
     * reads in a lexicon in text format, lines are of the kind:
     * 
     * graphemechain | phonechain | otherinformation
     * 
     * Stress is optionally preserved, marking the first vowel of a stressed
     * syllable with "1".
     * 
     * @param lexicon
     *            reader with lines of lexicon
     * @param splitPattern
     *            a regular expression used for identifying the field separator
     *            in each line.
     */
    public void readLexicon(BufferedReader lexicon, String splitPattern) throws IOException {

        String line;

        while ((line = lexicon.readLine()) != null) {
            String[] lineParts = line.trim().split(splitPattern);
            String graphStr = lineParts[0];
            if (convertToLowercase)
                graphStr = graphStr.toLowerCase(phSet.getLocale());
            graphStr = graphStr.replaceAll("['-.]", "");

            // remove all secondary stress markers
            String phonStr = lineParts[1].replaceAll(",", "");
            String[] syllables = phonStr.split("-");
            List<String> separatedPhones = new ArrayList<String>();
            List<String> separatedGraphemes = new ArrayList<String>();
            String currPh;
            for (String syl : syllables) {
                boolean stress = false;
                if (syl.startsWith("'")) {
                    syl = syl.substring(1);
                    stress = true;
                }
                for (Allophone ph : phSet.splitIntoAllophones(syl)) {
                    currPh = ph.name();
                    if (stress && considerStress && ph.isVowel()) {
                        currPh += "1";
                        stress = false;
                    }
                    separatedPhones.add(currPh);
                }// ... for each allophone
            }

            for (int i = 0; i < graphStr.length(); i++) {
                this.graphemeSet.add(graphStr.substring(i, i + 1));
                separatedGraphemes.add(graphStr.substring(i, i + 1));
            }
            this.addAlreadySplit(separatedGraphemes, separatedPhones);
        }
        // an entry for "null", which maps to the empty string:
        this.addAlreadySplit(new String[] { "null" }, new String[] { "" });

        System.out.println("readLexicon complete " + inSplit.size());
    }

    /**
     * reads in a lexicon in text format, lines are of the kind:
     * 
     * graphemechain | phonechain | otherinformation
     * 
     * Stress is optionally preserved, marking the first vowel of a stressed
     * syllable with "1".
     * 
     */
    public void readLexicon(HashMap<String, String> lexicon) {

        Iterator<String> it = lexicon.keySet().iterator();
        while (it.hasNext()) {
            String graphStr = it.next();

            // remove all secondary stress markers
            String phonStr = lexicon.get(graphStr).replaceAll(",", "");
            if (convertToLowercase)
                graphStr = graphStr.toLowerCase(phSet.getLocale());
            graphStr = graphStr.replaceAll("['-.]", "");

            String[] syllables = phonStr.split("-");
            List<String> separatedPhones = new ArrayList<String>();
            List<String> separatedGraphemes = new ArrayList<String>();
            String currPh;
            for (String syl : syllables) {
                boolean stress = false;
                if (syl.startsWith("'")) {
                    syl = syl.substring(1);
                    stress = true;
                }
                for (Allophone ph : phSet.splitIntoAllophones(syl)) {
                    currPh = ph.name();
                    if (stress && considerStress && ph.isVowel()) {
                        currPh += "1";
                        stress = false;
                    }
                    separatedPhones.add(currPh);
                }// ... for each allophone
            }

            for (int i = 0; i < graphStr.length(); i++) {
                this.graphemeSet.add(graphStr.substring(i, i + 1));
                separatedGraphemes.add(graphStr.substring(i, i + 1));
            }
            this.addAlreadySplit(separatedGraphemes, separatedPhones);
        }
        // Need one entry for the "null" grapheme, which maps to the empty
        // string:
        this.addAlreadySplit(new String[] { "null" }, new String[] { "" });
    }

    public static void main(String[] args) throws Exception {
        String BP = "/Users/posttool/Documents/github/la/src/test/resources/en_US/";

        AllophoneSet as = AllophoneSet.getAllophoneSet(BP + "phones.xml");
        TrainerGlyphToPhone tp = new TrainerGlyphToPhone(as, true, true, 2);

        BufferedReader lexReader = new BufferedReader(new InputStreamReader(new FileInputStream(BP + "dict.txt"),
                "UTF-8"));// ISO-8859-1

        // read lexicon for training
        tp.readLexicon(lexReader, "\\s*\\|\\s*");

        // make some alignment iterations
        for (int i = 0; i < 5; i++) {
            System.out.println("iteration " + i);
            tp.alignIteration();
        }

        CART st = tp.trainTree(20);
        System.out.println(st);

        // CARTWriter cw = new CARTWriter();
        // cw.dump(st, "english/trees/");
        predictPronunciation(as, st, "this");
        predictPronunciation(as, st, "however");
        predictPronunciation(as, st, "youbelaline");
    }

    public static String predictPronunciation(AllophoneSet allophoneSet, CART tree, String graphemes) {
        boolean convertToLowercase = true;
        int context = 2;
        if (convertToLowercase)
            graphemes = graphemes.toLowerCase(allophoneSet.getLocale());
        FeatureDefinition featureDefinition = tree.getFeatureDefinition();
        int indexPredictedFeature = featureDefinition.getFeatureIndex(GlyphToPhone.PREDICTED_STRING_FEATURENAME);

        String returnStr = "";

        for (int i = 0; i < graphemes.length(); i++) {
            byte[] byteFeatures = new byte[2 * context + 1];
            for (int fnr = 0; fnr < 2 * context + 1; fnr++) {
                int pos = i - context + fnr;
                String grAtPos = (pos < 0 || pos >= graphemes.length()) ? "null" : graphemes.substring(pos, pos + 1);
                try {
                    byteFeatures[fnr] = tree.getFeatureDefinition().getFeatureValueAsByte(fnr, grAtPos);
                    // ... can also try to call explicit:
                    // features[fnr] = this.fd.getFeatureValueAsByte("att"+fnr,
                    // cg.substr(pos)
                } catch (IllegalArgumentException iae) {
                    // Silently ignore unknown characters
                    byteFeatures[fnr] = tree.getFeatureDefinition().getFeatureValueAsByte(fnr, "null");
                }
            }

            FeatureVector fv = new FeatureVector(byteFeatures, new short[] {}, new float[] {}, 0);

            StringAndFloatLeafNode leaf = (StringAndFloatLeafNode) tree.interpretToNode(fv, 0);
            String prediction = leaf.mostProbableString(featureDefinition, indexPredictedFeature);
            returnStr += prediction.substring(1, prediction.length() - 1);
        }
        System.out.println(">" + returnStr);
        return returnStr;

    }

}
// http://people.ds.cam.ac.uk/ssb22/gradint/lexconvert.html