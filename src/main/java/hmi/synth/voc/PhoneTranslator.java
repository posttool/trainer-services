package hmi.synth.voc;

import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

public class PhoneTranslator {

    private String contextFeatureFile, trickyPhonesFile;
    private int iPhoneme, iPrevPhoneme, iPrevPrevPhoneme, iNextPhoneme, iNextNextPhoneme;
    private Map<String, String> trickyPhones = new HashMap<String, String>();
    private Map<String, String> actualPhones = new HashMap<String, String>();

    // When creating a phoneTranslator object a trickyPhonesFile can be provided
    // so the phone aliases are loaded.
    public PhoneTranslator(InputStream trickyPhonesStream) throws IOException {
        if (trickyPhonesStream != null) {
            loadTrickyPhones(trickyPhonesStream);
        }
    }

    public void setContextFeatureFile(String str) {
        contextFeatureFile = str;
    }

    /**
     * Convert the feature vector into a context model name to be used by
     * HTS/HTK.
     * 
     * @param def
     *            a feature definition
     * @param featureVector
     *            a feature vector which must be consistent with the Feature
     *            definition
     * @param featureList
     *            a list of features to use in constructing the context model
     *            name. If missing, all features in the feature definition are
     *            used.
     * @return the string representation of one context name. NOTE: is this
     *         function used somewhere? CHECK!
     */
    public String features2context(FeatureDefinition def, FeatureVector featureVector, Vector<String> featureList) {

        int feaAsInt;
        String phone, prev_phone, prev_prev_phone, next_phone, next_next_phone;

        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }

        feaAsInt = featureVector.getFeatureAsInt(iPhoneme);
        phone = replaceTrickyPhones(def.getFeatureValueAsString(iPhoneme, feaAsInt));

