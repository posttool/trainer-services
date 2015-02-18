package hmi.synth.voc;

import hmi.synth.voc.PData.FeatureType;

import java.util.Arrays;

public class PModel {

    /* the name of the phone corresponding to this model ph(0) */
    private String phoneName;

    private double durError;
    /* duration for each state */
    private int dur[];

    /* total duration in frames */
    private int totalDur;

    /* total duration of this model in milliseconds */
    private int totalDurMillisec;

    /* mean vector of log f0 pdfs for each state */
    private double lf0Mean[][];

    /* variance (diag) elements of log f0 for each state */
    private double lf0Variance[][];
    /* mean vector of mel-cepstrum pdfs for each state */
    private double mcepMean[][];

    /* variance (diag) elements of mel-cepstrum for each state */
    private double mcepVariance[][];

    /* mean vector of strengths pdfs for each state */
    private double strMean[][];

    /* variance (diag) elements of strengths for each state */
    private double strVariance[][];

    /* mean vector of fourier magnitude pdfs for each state */
    private double magMean[][];

    /* variance (diag) elements of fourier magnitudes for each state */
    private double magVariance[][];

    /* voiced/unvoiced decision for each state */
    private boolean voiced[];

    /* duration in x input acoustparams, format d="val" in millisec. */
    private String xmlDur;

    /*
     * F0 values in x input acoustparams, format f0="(1,val1)...(100,val2)"
     * (%pos in total duration, f0 Hz)
     */
    private String xmlF0;

    private boolean gvSwitch;

    public PModel(int nstate) {
        totalDur = 0;
        dur = new int[nstate];
        lf0Mean = new double[nstate][];
        lf0Variance = new double[nstate][];
        voiced = new boolean[nstate];

        mcepMean = new double[nstate][];
        mcepVariance = new double[nstate][];

        strMean = new double[nstate][];
        strVariance = new double[nstate][];

        magMean = new double[nstate][];
        magVariance = new double[nstate][];

        xmlDur = null;
        xmlF0 = null;

        gvSwitch = true;
    }

    public void setPhoneName(String var) {
        phoneName = var;
    }

    public String getPhoneName() {
        return phoneName;
    }

    public void setDur(int i, int val) {
        dur[i] = val;
    }

    public int getDur(int i) {
        return dur[i];
    }

    public void setDurError(double e) {
        durError = e;
    }

    public double getDurError() {
        return durError;
    }

    public void setTotalDur(int val) {
        totalDur = val;
    }

    public int getTotalDur() {
        return totalDur;
    }

    public void incrTotalDur(int val) {
        totalDur += val;
    }

    public void setTotalDurMillisec(int val) {
        totalDurMillisec = val;
    }

    public int getTotalDurMillisec() {
        return totalDurMillisec;
    }

    public void setLf0Mean(int i, int j, double val) {
        lf0Mean[i][j] = val;
    }

    public double getLf0Mean(int i, int j) {
        return lf0Mean[i][j];
    }

    public void setLf0Variance(int i, int j, double val) {
        lf0Variance[i][j] = val;
    }

    public double getLf0Variance(int i, int j) {
        return lf0Variance[i][j];
    }

    public void setLf0Mean(int i, double val[]) {
        lf0Mean[i] = val;
    }

    public void setLf0Variance(int i, double val[]) {
        lf0Variance[i] = val;
    }

    public void setMcepMean(int i, int j, double val) {
        mcepMean[i][j] = val;
    }

    public double getMcepMean(int i, int j) {
        return mcepMean[i][j];
    }

    public void setMcepVariance(int i, int j, double val) {
        mcepVariance[i][j] = val;
    }

    public double getMcepVariance(int i, int j) {
        return mcepVariance[i][j];
    }

    public void setMcepMean(int i, double val[]) {
        mcepMean[i] = val;
    }

    public void setMcepVariance(int i, double val[]) {
        mcepVariance[i] = val;
    }

