package hmi.synth.voc.test;

import hmi.sig.AudioPlayer;
import hmi.sig.Mfccs;
import hmi.synth.voc.PData;
import hmi.synth.voc.PVoice;
import hmi.synth.voc.PStream;
import hmi.synth.voc.ParameterGenerator;
import hmi.synth.voc.PUttModel;
import hmi.synth.voc.Vocoder;
import hmi.synth.voc.PhoneDuration;
import hmi.synth.voc.PData.FeatureType;
import hmi.util.LDataInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class Test1 {
    static String BP = "/Users/posttool/Documents";

    public static Properties getVoiceProps0() {
        Properties p = new Properties();
        p.setProperty("base", BP + "/github/marytts/voice-cmu-slt-hsmm/src/main/resources/marytts/voice/CmuSltHsmm/");
        p.setProperty("gender", "male");
        p.setProperty("rate", "16000");
        p.setProperty("alpha", "0.1");
        p.setProperty("beta", "0.1");
        p.setProperty("logGain", "true");
        p.setProperty("useGV", "true");
        p.setProperty("maxMgcGvIter", "200");
        p.setProperty("maxLf0GvIter", "200");
        p.setProperty("featuresFile", "cmu_us_arctic_slt_b0487.pfeats");
        return p;
    }
    public static Properties getVoiceProps1() {
        Properties p = new Properties();
        p.setProperty("base", BP + "/github/hmi-www/app/build/data/dv-2-voc/mary/voice-my_voice-hsmm/src/main/resources/marytts/voice/My_voiceHsmm/");
        p.setProperty("gender", "male");
        p.setProperty("rate", "16000");
        p.setProperty("alpha", "0.3");
        p.setProperty("beta", "0.1");
        p.setProperty("logGain", "true");
        p.setProperty("useGV", "true");
        p.setProperty("maxMgcGvIter", "200");
        p.setProperty("maxLf0GvIter", "200");
        p.setProperty("featuresFile", "features_example.pfeats");
        p.setProperty("excitationFilters", "mix_excitation_5filters_99taps_16Kz.txt");
        return p;
    }

    public void synthesisWithContinuousFeatureProcessors() throws Exception {

        PVoice hmm_tts = new PVoice();
        PData htsData = new PData();

        /*
         * For initialize provide the name of the hmm voice and the name of its
         * configuration file,
         */
        String outWavFile = BP + "/tmp.wav";

        htsData.initHMMData(getVoiceProps1());

        // Set these variables so the htsEngine use the
        // ContinuousFeatureProcessors features
        htsData.setUseAcousticModels(true);

        // The settings for using GV and MixExc can
        // besynthesisWithExternalProsodySpecificationFiles changed in this way:
        htsData.setUseGV(true);
        htsData.setUseMixExc(true);
        htsData.setUseFourierMag(true); // if the voice was trained with Fourier
                                        // magnitudes

        /**
         * The utterance model, um, is a Vector (or linked list) of Model
         * objects. It will contain the list of models for current label file.
         */
        PUttModel um = new PUttModel();
        ParameterGenerator pdf2par = new ParameterGenerator();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;

        try {
            String feaFile = BP + "/github/hmi-www/app/build/data/dv-2-voc/phonefeatures/X_0002.pfeats";

            um = hmm_tts.processUttFromFile(feaFile, htsData);

            pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

            ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

            System.out.println("saving to file: " + outWavFile);
            File fileOut = new File(outWavFile);

            if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
            }

            System.out.println("Calling audioplayer:");
            AudioPlayer player = new AudioPlayer(fileOut);
            player.start();
            player.join();
            System.out.println("audioplayer finished...");

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

    public void synthesisWithProsodySpecificationInExternalFiles() throws Exception {

        String feaFile = "voices/cmu-slt-hsmm/cmu_us_arctic_slt_a0001.pfeats";

        PVoice hmm_tts = new PVoice();
        PData htsData = new PData();

        /*
         * For initialise provide the name of the hmm voice and the name of its
         * configuration file,
         */
        String BP = "//"; /* base directory. */

        String outWavFile = BP + "tmp/tmp.wav"; /* to save generated audio file */

        htsData.initHMMData(getVoiceProps1());

        // The settings for using GV and MixExc can be changed in this way:
        htsData.setUseGV(true);
        htsData.setUseMixExc(true);
        htsData.setUseFourierMag(true); // if the voice was trained with Fourier
                                        // magnitudes

        /**
         * The utterance model, um, is a Vector (or linked list) of Model
         * objects. It will contain the list of models for current label file.
         */
        PUttModel um = new PUttModel();
        ParameterGenerator pdf2par = new ParameterGenerator();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;

        // Specify external files:
        // external duration extracted with the voice import tools - EHMM
        String labFile = "/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.lab";
        // external duration obtained , there is a problem with this because it
        // does not have an initial sil
        // String labFile =
        // "/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.realised_durations";

        // external F0 contour obtained with SPTK during HMMs creation
        String lf0File = "/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.lf0";

        // Load and set external durations
        // ---htsData.setUseDurationFromExternalFile(true);
        float totalDuration;

        float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
        hmm_tts.setPhonemeAlignmentForDurations(true);
        Vector<PhoneDuration> durations = new Vector<PhoneDuration>();
        totalDuration = loadDurationsForAlignment(labFile, durations);
        // set the external durations
        hmm_tts.setAlignDurations(durations);
        int totalDurationFrames = (int) ((totalDuration / fperiodsec));
        // Depending on how well aligned the durations and the lfo file are
        // this factor can be used to extend or shrink the durations per phoneme
        // so it syncronize with the number of frames in the lf0 file
        hmm_tts.setNewStateDurationFactor(0.37);

        // set external logf0
        htsData.setUseAcousticModels(true);

        try {
            um = hmm_tts.processUttFromFile(feaFile, htsData);

            pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

            ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

            System.out.println("saving to file: " + outWavFile);
            File fileOut = new File(outWavFile);

            if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
            }

            System.out.println("Calling audioplayer:");
            AudioPlayer player = new AudioPlayer(fileOut);
            player.start();
            player.join();
            System.out.println("audioplayer finished...");

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    } /* main method */

    public float loadDurationsForAlignment(String fileName, Vector<PhoneDuration> alignDur) {

        Scanner s = null;
        String line;
        float totalDuration = 0;
        float previous = 0;
        float current = 0;
        try {
            s = new Scanner(new File(fileName));
            int i = 0;
            while (s.hasNext()) {
                line = s.nextLine();
                if (!line.startsWith("#") && !line.startsWith("format")) {
                    String val[] = line.split(" ");
                    current = Float.parseFloat(val[0]);
                    PhoneDuration var;
                    if (previous == 0)
                        alignDur.add(new PhoneDuration(val[2], current));
                    else
                        alignDur.add(new PhoneDuration(val[2], (current - previous)));

                    totalDuration += alignDur.get(i).getDuration();
                    System.out.println("phone = " + alignDur.get(i).getPhoneme() + " dur(" + i + ")="
                            + alignDur.get(i).getDuration() + " totalDuration=" + totalDuration);
                    i++;
                    previous = current;
                }
            }
            System.out.println();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // return alignDur;
        return totalDuration;
    }

    public void loadF0contour(String lf0File, int totalDurationFrames, ParameterGenerator pdf2par) throws Exception {
        PStream lf0Pst = null;
        boolean[] voiced = null;
        LDataInputStream lf0Data;

        int lf0Vsize = 3;
        int totalFrame = 0;
        int lf0VoicedFrame = 0;
        float fval;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
        /* First i need to know the size of the vectors */
        try {
            while (true) {
                fval = lf0Data.readFloat();
                totalFrame++;
                if (fval > 0)
                    lf0VoicedFrame++;
            }
        } catch (EOFException e) {
        }
        lf0Data.close();

        // Here we need to check that the total duration in frames is the same
        // as the number of frames
        // (NOTE: it can be a problem afterwards when the durations per phone
        // are aligned to the lenght of each state
        // in htsEngine._processUtt() )
        if (totalDurationFrames != totalFrame) {
            System.out.println("The total duration in frames " + totalDurationFrames
                    + " is not the same as the number of frames " + totalFrame + " in the lf0 file: " + lf0File);
        } else
            System.out.println("totalDurationFrames = " + totalDurationFrames + "  totalF0Frames = " + totalFrame);

        voiced = new boolean[totalFrame];
        lf0Pst = new PStream(lf0Vsize, totalFrame, PData.FeatureType.LF0, 0);

        /* load lf0 data */
        /* for lf0 i just need to load the voiced values */
        lf0VoicedFrame = 0;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
        for (int i = 0; i < totalFrame; i++) {
            fval = lf0Data.readFloat();
            if (fval < 0) {
                voiced[i] = false;
                System.out.println("frame: " + i + " = 0.0");
            } else {
                voiced[i] = true;
                lf0Pst.setPar(lf0VoicedFrame, 0, fval);
                lf0VoicedFrame++;
                System.out.format("frame: %d = %.2f\n", i, fval);
            }
        }
        lf0Data.close();

        // Set lf0 and voiced in pdf2par
        pdf2par.setlf0Pst(lf0Pst);
        pdf2par.setVoicedArray(voiced);

    }

    public void generateParameters() throws IOException, InterruptedException, Exception {

        int i, j;

        String contextFeaDir = "/quality-control-experiment/slt/phonefeatures/";
        String outputDir = "/quality-control-experiment/slt/hmmGenerated/";
        String filesList = "/quality-control-experiment/slt/phonefeatures-list.txt";

        PVoice hmm_tts = new PVoice();

        PData htsData = new PData();
        Properties p = new Properties();
        p.setProperty("base", BP + "/github/hmi-www/app/build/data/test-2/mary/voice-my_hmmmm_voice-hsmm"
                + "/src/main/resources/marytts/voice/My_hmmmm_voiceHsmm/");
        p.setProperty("gender", "female");
        p.setProperty("rate", "16000");
        p.setProperty("alpha", "0.42");
        p.setProperty("beta", "0.0");
        p.setProperty("logGain", "true");
        p.setProperty("useGV", "true");
        p.setProperty("maxMgcGvIter", "200");
        p.setProperty("maxLf0GvIter", "200");
        p.setProperty("featuresFile", "features_example.pfeats");
        p.setProperty("excitationFilters", "mix_excitation_5filters_99taps_16Kz.txt");
        htsData.initHMMData(p);
        float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
        float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());

        // Settings for using GV, mixed excitation
        htsData.setUseGV(true);
        htsData.setUseMixExc(true);

        /* generate files out of HMMs */
        String file, feaFile, parFile, durStateFile, durFile, mgcModifiedFile, outWavFile;
        try {
            Scanner filesScanner = new Scanner(new BufferedReader(new FileReader(filesList)));
            while (filesScanner.hasNext()) {

                file = filesScanner.nextLine();

                feaFile = contextFeaDir + file + ".pfeats";
                parFile = outputDir + file; /* generated parameters mfcc and f0 */
                durFile = outputDir + file + ".lab"; /* realized durations */
                durStateFile = outputDir + file + ".slab";
                outWavFile = outputDir + file + ".wav";

                PUttModel um = new PUttModel();
                ParameterGenerator pdf2par = new ParameterGenerator();
                Vocoder par2speech = new Vocoder();
                AudioInputStream ais;

                /*
                 * Process label file of context features and creates UttModel
                 * um.
                 */
                um = hmm_tts.processUttFromFile(feaFile, htsData);

                /* save realized durations in a lab file */
                FileWriter outputStream;
                outputStream = new FileWriter(durFile);
                outputStream.write(hmm_tts.getRealisedDurations());
                outputStream.close();

                /* save realized durations at state label in a slab file */
                float totalDur = 0;
                int numStates = htsData.getCartTreeSet().getNumStates();
                outputStream = new FileWriter(durStateFile);
                outputStream.write("#\n");
                for (i = 0; i < um.getNumModel(); i++) {
                    for (j = 0; j < numStates; j++) {
                        totalDur += (um.getUttModel(i).getDur(j) * fperiodsec);
                        if (j < (numStates - 1))
                            outputStream.write(totalDur + " 0 " + um.getUttModel(i).getPhoneName() + "\n");
                        else
                            outputStream.write(totalDur + " 1 " + um.getUttModel(i).getPhoneName() + "\n");
                    }
                }
                outputStream.close();

                /*
                 * Generate sequence of speech parameter vectors, generate
                 * parameters out of sequence of pdf's
                 */
                boolean debug = true; /*
                                       * with debug=true it saves the generated
                                       * parameters f0 and mfcc in parFile.f0
                                       * and parFile.mfcc
                                       */
                pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

                /*
                 * Synthesize speech waveform, generate speech out of sequence
                 * of parameter
                 */
                ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

                System.out.println("saving to file: " + outWavFile);
                File fileOut = new File(outWavFile);

                if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
                }
                /*
                 * // uncomment to listen the files
                 * System.out.println("Calling audioplayer:"); AudioPlayer
                 * player = new AudioPlayer(fileOut); player.start();
                 * player.join(); System.out.println("audioplayer finished...");
                 */

            } // while files in testFiles
            filesScanner.close();

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }

    } /* main method */

    public void getSptkMfcc() throws IOException, InterruptedException, Exception {

        String inFile = "/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.wav";
        String outFile = "/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.mfc";
        String tmpFile = "/quality-control-experiment/slt/tmp.mfc";
        String tmpRawFile = "/quality-control-experiment/slt/tmp.raw";
        String cmd;
        // SPTK parameters
        int fs = 16000;
        int frameLength = 400;
        int frameLengthOutput = 512;
        int framePeriod = 80;
        int mgcOrder = 24;
        int mgcDimension = 25;
        // header parameters
        double ws = (frameLength / fs); // window size in seconds
        double ss = (framePeriod / fs); // skip size in seconds

        // SOX and SPTK commands
        String sox = "/usr/bin/sox";
        String x2x = " /sw/SPTK-3.1/bin/x2x";
        String frame = " /sw/SPTK-3.1/bin/frame";
        String window = " /sw/SPTK-3.1/bin/window";
        String mcep = " /sw/SPTK-3.1/bin/mcep";
        String swab = "/sw/SPTK-3.1/bin/swab";

        // convert the wav file to raw file with sox
        cmd = sox + " " + inFile + " " + tmpRawFile;
        launchProc(cmd, "sox", inFile);

        System.out.println("Extracting MGC coefficients from " + inFile);

        cmd = x2x + " +sf " + tmpRawFile + " | " + frame + " +f -l " + frameLength + " -p " + framePeriod + " | "
                + window + " -l " + frameLength + " -L " + frameLengthOutput + " -w 1 -n 1 | " + mcep + " -a 0.42 -m "
                + mgcOrder + "  -l " + frameLengthOutput + " | " + swab + " +f > " + tmpFile;

        System.out.println("cmd=" + cmd);
        launchBatchProc(cmd, "getSptkMfcc", inFile);

        // Now get the data and add the header
        int numFrames;
        DataInputStream mfcData = null;
        Vector<Float> mfc = new Vector<Float>();

        mfcData = new DataInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));
        try {
            while (true) {
                mfc.add(mfcData.readFloat());
            }
        } catch (EOFException e) {
        }
        mfcData.close();

        numFrames = mfc.size();
        int numVectors = numFrames / mgcDimension;
        Mfccs mgc = new Mfccs(numVectors, mgcDimension);

        int k = 0;
        for (int i = 0; i < numVectors; i++) {
            for (int j = 0; j < mgcDimension; j++) {
                mgc.mfccs[i][j] = mfc.get(k);
                k++;
            }
        }
        // header parameters
        mgc.params.samplingRate = fs; /* samplingRateInHz */
        mgc.params.skipsize = (float) ss; /* skipSizeInSeconds */
        mgc.params.winsize = (float) ws; /* windowSizeInSeconds */

        mgc.writeMfccFile(outFile);
    }

    // public void getSptkSnackLf0() throws IOException, InterruptedException,
    // Exception {
    //
    // String inFile =
    // "/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.wav";
    // String outFile =
    // "/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.lf0";
    // String tmpFile = "/quality-control-experiment/slt/tmp.mfc";
    // String tmpRawFile = "/quality-control-experiment/slt/tmp.raw";
    // String tmpRawLongFile = "/quality-control-experiment/slt/tmp_long.raw";
    // String scriptFileName = "/quality-control-experiment/slt/lf0.tcl";
    // String snackFile = "/quality-control-experiment/slt/tmp.lf0";
    // String MAXPITCH;
    // String MINPITCH;
    // String gender = "female";
    // if (gender.contentEquals("female")) {
    // MAXPITCH = "500";
    // MINPITCH = "100";
    // } else { // male
    // MAXPITCH = "300";
    // MINPITCH = "75";
    // }
    // String FRAMELENGTH = "0.005";
    // String FRAMERATE = "16000";
    //
    // String cmd;
    //
    // // SOX and SPTK commands
    // String sox = "/usr/bin/sox";
    // String x2x = " /sw/SPTK-3.1/bin/x2x";
    // String step = "/sw/SPTK-3.1/bin/step";
    // String nrand = "/sw/SPTK-3.1/bin/nrand";
    // String sopr = "/sw/SPTK-3.1/bin/sopr";
    // String vopr = "/sw/SPTK-3.1/bin/vopr";
    // String SNACKDIR = "/sw/snack2.2.10/";
    //
    // // convert the wav file to raw file with sox
    // cmd = sox + " " + inFile + " " + tmpRawFile;
    // launchProc(cmd, "sox", inFile);
    //
    // // create temporary raw file, with 0.005 ms of silence (with a bit noise)
    // added
    // // at the beginning and 0.025 at the end
    // System.out.println("Create temporary raw file" + inFile);
    // cmd = step + " -l 80 -v 0.0 | x2x +fs > tmp.head\n" + step +
    // " -l 400 -v 0.0 | x2x +fs > tmp.tail\n" + "cat tmp.head "
    // + tmpRawFile + " tmp.tail | x2x +sf > tmp.long\n" +
    // "leng=`x2x +fa tmp.long | /usr/bin/wc -l`\n"
    // + "echo \"leng=$leng\"\n" + nrand + " -l $leng | " + sopr + " -m 50 | " +
    // vopr + " -a tmp.long | " + x2x
    // + " +fs > " + tmpRawLongFile + "\n" + "rm tmp.tail tmp.long tmp.head " +
    // tmpRawFile + "\n";
    //
    // System.out.println("cmd=" + cmd);
    // launchBatchProc(cmd, "getSptkSnackLf0", tmpRawFile);
    //
    // // Now extract F0 with snack and the modified raw file
    // System.out.println("scriptFileName = " + scriptFileName);
    // File script = new File(scriptFileName);
    //
    // System.out.println("Extracting LF0 coefficients from " + inFile);
    // if (script.exists())
    // script.delete();
    // PrintWriter toScript = new PrintWriter(new FileWriter(script));
    // toScript.println("#!" + SNACKDIR);
    // toScript.println("");
    // toScript.println("package require snack");
    // toScript.println("");
    // toScript.println("snack::sound s");
    // toScript.println("");
    // toScript.println("s read [lindex $argv 0] -fileformat RAW -rate [lindex $argv 1] -encoding Lin16 -byteorder littleEndian");
    // toScript.println("");
    // toScript.println("set fd [open [lindex $argv 2] w]");
    // toScript.println("set tmp [s pitch -method esps -maxpitch [lindex $argv 3] "
    // + "-minpitch [lindex $argv 4] -framelength [lindex $argv 5]]\n" +
    // "foreach line $tmp {\n"
    // + "  set x [lindex $line 0]\n" + "  if { $x == 0 } {\n" +
    // "    puts $fd -1.0e+10\n" + "  } else {\n"
    // + "    puts $fd [expr log($x)]\n" + "  }\n" + "}\n");
    // toScript.println("close $fd");
    // toScript.println("");
    // toScript.println("exit");
    // toScript.println("");
    // toScript.close();
    //
    // cmd = "tcl " + scriptFileName + " " + tmpRawLongFile + " " + FRAMERATE +
    // " " + snackFile + " " + MAXPITCH + " "
    // + MINPITCH + " " + FRAMELENGTH;
    // System.out.println("cmd=" + cmd);
    // launchProc(cmd, "getSptkSnackLf0", tmpRawLongFile);
    //
    // double[] f0 = new SnackTextfileDoubleDataSource(new
    // FileReader(snackFile)).getAllData();
    // for (int j = 0; j < f0.length; j++) {
    // System.out.println(j + "  f0[" + j + "]= " + f0[j]);
    // }
    //
    // }

    private void launchProc(String cmdLine, String task, String baseName) {

        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        try {
            proc = Runtime.getRuntime().exec(cmdLine);

            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while (true) {
                line = procStdout.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if (proc.exitValue() != 0) {
                throw new RuntimeException(task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "].");
            }
        } catch (IOException e) {
            throw new RuntimeException(task + " computation provoked an IOException on file [" + baseName + "].", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(task + " computation interrupted on file [" + baseName + "].", e);
        }

    }

    /**
     * A general process launcher for the various tasks but using an
     * intermediate batch file (copied from ESTCaller.java)
     * 
     * @param cmdLine
     *            the command line to be launched.
     * @param task
     *            a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the
     *            basename of the file currently processed, for error messages.
     */
    private void launchBatchProc(String cmdLine, String task, String baseName) {

        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String tmpFile = "./tmp.bat";

        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();

            /* make it executable... */
            proctmp = Runtime.getRuntime().exec("chmod +x " + tmpFile);
            proctmp.waitFor();
            proc = Runtime.getRuntime().exec(tmpFile);

            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while (true) {
                line = procStdout.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if (proc.exitValue() != 0) {
                throw new RuntimeException(task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "].");
            }

        } catch (IOException e) {
            throw new RuntimeException(task + " computation provoked an IOException on file [" + baseName + "].", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(task + " computation interrupted on file [" + baseName + "].", e);
        }

    }

    public static void main(String[] args) throws Exception {

        Test1 test = new Test1();

        // generate parameters out of a hsmm voice
        // test.generateParameters();

        // extract mfcc from a wav file using sptk
        // test.getSptkMfcc();

        // extract lf0 from a wav file using sptk and snack
        // test.getSptkSnackLf0();

        // Synthesis with external duration and f0
        // it requires ContinuousFeatureProcessors in the TARGETFEATURES file
        test.synthesisWithContinuousFeatureProcessors();

        // Synthesis with external duration and f0
        // it requires two external files: labels file .lab and logf0 file .lf0
        // The duration indicated in the lab file must correspond to the number
        // of frames in the .lf0 file
        // The lf0 file must be generated frame syncronous.
        // test.synthesisWithProsodySpecificationInExternalFiles();

    }

}
