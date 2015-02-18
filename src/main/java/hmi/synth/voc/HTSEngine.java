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

public class HTSEngine {

    private String realisedDurations; // HMM realised duration to be save in a
                                      // file
    private boolean phoneAlignmentForDurations;
    private boolean stateAlignmentForDurations = false;
    private Vector<PhonemeDuration> alignDur = null; // list of external
                                                     // duration per phone for
                                                     // alignment
                                                     // this are durations
                                                     // loaded from a external
                                                     // file
    private double newStateDurationFactor = 0.5; // this is a factor that
                                                 // extends or shrinks the
                                                 // duration of a state
                                                 // it can be used to try to
                                                 // syncronise the duration
                                                 // specified in a external
                                                 // file
                                                 // and the number of frames in
                                                 // a external lf0 file

    public String getRealisedDurations() {
        return realisedDurations;
    }

    public boolean getPhonemeAlignmentForDurations() {
        return phoneAlignmentForDurations;
    }

    public boolean getStateAlignmentForDurations() {
        return stateAlignmentForDurations;
    }

    public Vector<PhonemeDuration> getAlignDurations() {
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

    public void setAlignDurations(Vector<PhonemeDuration> val) {
        alignDur = val;
    }

    public void setNewStateDurationFactor(double dval) {
        newStateDurationFactor = dval;
    }

    public HTSEngine() {
        super();
        phoneAlignmentForDurations = false;
        stateAlignmentForDurations = false;
        alignDur = null;
    }

    public Audio process(SpeechMarkup d, List<Target> targetFeaturesList) throws Exception {

        Voice v = d.getVoice();
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice) v;

        /**
         * The utterance model, um, is a Vector (or linked list) of Model
         * objects. It will contain the list of models for current label file.
         */
        /* Process label file of context features and creates UttModel um */
        HTSUttModel um = processTargetList(targetFeaturesList, d.getSentences().get(0).getSegments(), hmmv.getHMMData());

        /* Process UttModel */
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        /*
         * Generate sequence of speech parameter vectors, generate parameters
         * out of sequence of pdf's
         */
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData());

        /*
         * set parameters for generation: f0Std, f0Mean and length, default
         * values 1.0, 0.0 and 0.0
         */
        /* These values are fixed in HMMVoice */

        /* Process generated parameters */
        HTSVocoder par2speech = new HTSVocoder();
        /*
         * Synthesize speech waveform, generate speech out of sequence of
         * parameters
         */
        AudioInputStream ais = par2speech.htsMLSAVocoder(pdf2par, hmmv.getHMMData());

        Audio output = new Audio(outputType(), d.getLocale());
        if (d.getAudioFileFormat() != null) {
            output.setAudioFileFormat(d.getAudioFileFormat());
            if (d.getAudio() != null) {
                // This (empty) AppendableSequenceAudioInputStream object allows
                // a
                // thread reading the audio data on the other "end" to get to
                // our data as we are producing it.
                assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
                output.setAudio(d.getAudio());
            }
        }
        output.appendAudio(ais);

        // set the actualDurations in tokensAndBoundaries
        if (tokensAndBoundaries != null)
            setRealisedProsody(tokensAndBoundaries, um);

