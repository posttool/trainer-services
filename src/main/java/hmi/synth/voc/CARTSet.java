package hmi.synth.voc;

import hmi.ml.cart.CART;
import hmi.ml.cart.LeafNode.PdfLeafNode;
import hmi.ml.cart.io.HTSCARTReader;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;
import hmi.synth.voc.PData.PdfFileFormat;

import java.io.IOException;

public class CARTSet {

    private CART[] durTree; // CART trees for duration
    private CART[] lf0Tree; // CART trees for log F0
    private CART[] mgcTree; // CART trees for spectrum
    private CART[] strTree; // CART trees for strengths
    private CART[] magTree; // CART trees for Fourier magnitudes

    private int numStates; /* # of HMM states for individual HMM */
    private int lf0Stream; /* # of stream for log f0 modeling */
    private int mcepVsize; /* vector size for mcep modeling */
    private int strVsize; /* vector size for strengths modeling */
    private int magVsize; /* vector size for Fourier magnitudes modeling */

    public int getNumStates() {
        return numStates;
    }

    public int getLf0Stream() {
        return lf0Stream;
    }

    public int getMcepVsize() {
        return mcepVsize;
    }

    public int getStrVsize() {
        return strVsize;
    }

    public int getMagVsize() {
        return magVsize;
    }

    public int getVsize(PData.FeatureType type) {
        switch (type) {
        case MGC:
            return mcepVsize;
        case STR:
            return strVsize;
        case MAG:
            return magVsize;
        default:
            return 1; // DUR and LF0
        }
    }

    /** Loads all the CART trees */
    public void loadTreeSet(PData htsData, FeatureDefinition featureDef, PhoneTranslator trickyPhones)
            throws IOException, Exception {
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = trickyPhones;

        HTSCARTReader htsReader = new HTSCARTReader();
        /*
         * DUR, LF0 and Mgc are required as minimum for generating voice. The
         * duration tree has only one state. The size of the vector in duration
         * is the number of states.
         */
        if (htsData.getTreeDurStream() != null) {
            System.out.println("Loading duration tree...");
            durTree = htsReader.load(1, htsData.getTreeDurStream(), htsData.getPdfDurStream(), PdfFileFormat.dur,
                    featureDef, phTranslator);
            numStates = htsReader.getVectorSize();
        }

        if (htsData.getTreeLf0Stream() != null) {
            System.out.println("Loading log F0 tree...");
            lf0Tree = htsReader.load(numStates, htsData.getTreeLf0Stream(), htsData.getPdfLf0Stream(),
                    PdfFileFormat.lf0, featureDef, phTranslator);
            lf0Stream = htsReader.getVectorSize();
        }

        if (htsData.getTreeMgcStream() != null) {
            System.out.println("Loading mgc tree...");
            mgcTree = htsReader.load(numStates, htsData.getTreeMgcStream(), htsData.getPdfMgcStream(),
                    PdfFileFormat.mgc, featureDef, phTranslator);
            mcepVsize = htsReader.getVectorSize();
        }

        /* STR and MAG are optional for generating mixed excitation */
        if (htsData.getTreeStrStream() != null) {
            System.out.println("Loading str tree...");
            strTree = htsReader.load(numStates, htsData.getTreeStrStream(), htsData.getPdfStrStream(),
                    PdfFileFormat.str, featureDef, phTranslator);
            strVsize = htsReader.getVectorSize();
        }
        if (htsData.getTreeMagStream() != null) {
            System.out.println("Loading mag tree...");
            magTree = htsReader.load(numStates, htsData.getTreeMagStream(), htsData.getPdfMagStream(),
                    PdfFileFormat.mag, featureDef, phTranslator);
            magVsize = htsReader.getVectorSize();
        }
    }

    /***
     * Searches fv in durTree CART[] set of trees, per state, and fill the
     * information in the HTSModel m.
     * 
     * @param m
     *            HTSModel where mean and variances per state are copied
     * @param fv
     *            context feature vector
     * @param htsData
     *            HMMData with configuration settings
     * @return duration
     * @throws Exception
     */
    public double searchDurInCartTree(PModel m, FeatureVector fv, PData htsData, double diffdur) {
        return searchDurInCartTree(m, fv, htsData, false, false, diffdur);
    }

    public double searchDurInCartTree(PModel m, FeatureVector fv, PData htsData, boolean firstPh, boolean lastPh,
            double diffdur) {
        double data, dd;
        double rho = htsData.getRho();
        double durscale = htsData.getDurationScale();
        double meanVector[], varVector[];
        // the duration tree has only one state
        PdfLeafNode node = (PdfLeafNode) durTree[0].interpretToNode(fv, 0);

        meanVector = node.getMean();
        varVector = node.getVariance();

        dd = diffdur;
        // in duration the length of the vector is the number of states.
        for (int s = 0; s < numStates; s++) {
            data = (meanVector[s] + rho * varVector[s]) * durscale;

            /*
             * check if the model is initial/final pause, if so reduce the
             * length of the pause to 10% of the calculated value.
             */
            // if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
            // data = data * 0.1;

            m.setDur(s, (int) (data + dd + 0.5));
            if (m.getDur(s) < 1)
                m.setDur(s, 1);

            // System.out.format("   state=%d  dur=%d  dd=%f  mean=%f  vari=%f \n",
            // s, m.getDur(s), dd, meanVector[s],
            // varVector[s]);
            m.incrTotalDur(m.getDur(s));
            dd += data - m.getDur(s);
        }
        m.setDurError(dd);
        return dd;

    }