        feaAsInt = featureVector.getFeatureAsInt(iPrevPhoneme);
        if (feaAsInt > 0)
            prev_phone = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPhoneme, feaAsInt));
        else
            prev_phone = phone;
        // System.out.println("iPrevPhoneme=" + iPrevPhoneme + "  val=" +
        // feaAsInt);

        feaAsInt = featureVector.getFeatureAsInt(iPrevPrevPhoneme);
        if (feaAsInt > 0)
            prev_prev_phone = replaceTrickyPhones(def.getFeatureValueAsString(iPrevPrevPhoneme, feaAsInt));
        else
            prev_prev_phone = prev_phone;
        // System.out.println("iPrevPrevPhoneme=" + iPrevPrevPhoneme + "  val="
        // + feaAsInt);

        feaAsInt = featureVector.getFeatureAsInt(iNextPhoneme);
        if (feaAsInt > 0)
            next_phone = replaceTrickyPhones(def.getFeatureValueAsString(iNextPhoneme, feaAsInt));
        else
            next_phone = phone;
        // System.out.println("iNextPhoneme=" + iNextPhoneme + "  val=" +
        // feaAsInt);

        feaAsInt = featureVector.getFeatureAsInt(iNextNextPhoneme);
        if (feaAsInt > 0)
            next_next_phone = replaceTrickyPhones(def.getFeatureValueAsString(iNextNextPhoneme, feaAsInt));
        else
            next_next_phone = next_phone;
        // System.out.println("iNextNextPhoneme=" + iNextNextPhoneme + "  val="
        // + feaAsInt + "\n");

        StringBuilder contextName = new StringBuilder();
        contextName.append("prev_prev_phone=" + prev_prev_phone);
        contextName.append("|prev_phone=" + prev_phone);
        contextName.append("|phone=" + phone);
        contextName.append("|next_phone=" + next_phone);
        contextName.append("|next_next_phone=" + next_next_phone);
        contextName.append("||");
        /* append the other context features included in the featureList */
        for (String f : featureList) {
            if (!def.hasFeature(f)) {
                throw new IllegalArgumentException("Feature '" + f
                        + "' is not known in the feature definition. Valid features are: " + def.getFeatureNames());
            }
            // String shortF = shortenPfeat(f);
            // contextName.append(shortF);
            contextName.append(f);
            contextName.append("=");
            String value = def.getFeatureValueAsString(f, featureVector);
            if (f.contains("sentence_punc") || f.contains("punctuation"))
                value = replacePunc(value);
            else if (f.contains("tobi"))
                value = replaceToBI(value);
            contextName.append(value);
            contextName.append("|");
        }

        return contextName.toString();
    } /* method features2context */

    /**
     * Convert the feature vector into a context model name to be used by
     * HTS/HTK.
     * 
     * @param def
     *            a feature definition
     * @param featureVector
     *            a feature vector which must be consistent with the Feature
     *            definition
     * @param featureList
     *            a list of features to use in constructing the context model
     *            name. If missing, all features in the feature definition are
     *            used.
     * @return the string representation of one context name.
     */
    public String features2LongContext(FeatureDefinition def, FeatureVector featureVector, Vector<String> featureList) {
        if (featureList == null) {
            featureList = new Vector<String>(Arrays.asList(def.getFeatureNames().split("\\s+")));
        }
        StringBuilder contextName = new StringBuilder();
        contextName.append("|");
        for (String f : featureList) {
            if (!def.hasFeature(f)) {
                throw new IllegalArgumentException("Feature '" + f
                        + "' is not known in the feature definition. Valid features are: " + def.getFeatureNames());
            }
            contextName.append(f);
            contextName.append("=");
            String value = def.getFeatureValueAsString(f, featureVector);
            if (f.endsWith("phone"))
                value = replaceTrickyPhones(value);
            else if (f.contains("sentence_punc") || f.contains("punctuation"))
                value = replacePunc(value);
            else if (f.contains("tobi"))
                value = replaceToBI(value);
            contextName.append(value);
            contextName.append("|");
        }

        return contextName.toString();
    } /* method features2context */

    private void loadTrickyPhones(InputStream trickyStream) throws IOException {

        Scanner aliasList = null;
        aliasList = new Scanner(new BufferedReader(new InputStreamReader(trickyStream, "UTF-8")));
        String line;
        System.out.println("loading tricky phones");
        while (aliasList.hasNext()) {
            line = aliasList.nextLine();
            String[] ph = line.split(" ");

            trickyPhones.put(ph[0], ph[1]);
            actualPhones.put(ph[1], ph[0]);
            System.out.println("  " + ph[0] + " -->  " + ph[1]);

        }
        if (aliasList != null) {
            aliasList.close();
        }
    }

    /**
     * Translation table for labels which are incompatible with HTK or shell
     * filenames See common_routines.pl in HTS training.
     * 
     * @param lab
     * @return String
     */
    public String replaceTrickyPhones(String lab) {

        String s = lab;

        if (trickyPhones.containsKey(lab)) {
            s = trickyPhones.get(lab);
        }
        return s;
    }

    /**
     * Translation table for labels which are incompatible with HTK or shell
     * filenames See common_routines.pl in HTS training. In this function the
     * phones as used internally in HTSEngine are changed back to the allophone
     * set, this function is necessary when correcting the actual durations of
     * AcousticPhonemes.
     * 
     * @param lab
     * @return String
     */
    public String replaceBackTrickyPhones(String lab) {

        String s = lab;

        if (actualPhones.containsKey(lab)) {
            s = actualPhones.get(lab);
        }
        return s;
    }

    /**
     * Shorten the key name (to make the full context names shorter) See
     * common_routines.pl in HTS training. not needed CHECK
     */
    public String shortenPfeat(String fea) {

        // First time: need to do the shortening:
        String s = fea;
        // s = s.replace("^pos$/POS/g; /* ??? */
        // s = fea.replace("", "");
        s = s.replace("phone", "phn");
        s = s.replace("prev", "p");
        s = s.replace("next", "n");
        s = s.replace("sentence", "snt");
        s = s.replace("phrase", "phr");
        s = s.replace("word", "wrd");
        s = s.replace("from_", "");
        s = s.replace("to_", "");
        s = s.replace("in_", "");
        s = s.replace("is_", "");
        s = s.replace("break", "brk");
        s = s.replace("start", "stt");
        s = s.replace("accented", "acc");
        s = s.replace("accent", "acc");
        s = s.replace("stressed", "str");
        s = s.replace("punctuation", "punc");
        s = s.replace("frequency", "freq");
        s = s.replace("position", "pos");
        s = s.replace("halfphone_lr", "lr");

        // feat2shortFeat.put(fea, s);
        return s;
    }

    public String replacePunc(String lab) {
        String s = lab;

        if (lab.contentEquals("."))
            s = "pt";
        else if (lab.contentEquals(","))
            s = "cm";
        else if (lab.contentEquals("("))
            s = "op";
        else if (lab.contentEquals(")"))
            s = "cp";
        else if (lab.contentEquals("?"))
            s = "in";
        else if (lab.contentEquals("\""))
            s = "qt";

        return s;

    }

    public String replaceBackPunc(String lab) {
        String s = lab;

        if (lab.contentEquals("pt"))
            s = ".";
        else if (lab.contentEquals("cm"))
            s = ",";
        else if (lab.contentEquals("op"))
            s = "(";
        else if (lab.contentEquals("cp"))
            s = ")";
        else if (lab.contentEquals("in"))
            s = "?";
        else if (lab.contentEquals("qt"))
            s = "\"";

        return s;

    }

    public String replaceToBI(String lab) {
        String s = lab;

        if (lab.contains("*"))
            s = s.replace("*", "st");

        if (lab.contains("%"))
            s = s.replace("%", "pc");

        if (lab.contains("^"))
            s = s.replace("^", "ht");

        return s;

    }

    public String replaceBackToBI(String lab) {
        String s = lab;

        if (lab.contains("st"))
            s = s.replace("st", "*");

        if (lab.contains("pc"))
            s = s.replace("pc", "%");

        if (lab.contains("ht"))
            s = s.replace("ht", "^");

        return s;

    }

    public static void main(String[] args) throws Exception {

        PhoneTranslator phTrans;
        String oriLab, alias, ori;
        phTrans = new PhoneTranslator(new FileInputStream("/HMM-voices/turkish/trickyPhones.txt"));

        oriLab = "@'";
        alias = phTrans.replaceTrickyPhones(oriLab);
        ori = phTrans.replaceBackTrickyPhones(alias);
        System.out.println("oriLab=" + oriLab + "  alias=" + alias + "  ori=" + ori);

        oriLab = "e~";
        alias = phTrans.replaceTrickyPhones(oriLab);
        ori = phTrans.replaceBackTrickyPhones(alias);
        System.out.println("oriLab=" + oriLab + "  alias=" + alias + "  ori=" + ori);

    }

}
