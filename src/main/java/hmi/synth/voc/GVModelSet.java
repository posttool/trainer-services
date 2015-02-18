package hmi.synth.voc;

import hmi.ml.feature.FeatureDefinition;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Set of Global Mean and (diagonal) Variance for log f0, mel-cepstrum, bandpass
 * voicing strengths and Fourier magnitudes (
 */
public class GVModelSet {

    private double gvmeanMgc[];
    private double gvcovInvMgc[];

    private double gvmeanLf0[];
    private double gvcovInvLf0[];

    private double gvmeanStr[];
    private double gvcovInvStr[];

    private double gvmeanMag[];
    private double gvcovInvMag[];

    public double[] getGVmeanMgc() {
        return gvmeanMgc;
    }

    public double[] getGVcovInvMgc() {
        return gvcovInvMgc;
    }

    public double[] getGVmeanLf0() {
        return gvmeanLf0;
    }

    public double[] getGVcovInvLf0() {
        return gvcovInvLf0;
    }

    public double[] getGVmeanStr() {
        return gvmeanStr;
    }

    public double[] getGVcovInvStr() {
        return gvcovInvStr;
    }

    public double[] getGVmeanMag() {
        return gvmeanMag;
    }

    public double[] getGVcovInvMag() {
        return gvcovInvMag;
    }

    public void loadGVModelSet(PData htsData, FeatureDefinition featureDef) throws IOException {

//        int numMSDFlag, numStream, vectorSize, numDurPdf;
//        double gvcov;
//        DataInputStream data_in;
        InputStream gvStream;

        /* Here global variance vectors are loaded from corresponding files */
//        int m, i, nmix;
        if (htsData.getUseGV()) {
            // GV for Mgc
            if ((gvStream = htsData.getPdfMgcGVStream()) != null)
                loadGvFromFile(gvStream, "mgc", htsData.getGvMethodGradient(), htsData.getGvWeightMgc());

            // GV for Lf0
            if ((gvStream = htsData.getPdfLf0GVStream()) != null)
                loadGvFromFile(gvStream, "lf0", htsData.getGvMethodGradient(), htsData.getGvWeightLf0());

            // GV for Str
            if ((gvStream = htsData.getPdfStrGVStream()) != null)
                loadGvFromFile(gvStream, "str", htsData.getGvMethodGradient(), htsData.getGvWeightStr());

            // GV for Mag
            if ((gvStream = htsData.getPdfMagGVStream()) != null)
                loadGvFromFile(gvStream, "mag", htsData.getGvMethodGradient(), htsData.getGvWeightMag());

            // gv-switch
            // if( (gvFile=htsData.getSwitchGVFile()) != null)
            // loadSwitchGvFromFile(gvFile, featureDef, trickyPhones);

        }

    }

    private void loadGvFromFile(InputStream gvStream, String par, boolean gradientMethod, double gvWeight)
            throws IOException {

        int numMSDFlag, numStream, vectorSize, numDurPdf;
        DataInputStream data_in;
        int m, i;

        data_in = new DataInputStream(new BufferedInputStream(gvStream));
        System.out.println("LoadGVModelSet reading model of type '" + par + "' with gvWeight = " + gvWeight);

        numMSDFlag = data_in.readInt();
        numStream = data_in.readInt();
        vectorSize = data_in.readInt();
        numDurPdf = data_in.readInt();

        if (par.contentEquals("mgc")) {
            gvmeanMgc = new double[vectorSize];
            gvcovInvMgc = new double[vectorSize];
            readBinaryFile(data_in, gvmeanMgc, gvcovInvMgc, vectorSize, gradientMethod, gvWeight);
        } else if (par.contentEquals("lf0")) {
            gvmeanLf0 = new double[vectorSize];
            gvcovInvLf0 = new double[vectorSize];
            readBinaryFile(data_in, gvmeanLf0, gvcovInvLf0, vectorSize, gradientMethod, gvWeight);
        } else if (par.contentEquals("str")) {
            gvmeanStr = new double[vectorSize];
            gvcovInvStr = new double[vectorSize];
            readBinaryFile(data_in, gvmeanStr, gvcovInvStr, vectorSize, gradientMethod, gvWeight);
        } else if (par.contentEquals("mag")) {
            gvmeanMag = new double[vectorSize];
            gvcovInvMag = new double[vectorSize];
            readBinaryFile(data_in, gvmeanMag, gvcovInvMag, vectorSize, gradientMethod, gvWeight);
        }
        data_in.close();
    }

    private void readBinaryFile(DataInputStream data_in, double mean[], double ivar[], int vectorSize,
            boolean gradientMethod, double gvWeight) throws IOException {
        int i;
        double var;
        if (gradientMethod) {
            for (i = 0; i < vectorSize; i++) {
                mean[i] = data_in.readFloat() * gvWeight;
                var = data_in.readFloat();
                assert var > 0.0;
                ivar[i] = 1.0 / var;
            }
        } else {
            for (i = 0; i < vectorSize; i++) {
                mean[i] = data_in.readFloat() * gvWeight;
                ivar[i] = data_in.readFloat();
            }
        }
    }

    public void loadSwitchGvFromFile(String gvFile, FeatureDefinition featDef, PhoneTranslator trickyPhones)
            throws Exception {

        // featDef = featDefinition;
        // phTrans = phoneTranslator;
        PhoneTranslator phTrans = trickyPhones;

        int state, feaIndex;
        BufferedReader s = null;
        String line, buf, aux;
        StringTokenizer sline;
        // phTrans = phTranslator;

        assert featDef != null : "Feature Definition was not set";

        try {
            /* read lines of tree-*.inf fileName */
            s = new BufferedReader(new InputStreamReader(new FileInputStream(gvFile)));
            System.out.println("load: reading " + gvFile);

            // skip questions section
            while ((line = s.readLine()) != null) {
                if (line.indexOf("QS") < 0)
                    break; /* a new state is indicated by {*}[2], {*}[3], ... */
            }

            while ((line = s.readLine()) != null) {
                if (line.indexOf("{*}") >= 0) { /*
                                                 * this is the indicator of a
                                                 * new state-tree
                                                 */
                    aux = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                    state = Integer.parseInt(aux);

                    sline = new StringTokenizer(aux);

                    /* 1: gets index node and looks for the node whose idx = buf */
                    buf = sline.nextToken();

                    /* 2: gets question name and question name val */
                    buf = sline.nextToken();
                    String[] fea_val = buf.split("="); /*
                                                        * splits
                                                        * featureName=featureValue
                                                        */
                    feaIndex = featDef.getFeatureIndex(fea_val[0]);

                    /* Replace back punctuation values */
                    /*
                     * what about tricky phones, if using halfphones it would
                     * not be necessary
                     */
                    if (fea_val[0].contentEquals("sentence_punc") || fea_val[0].contentEquals("prev_punctuation")
                            || fea_val[0].contentEquals("next_punctuation")) {
                        // System.out.print("CART replace punc: " + fea_val[0] +
                        // " = " + fea_val[1]);
                        fea_val[1] = phTrans.replaceBackPunc(fea_val[1]);
                        // System.out.println(" --> " + fea_val[0] + " = " +
                        // fea_val[1]);
                    } else if (fea_val[0].contains("tobi_")) {
                        // System.out.print("CART replace tobi: " + fea_val[0] +
                        // " = " + fea_val[1]);
                        fea_val[1] = phTrans.replaceBackToBI(fea_val[1]);
                        // System.out.println(" --> " + fea_val[0] + " = " +
                        // fea_val[1]);
                    } else if (fea_val[0].contains("phone")) {
                        // System.out.print("CART replace phone: " + fea_val[0]
                        // + " = " + fea_val[1]);
                        fea_val[1] = phTrans.replaceBackTrickyPhones(fea_val[1]);
                        // System.out.println(" --> " + fea_val[0] + " = " +
                        // fea_val[1]);
                    }

                    // add featureName and featureValue to the switch off gv
                    // phones

                }
            } /* while */
            if (s != null)
                s.close();

        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException: " + e.getMessage());
            throw new FileNotFoundException("LoadTreeSet: " + e.getMessage());
        }

    }
}