        return output;

    }

    public static void setRealisedProsody(List<Segment> tokensAndBoundaries, HTSUttModel um) {
        int i, j, index;
        NodeList no1, no2;
        NamedNodeMap att;
        Scanner s = null;
        String line, str[];
        float totalDur = 0f; // total duration, in seconds
        double f0[];
        HMMModel m;

        int numModel = 0;

        for (Segment e : tokensAndBoundaries) {
            // System.out.println("TAG: " + e.getTagName());
            if (e.getTagName().equals(TOKEN)) {
                NodeIterator nIt = createNodeIterator(e, PHONE);
                Element phone;
                while ((phone = (Element) nIt.nextNode()) != null) {
                    String p = phone.getAttribute("p");
                    m = um.getUttModel(numModel++);

                    // CHECK THIS!!!!!!!

                    // System.out.println("realised p=" + p + "  phoneName=" +
                    // m.getPhoneName());
                    // int currentDur = m.getTotalDurMillisec();
                    totalDur += m.getTotalDurMillisec() * 0.001f;
                    // phone.setAttribute("d", String.valueOf(currentDur));
                    phone.setAttribute("d", m.getXmlDur());
                    phone.setAttribute("end", String.valueOf(totalDur));

                    // phone.setAttribute("f0", m.getUnit_f0ArrayStr());
                    phone.setAttribute("f0", m.getXmlF0());

                }
            } else if (e.getTagName().contentEquals(BOUNDARY)) {
                int breakindex = 0;
                try {
                    breakindex = Integer.parseInt(e.getAttribute("breakindex"));
                } catch (NumberFormatException nfe) {
                }
                if (e.hasAttribute("duration") || breakindex >= 3) {
                    m = um.getUttModel(numModel++);
                    if (m.getPhoneName().contentEquals("_")) {
                        int currentDur = m.getTotalDurMillisec();
                        // index = ph.indexOf("_");
                        totalDur += currentDur * 0.001f;
                        e.setAttribute("duration", String.valueOf(currentDur));
                    }
                }
            }
        }
    }

    public HTSUttModel processUttFromFile(String feaFile, HMMData htsData) throws Exception {

        List<Target> targetFeaturesList = getTargetsFromFile(feaFile, htsData);
        return processTargetList(targetFeaturesList, null, htsData);

    }

    public static List<Target> getTargetsFromFile(String LabFile, HMMData htsData) throws Exception {
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

    public List<Target> getTargetsFromText(String LabText, HMMData htsData) throws Exception {
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

    public static List<Target> getTargets(Scanner s, HMMData htsData) {
        int i;
        // Scanner s = null;
        String nextLine;
        FeatureDefinition feaDef = htsData.getFeatureDefinition();
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

    /***
     * Process feature vectors in target list to generate a list of models for
     * generation and realisation
     * 
     * @param targetFeaturesList
     *            : each target must contain the corresponding feature vector
     * @param segmentsAndBoundaries
     *            : if applying external prosody provide acoust params as a list
     *            of elements
     * @param um
     *            : as a result of this process a utterance model list is
     *            created for generation and then realisation
     * @param htsData
     *            : parameters and configuration of the voice
     * @throws Exception
     */
    protected HTSUttModel processTargetList(List<Target> targetFeaturesList, List<Segment> segmentsAndBoundaries,
            HMMData htsData) throws Exception {
        HTSUttModel um = new HTSUttModel();
        CartTreeSet cart = htsData.getCartTreeSet();
        realisedDurations = "#\n";
        int numLab = 0;
        double diffdurOld = 0.0;
        int alignDurSize = 0;
        final float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
        final float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
        boolean firstPh = true;
        float durVal = 0.0f;
        FeatureDefinition feaDef = htsData.getFeatureDefinition();

        int featureIndex = feaDef.getFeatureIndex("phone");
        if (htsData.getUseAcousticModels()) {
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
            HMMModel m = new HMMModel(cart.getNumStates());
            um.addUttModel(m);
            m.setPhoneName(fv.getFeatureAsString(featureIndex, feaDef));

            // Check if context-dependent gv (gv without sil)
            if (htsData.getUseContextDependentGV()) {
                if (m.getPhoneName().contentEquals("_"))
                    m.setGvSwitch(false);
            }
            // System.out.println("HTSEngine: phone=" + m.getPhoneName());

            double diffdurNew;

            // get the duration and f0 values from the acoustparams =
            // segmentsAndBoundaries
            if (phoneAlignmentForDurations && segmentsAndBoundaries != null) {
                Element e = segmentsAndBoundaries.get(i);
                // System.out.print("HTSEngine: phone=" + m.getPhoneName() +
                // "  TagName=" + e.getTagName());
                // get the durations of the Gaussians, because we need to know
                // how long each estate should be
                // knowing the duration of each state we can modified it so the
                // 5 states reflect the external duration
                // Here the duration for phones and sil (_) are calcualted
                diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, false, diffdurOld);

                if (e.getTagName().contentEquals("ph")) {
                    m.setXmlDur(e.getAttribute("d"));
                    durVal = Float.parseFloat(m.getXmlDur());
                    // System.out.println("  durVal=" + durVal +
                    // " totalDurGauss=" + (fperiodmillisec * m.getTotalDur()) +
                    // "(" +
                    // m.getTotalDur() + " frames)" );
                    // get proportion of this duration for each state;
                    // m.getTotalDur() contains total duration of the 5 states
                    // in
                    // frames
                    double durationsFraction = durVal / (fperiodmillisec * m.getTotalDur());
                    m.setTotalDur(0);
                    for (int k = 0; k < cart.getNumStates(); k++) {
                        // System.out.print("   state: " + k +
                        // " durFromGaussians=" + m.getDur(k));
                        int newStateDuration = (int) (durationsFraction * m.getDur(k) + newStateDurationFactor);
                        newStateDuration = Math.max(1, newStateDuration);
                        m.setDur(k, newStateDuration);
                        m.incrTotalDur(newStateDuration);
                        // System.out.println("   durNew=" + m.getDur(k));
                    }

                } else if (e.getTagName().contentEquals("boundary")) { // the
                                                                       // duration
                                                                       // for
                                                                       // boundaries
                                                                       // predicted
                                                                       // in the
                                                                       // AcousticModeller
                                                                       // is not
                                                                       // calculated
                                                                       // with
                                                                       // HMMs
                    durVal = 0;
                    if (!e.getAttribute("duration").isEmpty())
                        durVal = Float.parseFloat(e.getAttribute("duration"));

                    // TODO: here we need to differentiate a duration coming
                    // from outside and one fixed by the BoundaryModel
                    // theBoundaryModel fix always
                    // duration="400" for breakindex
                    // durations different from 400 milisec. are used here
                    // otherwise it is ignored and use the
                    // the duration calculated from the gaussians instead.
                    if (durVal != 400) {
                        // if duration comes from a specified duration in
                        // miliseconds, i use that
                        int durValFrames = Math.round(durVal / fperiodmillisec);
                        int totalDurGaussians = m.getTotalDur();
                        m.setTotalDur(durValFrames);
                        // System.out.println("  boundary attribute:duration=" +
                        // durVal + "  in frames=" + durValFrames);

                        // the specified duration has to be split among the five
                        // states
                        float durationsFraction = durVal / (fperiodmillisec * m.getTotalDur());
                        m.setTotalDur(0);
                        for (int k = 0; k < cart.getNumStates(); k++) {
                            // System.out.print("   state: " + k +
                            // " durFromGaussians=" + m.getDur(k));
                            int newStateDuration = Math.round(((float) m.getDur(k) / (float) totalDurGaussians)
                                    * durValFrames);
                            newStateDuration = Math.max(newStateDuration, 1);
                            m.setDur(k, newStateDuration);
                            m.setTotalDur(m.getTotalDur() + m.getDur(k));
                            // System.out.println("   durNew=" + m.getDur(k));
                        }

                    } else {
                        if (!e.getAttribute("breakindex").isEmpty()) {
                            durVal = Float.parseFloat(e.getAttribute("breakindex"));
                            // System.out.print("   boundary attribute:breakindex="
                            // + durVal);
                        }
                        durVal = (m.getTotalDur() * fperiodmillisec);
                    }
                    // System.out.println("  setXml(durVal)=" + durVal);
                    m.setXmlDur(Float.toString(durVal));
                }

                // set F0 values
                if (e.hasAttribute("f0")) {
                    m.setXmlF0(e.getAttribute("f0"));
                    // System.out.println("   f0=" + e.getAttribute("f0"));
                }

            } else { // Estimate state duration from state duration model
                     // (Gaussian)
                diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, false, diffdurOld);
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
            cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());

            /* Find pdf for Mgc, this function sets the pdf for each state. */
            cart.searchMgcInCartTree(m, fv, feaDef);

            /*
             * Find pdf for strengths, this function sets the pdf for each
             * state.
             */
            if (htsData.getTreeStrStream() != null)
                cart.searchStrInCartTree(m, fv, feaDef);

            /*
             * Find pdf for Fourier magnitudes, this function sets the pdf for
             * each state.
             */
            if (htsData.getTreeMagStream() != null)
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
            HMMModel m = um.getUttModel(i);
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
    } /* method processTargetList */

    /**
     * Stand alone testing using a TARGETFEATURES file as input.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        int j;

        HTSEngine hmm_tts = new HTSEngine();

        /*
         * htsData contains: Data in the configuration file, .pdf, tree-xxx.inf
         * file names and other parameters. After initHMMData it contains:
         * ModelSet: Contains the .pdf's (means and variances) for dur, lf0,
         * Mgc, str and mag these are all the HMMs trained for a particular
         * voice TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, Mgc, str and
         * mag these are all the trees trained for a particular voice.
         */
        HMMData htsData = new HMMData();

        /* stand alone with cmu-slt-hsmm voice */
        String BP = "/user/xxx/";
        String voiceDir = BP + "voice-cmu-slt-hsmm/src/main/resources/";
        String voiceName = "cmu-slt-hsmm"; /* voice name */
        String voiceConfig = "/voice/CmuSltHsmm/voice.config";
        String durFile = BP + "tmp/tmp.lab"; /*
                                              * to save realised durations in
                                              * .lab format
                                              */
        String parFile = BP + "tmp/tmp"; /*
                                          * to save generated parameters tmp.mfc
                                          * and tmp.f0
                                          */
        String outWavFile = BP + "tmp/tmp.wav"; /*
                                                 * to save generated audio file
                                                 */

        // The settings for using GV and MixExc can be changed in this way:
        htsData.initHMMData(voiceName, voiceDir, voiceConfig);

        htsData.setUseGV(true);
        htsData.setUseMixExc(true);

        // Important: the stand alone works without the acoustic modeler, so it
        // should be de-activated
        htsData.setUseAcousticModels(false);

        /**
         * The utterance model, um, is a Vector (or linked list) of Model
         * objects. It will contain the list of models for current label file.
         */
        HTSUttModel um;
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;

        /** Example of context features file */
        String feaFile = voiceDir + "/voice/CmuSltHsmm/cmu_us_arctic_slt_b0487.pfeats";

        try {
            /*
             * Process context features file and creates UttModel um, a linked
             * list of all the models in the utterance. For each model, it
             * searches in each tree, dur, cmp, etc, the pdf index that
             * corresponds to a triphone context feature and with that index
             * retrieves from the ModelSet the mean and variance for each state
             * of the HMM.
             */
            um = hmm_tts.processUttFromFile(feaFile, htsData);

            /* save realised durations in a lab file */
            FileWriter outputStream = new FileWriter(durFile);
            outputStream.write(hmm_tts.getRealisedDurations());
            outputStream.close();

            /*
             * Generate sequence of speech parameter vectors, generate
             * parameters out of sequence of pdf's
             */
            /*
             * the generated parameters will be saved in tmp.mfc and tmp.f0,
             * including header.
             */
            boolean debug = true; /*
                                   * so it save the generated parameters in
                                   * parFile
                                   */
            pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

            /*
             * Synthesize speech waveform, generate speech out of sequence of
             * parameters
             */
            ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

            System.out.println("Saving to file: " + outWavFile);
            System.out.println("Realised durations saved to file: " + durFile);
            File fileOut = new File(outWavFile);

            if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
            }

            System.out.println("Calling audioplayer:");
            AudioPlayer player = new AudioPlayer(fileOut);
            player.start();
            player.join();
            System.out.println("Audioplayer finished...");

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    } /* main method */

} /* class HTSEngine */
