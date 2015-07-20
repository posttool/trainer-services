package hmi.synth.voc;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class PData {

    public static final int HTS_NUMMTYPE = 5;

    public static enum FeatureType {
        DUR, // duration
        MGC, // MGC mel-generalized cepstral coefficients
        LF0, // log(fundamental frequency)
        STR, // strength of excitation?
        MAG, // fourier magnitudes for pulse generation
    }

    ;

    public enum PdfFileFormat {
        dur, lf0, mgc, str, mag, join
    }

    ;

    /**
     * Global variables for some functions, initialised with default values, so
     * these values can be loaded from a configuration file.
     */
    private int rate = 16000; /* sampling rate default: 16Khz */
    private int fperiod = 80; /*
                               * frame period or frame shift (point) default:
                               * 0.005sec = rate*0.005 = 80
                               */
    private double rho = 0.0; /* variable for speaking rate control */

    /*
     * MGC: stage=gamma=0.0 alpha=0.42 linear gain LSP: gamma>0.0 LSP: gamma=1.0
     * alpha=0.0 Mel-LSP: gamma=1.0 alpha=0.42 MGC-LSP: gamma=3.0 alpha=0.42
     */
    private int stage = 0; /* defines gamma=-1/stage : if stage=0 then Gamma=0 */
    private double alpha = 0.55; // 0.42; /* variable for frequency warping
    // parameter */
    private double beta = 0.0; /* variable for postfiltering */
    private boolean useLogGain = false; /* log gain flag (for LSP) */

    private double uv = 0.5; /* variable for U/V threshold */
    private boolean algnst = false; /* use state level alignment for duration */
    private boolean algnph = false; /* use phone level alignment for duration */
    private boolean useMixExc = true; /* use Mixed Excitation */
    private boolean useFourierMag = false; /*
                                            * use Fourier magnitudes for pulse
                                            * generation
                                            */

    /**
     * Global variance (GV) settings
     */
    private boolean useGV = false; /*
                                    * use global variance in parameter
                                    * generation
                                    */
    private boolean useContextDependentGV = false; /*
                                                    * Variable for allowing
                                                    * context-dependent GV for
                                                    * sil
                                                    */
    private boolean gvMethodGradient = true; /*
                                              * GV method: gradient or
                                              * derivative (default gradient)
                                              */

    /*
     * Max number of GV iterations when using gradient method, for derivative 5
     * is used by default
     */
    private int maxMgcGvIter = 100;
    private int maxLf0GvIter = 100;
    private int maxStrGvIter = 100;
    private int maxMagGvIter = 100;

    /* GV weights for each parameter: between 0.0-2.0 */
    private double gvWeightMgc = 1.0;
    private double gvWeightLf0 = 1.0;
    private double gvWeightStr = 1.0;
    private double gvWeightMag = 1.0;

    private boolean useAcousticModels = false;

    /**
     * variables for controlling generation of speech in the vocoder these
     * variables have default values but can be fixed and read from the audio
     * effects component. [Default][min--max]
     */
    private double f0Std = 1.0; /*
                                 * variable for f0 control, multiply f0
                                 * [1.0][0.0--5.0]
                                 */
    private double f0Mean = 0.0; /*
                                  * variable for f0 control, add f0
                                  * [0.0][0.0--100.0]
                                  */
    private double length = 0.0; /* total number of frame for generated speech */
    /* length of generated speech (in seconds) [N/A][0.0--30.0] */
    private double durationScale = 1.0; /*
                                         * less than 1.0 is faster and more than
                                         * 1.0 is slower, min=0.1 max=3.0
                                         */

    /**
     * Tree files and TreeSet object
     */
    private InputStream treeDurStream; /* durations tree file */
    private InputStream treeLf0Stream; /* lf0 tree file */
    private InputStream treeMgcStream; /* Mgc tree file */
    private InputStream treeStrStream; /* Strengths tree file */
    private InputStream treeMagStream; /* Fourier magnitudes tree file */

    /**
     * CartTreeSet contains the tree-xxx.inf, xxx: dur, lf0, Mgc, str and mag
     * these are all the trees trained for a particular voice. the Cart tree
     * also contains the corresponding pdfs.
     */
    private CARTSet cart = new CARTSet();

    /**
     * HMM pdf model files and ModelSet object
     */
    private InputStream pdfDurStream; /* durations Pdf file */
    private InputStream pdfLf0Stream; /* lf0 Pdf file */
    private InputStream pdfMgcStream; /* Mgc Pdf file */
    private InputStream pdfStrStream; /* Strengths Pdf file */
    private InputStream pdfMagStream; /* Fourier magnitudes Pdf file */

    /** GV pdf files */
    /**
     * Global variance file, it contains one global mean vector and one global
     * diagonal covariance vector
     */
    private InputStream pdfLf0GVStream; /* lf0 GV pdf file */
    private InputStream pdfMgcGVStream; /* Mgc GV pdf file */
    private InputStream pdfStrGVStream; /* Str GV pdf file */
    private InputStream pdfMagGVStream; /* Mag GV pdf file */
    private InputStream switchGVStream; /*
                                         * File for allowing context dependent
                                         * GV.
                                         */
    /*
     * This file contains the phones, sil or pause, for which GV is not
     * calculated (not used yet)
     */
    /*
     * this tree does not have a corresponding pdf file, because it just
     * indicate which labels in context to avoid for GV.
     */

    /**
     * GVModelSet contains the global covariance and mean for lf0, mgc, str and
     * mag
     */
    private GVModelSet gv = new GVModelSet();

    /**
     * Variables for mixed excitation
     */
    private int numFilters;
    private int orderFilters;
    private double mixFilters[][]; /* filters for mixed excitation */


    public int getRate() {
        return rate;
    }

    public int getFperiod() {
        return fperiod;
    }

    public double getRho() {
        return rho;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public int getStage() {
        return stage;
    }

    public double getGamma() {
        return (stage != 0) ? -1.0 / stage : 0.0;
    }

    public boolean getUseLogGain() {
        return useLogGain;
    }

    public double getUV() {
        return uv;
    }

    public boolean getAlgnst() {
        return algnst;
    }

    public boolean getAlgnph() {
        return algnph;
    }

    public double getF0Std() {
        return f0Std;
    }

    public double getF0Mean() {
        return f0Mean;
    }

    public double getLength() {
        return length;
    }

    public double getDurationScale() {
        return durationScale;
    }

    public InputStream getTreeDurStream() {
        return treeDurStream;
    }

    public InputStream getTreeLf0Stream() {
        return treeLf0Stream;
    }

    public InputStream getTreeMgcStream() {
        return treeMgcStream;
    }

    public InputStream getTreeStrStream() {
        return treeStrStream;
    }

    public InputStream getTreeMagStream() {
        return treeMagStream;
    }


    public InputStream getPdfDurStream() {
        return pdfDurStream;
    }

    public InputStream getPdfLf0Stream() {
        return pdfLf0Stream;
    }

    public InputStream getPdfMgcStream() {
        return pdfMgcStream;
    }

    public InputStream getPdfStrStream() {
        return pdfStrStream;
    }

    public InputStream getPdfMagStream() {
        return pdfMagStream;
    }

    public boolean getUseAcousticModels() {
        return useAcousticModels;
    }

    public void setUseAcousticModels(boolean bval) {
        useAcousticModels = bval;
    }

    public boolean getUseMixExc() {
        return useMixExc;
    }

    public boolean getUseFourierMag() {
        return useFourierMag;
    }

    public boolean getUseGV() {
        return useGV;
    }

    public boolean getUseContextDependentGV() {
        return useContextDependentGV;
    }

    public boolean getGvMethodGradient() {
        return gvMethodGradient;
    }

    public int getMaxMgcGvIter() {
        return maxMgcGvIter;
    }

    public int getMaxLf0GvIter() {
        return maxLf0GvIter;
    }

    public int getMaxStrGvIter() {
        return maxStrGvIter;
    }

    public int getMaxMagGvIter() {
        return maxMagGvIter;
    }

    public double getGvWeightMgc() {
        return gvWeightMgc;
    }

    public double getGvWeightLf0() {
        return gvWeightLf0;
    }

    public double getGvWeightStr() {
        return gvWeightStr;
    }

    public double getGvWeightMag() {
        return gvWeightMag;
    }

    public InputStream getPdfLf0GVStream() {
        return pdfLf0GVStream;
    }

    public InputStream getPdfMgcGVStream() {
        return pdfMgcGVStream;
    }

    public InputStream getPdfStrGVStream() {
        return pdfStrGVStream;
    }

    public InputStream getPdfMagGVStream() {
        return pdfMagGVStream;
    }

    public InputStream getSwitchGVStream() {
        return switchGVStream;
    }

    public int getNumFilters() {
        return numFilters;
    }

    public int getOrderFilters() {
        return orderFilters;
    }

    public double[][] getMixFilters() {
        return mixFilters;
    }

    public void setRate(int ival) {
        rate = ival;
    }

    public void setFperiod(int ival) {
        fperiod = ival;
    }

    public void setAlpha(double dval) {
        alpha = dval;
    }

    public void setBeta(double dval) {
        beta = dval;
    }

    public void setStage(int ival) {
        stage = ival;
    }

    public void setUseLogGain(boolean bval) {
        useLogGain = bval;
    }

    /*
     * These variables have default values but can be modified with setting in
     * audio effects component.
     */
    public void setF0Std(double dval) {
        /* default=1.0, min=0.0, max=3.0 */
        if (dval >= 0.0 && dval <= 3.0)
            f0Std = dval;
        else
            f0Std = 1.0;
    }

    public void setF0Mean(double dval) {
        /* default=0.0, min=-300.0, max=300.0 */
        if (dval >= -300.0 && dval <= 300.0)
            f0Mean = dval;
        else
            f0Mean = 0.0;
    }

    public void setLength(double dval) {
        length = dval;
    }

    public void setDurationScale(double dval) {
        /* default=1.0, min=0.1, max=3.0 */
        if (dval >= 0.1 && dval <= 3.0)
            durationScale = dval;
        else
            durationScale = 1.0;

    }

    public CARTSet getCartTreeSet() {
        return cart;
    }

    public GVModelSet getGVModelSet() {
        return gv;
    }

    public void setPdfStrStream(InputStream str) {
        pdfStrStream = str;
    }

    public void setPdfMagStream(InputStream mag) {
        pdfMagStream = mag;
    }

    public void setUseMixExc(boolean bval) {
        useMixExc = bval;
    }

    public void setUseFourierMag(boolean bval) {
        useFourierMag = bval;
    }

    public void setUseGV(boolean bval) {
        useGV = bval;
    }

    public void setUseContextDepenendentGV(boolean bval) {
        useContextDependentGV = bval;
    }

    public void setGvMethod(String sval) {
        if (sval.contentEquals("gradient"))
            gvMethodGradient = true;
        else
            gvMethodGradient = false; // then simple derivative method is used
    }

    public void setMaxMgcGvIter(int val) {
        maxMgcGvIter = val;
    }

    public void setMaxLf0GvIter(int val) {
        maxLf0GvIter = val;
    }

    public void setMaxStrGvIter(int val) {
        maxStrGvIter = val;
    }

    public void setGvWeightMgc(double dval) {
        gvWeightMgc = dval;
    }

    public void setGvWeightLf0(double dval) {
        gvWeightLf0 = dval;
    }

    public void setGvWeightStr(double dval) {
        gvWeightStr = dval;
    }

    public void setNumFilters(int val) {
        numFilters = val;
    }

    public void setOrderFilters(int val) {
        orderFilters = val;
    }

    public void loadCartTreeSet() throws IOException, Exception {
        cart.loadTreeSet(this);
    }

    public void loadGVModelSet() throws IOException {
        gv.loadGVModelSet(this);
    }

    public InputStream getStream(String path, String file) throws FileNotFoundException {
        InputStream is = new FileInputStream(path + file);
        return is;
    }

    public void initHMMData(Properties p) throws IOException, Exception {
        System.out.println("Reached new initHMMData");
        String bp = p.getProperty("base");
        this.rate = Integer.parseInt(p.getProperty("rate", "16000"));
        this.fperiod = Integer.parseInt(p.getProperty("fperiod", "80"));
        this.alpha = Double.parseDouble(p.getProperty("alpha", "0.55"));
        this.stage = Integer.parseInt(p.getProperty("stage", "0"));
        this.useLogGain = Boolean.parseBoolean(p.getProperty("useLogGain", "false"));
        this.beta = Double.parseDouble(p.getProperty("beta", "0.0"));

        treeDurStream = getStream(bp, "tree-dur.inf"); /* Tree DUR Ftd? */
        treeLf0Stream = getStream(bp, "tree-lf0.inf"); /* Tree LF0 */
        treeMgcStream = getStream(bp, "tree-mgc.inf"); /* Tree MCP */
        treeStrStream = getStream(bp, "tree-str.inf"); /* Tree STR Fts */
        // treeMagStream = p.getStream(prefix + ".Fta"); /* Tree MAG */?

        pdfDurStream = getStream(bp, "dur.pdf"); /* Model DUR Fmd */
        pdfLf0Stream = getStream(bp, "lf0.pdf"); /* Model LF0 */
        pdfMgcStream = getStream(bp, "mgc.pdf"); /* Model MCP */
        pdfStrStream = getStream(bp, "str.pdf"); /* Model STR */
        // pdfMagStream = getStream(bp, "?????.Fma"); /* Model MAG */
        /* use AcousticModeller, so prosody modification is enabled */
        this.useAcousticModels = Boolean.parseBoolean(p.getProperty("useAcousticModels", "false"));
        /* Use Mixed excitation */
        this.useMixExc = Boolean.parseBoolean(p.getProperty("useMixExc", "false"));
        /* Use Fourier magnitudes for pulse generation */
        this.useFourierMag = Boolean.parseBoolean(p.getProperty("useFourierMag", "false"));
        /* Use Global Variance in parameter generation */
        this.useGV = Boolean.parseBoolean(p.getProperty("useGV", "false"));
        if (useGV) {
            useContextDependentGV = Boolean.parseBoolean(p.getProperty("useContextDependentGV", "false"));
            String gvMethod = p.getProperty("gvMethod");
            if (gvMethod != null)
                setGvMethod(gvMethod);

            // Number of iteration for GV
            maxMgcGvIter = Integer.parseInt(p.getProperty("maxMgcGvIter", "100"));
            maxLf0GvIter = Integer.parseInt(p.getProperty("maxLf0GvIter", "100"));
            maxStrGvIter = Integer.parseInt(p.getProperty("maxStrGvIter", "100"));

            // weights for GV
            gvWeightMgc = Double.parseDouble(p.getProperty("gvWeightMgc", "1.0"));
            gvWeightLf0 = Double.parseDouble(p.getProperty("gvWeightLf0", "1.0"));
            gvWeightStr = Double.parseDouble(p.getProperty("gvWeightStr", "1.0"));

            // GV pdf files: mean and variance (diagonal covariance)
            pdfLf0GVStream = getStream(bp, "gv-lf0.pdf"); /* GV Model LF0 */
            pdfMgcGVStream = getStream(bp, "gv-mgc.pdf"); /* GV Model MCP */
            pdfStrGVStream = getStream(bp, "gv-str.pdf"); /* GV Model STR */
            // pdfMagGVStream = p.getStream(prefix + ".Fgva"); /* GV Model MAG
            // */
        }

        /* targetfeatures file, for testing */
//        String ff = p.getProperty("featuresFile", "b0487.fea");
//        InputStream featureStream = getStream(bp, ff);
//        feaDef = FeatureIO.read(featureStream, false);


        /* Configuration for mixed excitation */
        InputStream mixFiltersStream = getStream(bp, p.getProperty("excitationFilters", "mix_excitation_filters.txt"));
        if (mixFiltersStream != null) {
            numFilters = Integer.parseInt(p.getProperty("numExcitationFilters", "5"));
            System.out.println("Loading Mixed Excitation Filters File: " + numFilters);
            readMixedExcitationFilters(mixFiltersStream);
        }

        /* Load TreeSet in CARTs. */
        System.out.println("Loading Tree Set in CARTs:");
        loadCartTreeSet();

        /* Load GV ModelSet gv */
        System.out.println("Loading GV Model Set:");
        loadGVModelSet();

        System.out.println("InitHMMData complete");
    }

    // public void initHMMData(String voiceName, String bp, String configFile)
    // throws Exception {
    //
    // Properties props = new Properties();
    //
    // FileInputStream fis = new FileInputStream(bp + configFile);
    // props.load(fis);
    // fis.close();
    // Map<String, String> pr = new HashMap<String, String>();
    // pr.put("jar:", bp);
    // initHMMData(new PropertiesAccessor(props, false, pr), voiceName);
    // }
    //
    // public void initHMMData(String voiceName) throws IOException, Exception {
    // initHMMData(Config.getVoiceConfig(voiceName).getPropertiesAccessor(true),
    // voiceName);
    // }
    //
    //
    // public void initHMMDataForHMMModel(String voiceName) throws IOException,
    // Exception {
    // String prefix = "voice." + voiceName;
    // treeDurStream = p.getStream(prefix + ".Ftd");
    // pdfDurStream = p.getStream(prefix + ".Fmd");
    //
    // treeLf0Stream = p.getStream(prefix + ".Ftf");
    // pdfLf0Stream = p.getStream(prefix + ".Fmf");
    // useGV = p.getBoolean(prefix + ".useGV");
    // if (useGV) {
    // useContextDependentGV = p.getBoolean(prefix + ".useContextDependentGV",
    // useContextDependentGV);
    // if (p.getProperty(prefix + ".gvMethod") != null) {
    // String sval = p.getProperty(prefix + ".gvMethod");
    // setGvMethod(sval);
    // }
    // maxLf0GvIter = p.getInteger(prefix + ".maxLf0GvIter", maxLf0GvIter);
    // gvWeightLf0 = p.getDouble(prefix + ".gvWeightLf0", gvWeightLf0);
    //
    // pdfLf0GVStream = p.getStream(prefix + ".Fgvf");
    // maxLf0GvIter = p.getInteger(prefix + ".maxLf0GvIter", maxLf0GvIter);
    //
    // }
    //
    // InputStream feaStream = p.getStream(prefix + ".FeaFile");
    // feaDef = FeatureIO.read(feaStream, false);
    //
    // /*
    // * trickyPhones file, if any. If aliases for tricky phones were used
    // * during the training of HMMs then these aliases are in this file, if
    // * no aliases were used then the string is empty. This file will be used
    // * during the loading of HMM trees, so aliases of tricky phone are
    // * aplied back.
    // */
    // InputStream trickyPhonesStream = p.getStream(prefix +
    // ".trickyPhonesFile");
    // trickyPhones = new PhoneTranslator(trickyPhonesStream);
    //
    // /* Load TreeSet ts and ModelSet ms for current voice */
    // System.out.println("Loading Tree Set in CARTs:");
    // cart.loadTreeSet(this, feaDef, trickyPhones);
    //
    // System.out.println("Loading GV Model Set:");
    // gv.loadGVModelSet(this, feaDef);
    //
    // }

    public void readMixedExcitationFilters(InputStream mixFiltersStream) throws IOException {
        String line;
        // first read the taps and then divide the total amount equally among
        // the number of filters
        Vector<Double> taps = new Vector<Double>();
        /* get the filter coefficients */
        Scanner s = null;
        int i, j;
        try {
            s = new Scanner(new BufferedReader(new InputStreamReader(mixFiltersStream, "UTF-8")));
            s.useLocale(Locale.US);

            System.out.println("reading mixed excitation filters");
            while (s.hasNext("#")) { /* skip comment lines */
                line = s.nextLine();
                // System.out.println("comment: " + line );
            }
            while (s.hasNextDouble())
                taps.add(s.nextDouble());
        } finally {
            if (s != null) {
                s.close();
            }
        }

        orderFilters = (int) (taps.size() / numFilters);
        mixFilters = new double[numFilters][orderFilters];
        int k = 0;
        for (i = 0; i < numFilters; i++) {
            for (j = 0; j < orderFilters; j++) {
                mixFilters[i][j] = taps.get(k++);
                // System.out.println("h["+i+"]["+j+"]="+h[i][j]);
            }
        }
        System.out.println("initMixedExcitation: loaded filter taps");
        System.out.println("initMixedExcitation: numFilters = " + numFilters + "  orderFilters = " + orderFilters);

    } /* method readMixedExcitationFiltersFile() */

    /**
     * return the set of FeatureTypes that are available in this HMMData object
     */
    public Set<FeatureType> getFeatureSet() {
        Set<FeatureType> featureTypes = EnumSet.noneOf(FeatureType.class);
        if (getPdfDurStream() != null)
            featureTypes.add(FeatureType.DUR);
        if (getPdfLf0Stream() != null)
            featureTypes.add(FeatureType.LF0);
        if (getPdfStrStream() != null)
            featureTypes.add(FeatureType.STR);
        if (getPdfMagStream() != null)
            featureTypes.add(FeatureType.MAG);
        if (getPdfMgcStream() != null)
            featureTypes.add(FeatureType.MGC);
        return featureTypes;
    }

}
