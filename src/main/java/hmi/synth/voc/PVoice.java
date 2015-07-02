package hmi.synth.voc;

import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;
import hmi.sig.AudioPlayer;
import hmi.synth.target.Target;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class PVoice {

    private String realisedDurations;
    private boolean phoneAlignmentForDurations;
    private boolean stateAlignmentForDurations = false;
    private Vector<PhoneDuration> alignDur = null;
    private double newStateDurationFactor = 0.5;

    public String getRealisedDurations() {
        return realisedDurations;
    }

    public boolean getPhonemeAlignmentForDurations() {
        return phoneAlignmentForDurations;
    }

    public boolean getStateAlignmentForDurations() {
        return stateAlignmentForDurations;
    }

    public Vector<PhoneDuration> getAlignDurations() {
        return alignDur;
    }

    public double getNewStateDurationFactor() {
        return newStateDurationFactor;
    }

    public void setRealisedDurations(String str) {
        realisedDurations = str;
    }

    public void setStateAlignmentForDurations(boolean bval) {
        stateAlignmentForDurations = bval;
    }

    public void setPhonemeAlignmentForDurations(boolean bval) {
        phoneAlignmentForDurations = bval;
    }

    public void setAlignDurations(Vector<PhoneDuration> val) {
        alignDur = val;
    }

    public void setNewStateDurationFactor(double dval) {
        newStateDurationFactor = dval;
    }

    public PVoice() {
        super();
        phoneAlignmentForDurations = false;
        stateAlignmentForDurations = false;
        alignDur = null;
    }

//    public Audio process(SpeechMarkup d, PData pdata, List<Target> targetFeaturesList) throws Exception {
//
//        PUttModel um = processTargetList(targetFeaturesList, d.getSentences().get(0).getSegments(), pdata);
//
//        ParameterGenerator pdf2par = new ParameterGenerator();
//
//        pdf2par.htsMaximumLikelihoodParameterGeneration(um, pdata);
//
//        Vocoder par2speech = new Vocoder();
//
//        AudioInputStream ais = par2speech.htsMLSAVocoder(pdf2par, pdata);
//
//        // set the actualDurations in tokensAndBoundaries
//        if (d != null)
//            setRealisedProsody(d.getSentences().get(0).getSegments(), um);
//
//        return output;
//
//    }

    public static void setRealisedProsody(List<Segment> tokensAndBoundaries, PUttModel um) {
        // TODO
    }

    public PUttModel processUttFromFile(String feaFile, PData htsData) throws Exception {
        List<Target> targetFeaturesList = getTargetsFromFile(feaFile, htsData);
        return processTargetList(targetFeaturesList, null, htsData);
    }

    public static List<Target> getTargetsFromFile(String LabFile, PData htsData) throws Exception {
        List<Target> targets = null;
        Scanner s = null;
        try {
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            targets = getTargets(s, htsData);

        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
        } finally {
            if (s != null)
                s.close();
        }
        return targets;
    }

    public List<Target> getTargetsFromText(String LabText, PData htsData) throws Exception {
        List<Target> targets;
        Scanner s = null;
        try {
            s = new Scanner(LabText);
            targets = getTargets(s, htsData);
        } finally {
            if (s != null)
                s.close();
        }
        return targets;
    }

    public static List<Target> getTargets(Scanner s, PData pdata) {
        int i;
        // Scanner s = null;
        String nextLine;
        FeatureDefinition feaDef = pdata.getFeatureDefinition();
        List<Target> targets = new ArrayList<Target>();
        FeatureVector fv;
        Target t;
        while (s.hasNext()) {
            nextLine = s.nextLine();
            if (nextLine.trim().equals(""))
                break;
        }
        /* skip until byte values */
        int numLines = 0;
        while (s.hasNext()) {
            nextLine = s.nextLine();
            if (nextLine.trim().equals(""))
                break;
            numLines++;
        }
        /* get feature vectors from byte values */
        i = 0;
        while (s.hasNext()) {
            nextLine = s.nextLine();
            // System.out.println("STR: " + nextLine);
            fv = feaDef.toFeatureVector(0, nextLine);
            t = new Target(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef), null);
            t.setFeatureVector(fv);
            targets.add(t);
        }
        return targets;
    }

    protected PUttModel processTargetList(List<Target> targetFeaturesList, List<Segment> segmentsAndBoundaries,
            PData pdata) throws Exception {
        PUttModel um = new PUttModel();
        CARTSet cart = pdata.getCartTreeSet();
        realisedDurations = "#\n";
        int numLab = 0;
        double diffdurOld = 0.0;
        int alignDurSize = 0;
        final float fperiodmillisec = ((float) pdata.getFperiod() / (float) pdata.getRate()) * 1000;
        final float fperiodsec = ((float) pdata.getFperiod() / (float) pdata.getRate());
        boolean firstPh = true;
        float durVal = 0.0f;
        FeatureDefinition feaDef = pdata.getFeatureDefinition();

        int featureIndex = feaDef.getFeatureIndex("phone");
        if (pdata.getUseAcousticModels()) {
            phoneAlignmentForDurations = true;
            System.out.println("Using prosody from acoustparams.");
        } else {
            phoneAlignmentForDurations = false;
            System.out.println("Estimating state durations from (Gaussian) state duration model.");
        }

        // process feature vectors in targetFeatureList
        int i = 0;
        for (Target target : targetFeaturesList) {

            FeatureVector fv = target.getFeatureVector(); // feaDef.toFeatureVector(0,
                                                          // nextLine);
            PModel m = new PModel(cart.getNumStates());
            um.addUttModel(m);
            m.setPhoneName(fv.getFeatureAsString(featureIndex, feaDef));

            // Check if context-dependent gv (gv without sil)
            if (pdata.getUseContextDependentGV()) {
                if (m.getPhoneName().contentEquals("_"))
                    m.setGvSwitch(false);
            }
            // System.out.println("HTSEngine: phone=" + m.getPhoneName());

            double diffdurNew;
            // TODO convert to speech markup
            // get the duration and f0 values from the acoustparams =
            // segmentsAndBoundaries
            if (phoneAlignmentForDurations && segmentsAndBoundaries != null) {
                // Element e = segmentsAndBoundaries.get(i);
                // // System.out.print("HTSEngine: phone=" + m.getPhoneName() +
                // // "  TagName=" + e.getTagName());
                // // get the durations of the Gaussians, because we need to
                // know
                // // how long each estate should be
                // // knowing the duration of each state we can modified it so
                // the
                // // 5 states reflect the external duration
                // // Here the duration for phones and sil (_) are calcualted
                diffdurNew = cart.searchDurInCartTree(m, fv, pdata, firstPh,
                false, diffdurOld);
                //
                // if (e.getTagName().contentEquals("ph")) {
                // m.setXmlDur(e.getAttribute("d"));
                // durVal = Float.parseFloat(m.getXmlDur());
                // // System.out.println("  durVal=" + durVal +
                // // " totalDurGauss=" + (fperiodmillisec * m.getTotalDur()) +
                // // "(" +
                // // m.getTotalDur() + " frames)" );
                // // get proportion of this duration for each state;
                // // m.getTotalDur() contains total duration of the 5 states
                // // in
                // // frames
                // double durationsFraction = durVal / (fperiodmillisec *
                // m.getTotalDur());
                // m.setTotalDur(0);
                // for (int k = 0; k < cart.getNumStates(); k++) {
                // // System.out.print("   state: " + k +
                // // " durFromGaussians=" + m.getDur(k));
                // int newStateDuration = (int) (durationsFraction * m.getDur(k)
                // +
                // newStateDurationFactor);
                // newStateDuration = Math.max(1, newStateDuration);
                // m.setDur(k, newStateDuration);
                // m.incrTotalDur(newStateDuration);
                // // System.out.println("   durNew=" + m.getDur(k));
                // }
                //
                // } else if (e.getTagName().contentEquals("boundary")) {
                // durVal = 0;
                // if (!e.getAttribute("duration").isEmpty())
                // durVal = Float.parseFloat(e.getAttribute("duration"));
                //
                // // TODO: here we need to differentiate a duration coming
                // // from outside and one fixed by the BoundaryModel
                // // theBoundaryModel fix always
                // // duration="400" for breakindex
                // // durations different from 400 milisec. are used here
                // // otherwise it is ignored and use the
                // // the duration calculated from the gaussians instead.
                // if (durVal != 400) {
                // // if duration comes from a specified duration in
                // // miliseconds, i use that
                // int durValFrames = Math.round(durVal / fperiodmillisec);
                // int totalDurGaussians = m.getTotalDur();
                // m.setTotalDur(durValFrames);
                // // System.out.println("  boundary attribute:duration=" +
                // // durVal + "  in frames=" + durValFrames);
                //
                // // the specified duration has to be split among the five
                // // states
                // float durationsFraction = durVal / (fperiodmillisec *
                // m.getTotalDur());
                // m.setTotalDur(0);
                // for (int k = 0; k < cart.getNumStates(); k++) {
                // // System.out.print("   state: " + k +
                // // " durFromGaussians=" + m.getDur(k));
                // int newStateDuration = Math.round(((float) m.getDur(k) /
                // (float)
                // totalDurGaussians)
                // * durValFrames);
                // newStateDuration = Math.max(newStateDuration, 1);
                // m.setDur(k, newStateDuration);
                // m.setTotalDur(m.getTotalDur() + m.getDur(k));
                // // System.out.println("   durNew=" + m.getDur(k));
                // }
                //
                // } else {
                // if (!e.getAttribute("breakindex").isEmpty()) {
                // durVal = Float.parseFloat(e.getAttribute("breakindex"));
                // // System.out.print("   boundary attribute:breakindex="
                // // + durVal);
                // }
                // durVal = (m.getTotalDur() * fperiodmillisec);
                // }
                // // System.out.println("  setXml(durVal)=" + durVal);
                // m.setXmlDur(Float.toString(durVal));
                // }
                //
                // // set F0 values
                // if (e.hasAttribute("f0")) {
                // m.setXmlF0(e.getAttribute("f0"));
                // // System.out.println("   f0=" + e.getAttribute("f0"));
                // }

            } else { // Estimate state duration from state duration model
                // (Gaussian)
                diffdurNew = cart.searchDurInCartTree(m, fv, pdata, firstPh, false, diffdurOld);
            }

            um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
            // System.out.println("   model=" + m.getPhoneName() +
            // "   TotalDurFrames=" + m.getTotalDur() + "  TotalDurMilisec=" +
            // (fperiodmillisec * m.getTotalDur()) + "\n");

            // Set realised durations
            m.setTotalDurMillisec((int) (fperiodmillisec * m.getTotalDur()));

            double durSec = um.getTotalFrame() * fperiodsec;
            realisedDurations += Double.toString(durSec) + " " + numLab + " " + m.getPhoneName() + "\n";
            numLab++;

            diffdurOld = diffdurNew; // to calculate the duration of next
                                     // phoneme

            /*
             * Find pdf for LF0, this function sets the pdf for each state. here
             * it is also set whether the model is voiced or not
             */
            // if ( ! htsData.getUseUnitDurationContinuousFeature() )
            // Here according to the HMM models it is decided whether the states
            // of this model are voiced or unvoiced
            // even if f0 is taken from Xml here we need to set the
            // voived/unvoiced values per model and state
            cart.searchLf0InCartTree(m, fv, feaDef, pdata.getUV());

            /* Find pdf for Mgc, this function sets the pdf for each state. */
            cart.searchMgcInCartTree(m, fv, feaDef);

            /*
             * Find pdf for strengths, this function sets the pdf for each
             * state.
             */
            if (pdata.getTreeStrStream() != null)
                cart.searchStrInCartTree(m, fv, feaDef);

            /*
             * Find pdf for Fourier magnitudes, this function sets the pdf for
             * each state.
             */
            if (pdata.getTreeMagStream() != null)
                cart.searchMagInCartTree(m, fv, feaDef);

            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel() + 1);
            /* update number of states */
            um.setNumState(um.getNumState() + cart.getNumStates());
            i++;

            firstPh = false;
        }

        if (phoneAlignmentForDurations && alignDur != null)
            if (um.getNumUttModel() != alignDurSize)
                throw new Exception("The number of durations provided for phone alignment (" + alignDurSize
                        + ") is greater than the number of feature vectors (" + um.getNumUttModel() + ").");

        for (i = 0; i < um.getNumUttModel(); i++) {
            PModel m = um.getUttModel(i);
            for (int mstate = 0; mstate < cart.getNumStates(); mstate++)
                if (m.getVoiced(mstate))
                    for (int frame = 0; frame < m.getDur(mstate); frame++)
                        um.setLf0Frame(um.getLf0Frame() + 1);
            // System.out.println("Vector m[" + i + "]=" + m.getPhoneName() );
        }

        System.out.println("Number of models in sentence numModel=" + um.getNumModel()
                + "  Total number of states numState=" + um.getNumState());
        System.out.println("Total number of frames=" + um.getTotalFrame() + "  Number of voiced frames="
                + um.getLf0Frame());

        // System.out.println("REALISED DURATIONS:" + realisedDurations);

        return um;
    } 

}