    /***
     * Searches fv in Lf0Tree CART[] set of trees, per state, and fill the
     * information in the HTSModel m.
     * 
     * @param m
     *            HTSModel where mean and variances per state are copied
     * @param fv
     *            context feature vector
     * @param featureDef
     *            Feature definition
     * @throws Exception
     */
    public void searchLf0InCartTree(PModel m, FeatureVector fv, FeatureDefinition featureDef, double uvthresh) {
        for (int s = 0; s < numStates; s++) {
            PdfLeafNode node = (PdfLeafNode) lf0Tree[s].interpretToNode(fv, 1);
            m.setLf0Mean(s, node.getMean());
            m.setLf0Variance(s, node.getVariance());
            // set voiced or unvoiced
            if (node.getVoicedWeight() > uvthresh)
                m.setVoiced(s, true);
            else
                m.setVoiced(s, false);
        }
        // m.printLf0Mean();
    }

    /***
     * Searches fv in mgcTree CART[] set of trees, per state, and fill the
     * information in the HTSModel m.
     * 
     * @param m
     *            HTSModel where mean and variances per state are copied
     * @param fv
     *            context feature vector
     * @param featureDef
     *            Feature definition
     * @throws Exception
     */
    public void searchMgcInCartTree(PModel m, FeatureVector fv, FeatureDefinition featureDef) {
        for (int s = 0; s < numStates; s++) {
            PdfLeafNode node = (PdfLeafNode) mgcTree[s].interpretToNode(fv, 1);
            m.setMcepMean(s, node.getMean());
            m.setMcepVariance(s, node.getVariance());
        }
    }

    /***
     * Searches fv in StrTree CART[] set of trees, per state, and fill the
     * information in the HTSModel m.
     * 
     * @param m
     *            HTSModel where mean and variances per state are copied
     * @param fv
     *            context feature vector
     * @param featureDef
     *            Feature definition
     * @throws Exception
     */
    public void searchStrInCartTree(PModel m, FeatureVector fv, FeatureDefinition featureDef) {
        for (int s = 0; s < numStates; s++) {
            PdfLeafNode node = (PdfLeafNode) strTree[s].interpretToNode(fv, 1);
            m.setStrMean(s, node.getMean());
            m.setStrVariance(s, node.getVariance());
        }
    }

    /***
     * Searches fv in MagTree CART[] set of trees, per state, and fill the
     * information in the HTSModel m.
     * 
     * @param m
     *            HTSModel where mean and variances per state are copied
     * @param fv
     *            context feature vector
     * @param featureDef
     *            Feature definition
     * @throws Exception
     */
    public void searchMagInCartTree(PModel m, FeatureVector fv, FeatureDefinition featureDef) {
        for (int s = 0; s < numStates; s++) {
            PdfLeafNode node = (PdfLeafNode) magTree[s].interpretToNode(fv, 1);
            m.setMagMean(s, node.getMean());
            m.setMagVariance(s, node.getVariance());
        }
    }

    /**
     * creates a HTSModel (pre-HMM optimization vector data for all parameter
     * streams of a given phoneme) given a feature vector compare with original
     */
    public PModel generateHTSModel(PData htsData, FeatureDefinition feaDef, FeatureVector fv, double oldErr) {
        PModel m = new PModel(getNumStates());
        String phoneFeature = fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef);
        m.setPhoneName(phoneFeature);
        try {

            double diffDur = searchDurInCartTree(m, fv, htsData, oldErr);
            m.setDurError(diffDur);
            // m.setTotalDurMillisec((int)(fperiodmillisec * m.getTotalDur()));
            // nobody ever uses totaldurmillisec and it's really
            // redundant to gettotaldur

            /*
             * Find pdf for LF0, this function sets the pdf for each state. here
             * it is also set whether the model is voiced or not
             */
            // if ( ! htsData.getUseUnitDurationContinuousFeature() )
            // Here according to the HMM models it is decided whether the states
            // of this model are voiced or unvoiced
            // even if f0 is taken from xml here we need to set the
            // voived/unvoiced values per model and state
            searchLf0InCartTree(m, fv, feaDef, htsData.getUV());

            /* Find pdf for MGC, this function sets the pdf for each state. */
            searchMgcInCartTree(m, fv, feaDef);

            /*
             * Find pdf for strengths, this function sets the pdf for each
             * state.
             */
            if (htsData.getTreeStrStream() != null)
                searchStrInCartTree(m, fv, feaDef);

            /*
             * Find pdf for Fourier magnitudes, this function sets the pdf for
             * each state.
             */
            if (htsData.getTreeMagStream() != null)
                searchMagInCartTree(m, fv, feaDef);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return m;
    }

}
