package hmi.phone.tized;

import hmi.ml.cart.CART;
import hmi.ml.cart.DecisionNode;
import hmi.ml.cart.LeafNode.StringAndFloatLeafNode;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureIO;
import hmi.ml.feature.FeatureVector;
import hmi.ml.string.StringAligner;
import hmi.ml.string.StringPair;
import hmi.phone.PhoneEl;
import hmi.phone.PhoneSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public class TrainerGlyphPhoneAligner {

    protected PhoneSet phSet;
    protected Set<String> graphemeSet;

    protected StringAligner aligner;

    protected int context;
    protected boolean convertToLowercase;
    protected boolean considerStress;

    protected static String NULL = "null";

    public TrainerGlyphPhoneAligner(PhoneSet aPhSet, boolean convertToLowercase, boolean considerStress, int context) {
        super();
        this.phSet = aPhSet;
        this.graphemeSet = new HashSet<String>();
        this.graphemeSet.add(NULL);// "null" is for all phone features
        this.aligner = new StringAligner();
        this.convertToLowercase = convertToLowercase;
        this.considerStress = considerStress;
        this.context = context;
    }

    public CART trainTree(int minLeafData) throws IOException {

        for (int i = 0; i < 5; i++) {
            System.out.println("alignment iteration " + i);
            aligner.alignIteration();
        }

        Map<String, List<String[]>> grapheme2align = new HashMap<String, List<String[]>>();
        for (String gr : this.graphemeSet) {
            grapheme2align.put(gr, new ArrayList<String[]>());
        }

        Set<String> phChains = new HashSet<String>();
        // for every alignment pair collect counts
        for (int i = 0; i < aligner.size(); i++) {
            StringPair[] alignment = aligner.get(i);
            for (int inNr = 0; inNr < alignment.length; inNr++) {
                // System.out.println(alignment[inNr]);
                // empty quotes represent empty string
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
                        datapoint[ct] = NULL;
                    }
                }

                // set target & add datapoint
                datapoint[2 * context + 1] = outAlNr;
                grapheme2align.get(alignment[inNr].getString1()).add(datapoint);
            }
        }

        // for conversion need feature definition file
        FeatureDefinition fd = graphemeFeatureDef(phChains);

        int centerGrapheme = fd.getFeatureIndex("att" + (context + 1));

        DecisionNode.ByteDecisionNode root = new DecisionNode.ByteDecisionNode(centerGrapheme,
                fd.getNumberOfValues(centerGrapheme), fd);

        for (String gr : fd.getPossibleValues(centerGrapheme)) {
            System.out.println("training decision tree for: " + gr);

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
            for (String phc : graphSpecPh) { // todo: use either fd of phChains
                targetVals.add(phc);
            }
            attributeDeclarations.add(new Attribute(TrainedPhonetizer.PREDICTED_STRING_FEATURENAME, targetVals));

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

            C45PruneableClassifierTree decisionTree;
            try {
                decisionTree = new C45PruneableClassifierTreeWithUnary(new BinC45ModelSelection(minLeafData, data,
                        false), true, 0.25f, true, true, false);
                decisionTree.buildClassifier(data);
            } catch (Exception e) {
                throw new RuntimeException("couldn't train decisiontree using weka: ", e);
            }

            CART t = TreeConverter.c45toStringCART(decisionTree, fd, data);
            root.addChild(t.getRootNode());
        }
        // for test... required by tree.interpretToNode
        root.countData();
        // is it needed when serializing model?

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
        fdString.append(TrainedPhonetizer.PREDICTED_STRING_FEATURENAME);
        for (String ph : phChains) {
            fdString.append(" ").append(ph);
        }

        fdString.append(lineBreak);

        fdString.append("ContinuousFeatureProcessors").append(lineBreak);

        BufferedReader featureReader = new BufferedReader(new StringReader(fdString.toString()));

        return FeatureIO.read(featureReader, false);
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
                for (PhoneEl ph : phSet.splitIntoPhones(syl)) {
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
            aligner.add(separatedGraphemes, separatedPhones);
        }
        // an entry for "null", which maps to the empty string:
        aligner.add(new String[] { NULL }, new String[] { "" });

        System.out.println("readLexicon complete " + aligner.size());
    }

    public static String predictPronunciation(PhoneSet allophoneSet, CART tree, int context, String graphemes) {
        boolean convertToLowercase = true;
        if (convertToLowercase)
            graphemes = graphemes.toLowerCase(allophoneSet.getLocale());
        FeatureDefinition featureDefinition = tree.getFeatureDefinition();
        int indexPredictedFeature = featureDefinition.getFeatureIndex(TrainedPhonetizer.PREDICTED_STRING_FEATURENAME);

        String returnStr = "";

        for (int i = 0; i < graphemes.length(); i++) {
            byte[] byteFeatures = new byte[2 * context + 1];
            for (int fnr = 0; fnr < 2 * context + 1; fnr++) {
                int pos = i - context + fnr;
                String grAtPos = (pos < 0 || pos >= graphemes.length()) ? NULL : graphemes.substring(pos, pos + 1);
                try {
                    byteFeatures[fnr] = tree.getFeatureDefinition().getFeatureValueAsByte(fnr, grAtPos);
                    // ... can also try to call explicit:
                    // features[fnr] = fd.getFeatureValueAsByte("att" + fnr,
                    // cg.substr(pos)
                } catch (IllegalArgumentException iae) {
                    // Silently ignore unknown characters
                    byteFeatures[fnr] = tree.getFeatureDefinition().getFeatureValueAsByte(fnr, NULL);
                }
            }

            FeatureVector fv = new FeatureVector(byteFeatures, new short[] {}, new float[] {}, 0);

            StringAndFloatLeafNode leaf = (StringAndFloatLeafNode) tree.interpretToNode(fv, 0);
            String prediction = leaf.mostProbableString(featureDefinition, indexPredictedFeature);
            returnStr += prediction.substring(1, prediction.length() - 1);
        }
        System.out.println("> " + returnStr);
        return returnStr;

    }

    // MAIN & TEST
    private static String BP = "/Users/posttool/Documents/github/la/src/test/resources/en_US/";

    public static void main(String[] args) throws Exception {
        int ctx = 3;
        PhoneSet as = PhoneSet.getPhoneSet(BP + "phones.xml");
        TrainerGlyphPhoneAligner tp = new TrainerGlyphPhoneAligner(as, true, true, ctx);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(BP + "dict.txt")));
        tp.readLexicon(rdr, "\\s*\\|\\s*");

        CART st = tp.trainTree(10);

        // CARTWriter cw = new CARTWriter();
        // cw.dump(st, "dict.crt");
        predictPronunciation(as, st, ctx, "this");
        predictPronunciation(as, st, ctx, "however");
        predictPronunciation(as, st, ctx, "youbelaline");
        predictPronunciation(as, st, ctx, "Native");
        predictPronunciation(as, st, ctx, "speakers");
        predictPronunciation(as, st, ctx, "Native speakers of a given language usually perceive one phoneme in "
                + "that language as a single distinctive sound, and are both unaware of and even shocked "
                + "by the allophone variations used to pronounce single phonemes.");
    }

}
// http://people.ds.cam.ac.uk/ssb22/gradint/lexconvert.html