    public double[] getMean(FeatureType type, int i) {
        switch (type) {
        case MGC:
            return Arrays.copyOf(mcepMean[i], mcepMean[i].length);
        case STR:
            return Arrays.copyOf(strMean[i], strMean[i].length);
        case MAG:
            return Arrays.copyOf(magMean[i], magMean[i].length);
        case LF0:
            return Arrays.copyOf(lf0Mean[i], lf0Mean[i].length);
        default:
            throw new RuntimeException("You must not ask me about DUR");
        }
    }

    public double[] getVariance(FeatureType type, int i) {
        switch (type) {
        case MGC:
            return Arrays.copyOf(mcepVariance[i], mcepVariance[i].length);
        case STR:
            return Arrays.copyOf(strVariance[i], strVariance[i].length);
        case MAG:
            return Arrays.copyOf(magVariance[i], magVariance[i].length);
        case LF0:
            return Arrays.copyOf(lf0Variance[i], lf0Variance[i].length);
        default:
            throw new RuntimeException("You must not ask me about DUR");
        }
    }

    public void printMcepMean() {
        printVectors(mcepMean, mcepVariance);
    }

    public void printLf0Mean() {
        printVectors(lf0Mean, lf0Variance);
    }

    public void printVectors(double m[][], double v[][]) {
        for (int i = 0; i < v.length; i++) {
            System.out.print("  mean[" + i + "]: ");
            for (int j = 0; j < m[i].length; j++)
                System.out.format("%.6f ", m[i][j]);
            System.out.print("\n  vari[" + i + "]: ");
            for (int j = 0; j < v[i].length; j++)
                System.out.format("%.6f ", v[i][j]);
            System.out.println();
        }
    }

    public void printDuration(int numStates) {
        System.out.print("phoneName: " + phoneName + "\t");
        for (int i = 0; i < numStates; i++)
            System.out.print("dur[" + i + "]=" + dur[i] + " ");
        System.out.println("  totalDur=" + totalDur + "  totalDurMillisec=" + totalDurMillisec);
    }

    public void setStrMean(int i, int j, double val) {
        strMean[i][j] = val;
    }

    public double getStrMean(int i, int j) {
        return strMean[i][j];
    }

    public void setStrVariance(int i, int j, double val) {
        strVariance[i][j] = val;
    }

    public double getStrVariance(int i, int j) {
        return strVariance[i][j];
    }

    public void setStrMean(int i, double val[]) {
        strMean[i] = val;
    }

    public void setStrVariance(int i, double val[]) {
        strVariance[i] = val;
    }

    public void setMagMean(int i, int j, double val) {
        magMean[i][j] = val;
    }

    public double getMagMean(int i, int j) {
        return magMean[i][j];
    }

    public void setMagVariance(int i, int j, double val) {
        magVariance[i][j] = val;
    }

    public double getMagVariance(int i, int j) {
        return magVariance[i][j];
    }

    public void setMagMean(int i, double val[]) {
        magMean[i] = val;
    }

    public void setMagVariance(int i, double val[]) {
        magVariance[i] = val;
    }

    public void setVoiced(int i, boolean val) {
        voiced[i] = val;
    }

    public boolean getVoiced(int i) {
        return voiced[i];
    }

    public int getNumVoiced() {
        int numVoiced = 0;
        for (int i = 0; i < voiced.length; i++) {
            if (getVoiced(i))
                numVoiced += getDur(i);
        }
        return numVoiced;
    }

    public void setXmlDur(String str) {
        xmlDur = str;
    }

    public String getXmlDur() {
        return xmlDur;
    }

    public void setXmlF0(String str) {
        xmlF0 = str;
    }

    public String getXmlF0() {
        return xmlF0;
    }

    public void setGvSwitch(boolean bv) {
        gvSwitch = bv;
    }

    public boolean getGvSwitch() {
        return gvSwitch;
    }

    @Override
    public String toString() {
        return getPhoneName();
    }

}
