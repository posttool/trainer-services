package hmi.synth.voc.train;

import hmi.phone.PhoneEl;
import hmi.phone.PhoneSet;
import hmi.util.DirectoryList;
import hmi.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class HTKLabeler {
    DirectoryList wavList;
    private File htk;
    private String outputDir;
    protected String xmlExt = ".xml";
    protected String labExt = ".lab";
    private int progress = -1;
    private String locale;
    protected String maryInputType;
    protected String maryOutputType;
    protected int percent = 0;
    protected File promtallophonesDir;
    protected Map<String, TreeMap<String, String>> dictionary;
    protected PhoneSet allophoneSet;
    protected int MAX_ITERATIONS = 150;
    protected int MAX_SP_ITERATION = 10; // iteration when intra word forced
                                         // pauses (ssil) are inserted
    protected int MAX_VP_ITERATION = 20; // iteration when virtual pauses (sp)
                                         // are inserted
    protected int MAX_MIX_ITERATION = 30; // iteration when mixtures are
                                          // increased

    protected int noIterCompleted = 0;

    public static final String FSEP = File.separator; // System.getProperty("file.separator");
    public static final String LOCALE = "locale";
    public static final String WAVDIR = "wavDir";
    public static final String WAVEXT = "wavExt";
    public static final String ALLOPHONES = "allophonesFile";
    public static final String HTDIR = "HTKLabeler.htDir";
    public static final String HTKDIR = "HTKLabeler.htkDir";
    public static final String OUTLABDIR = "HTKLabeler.outputLabDir";
    public static final String MAXITER = "HTKLabeler.maxNoOfIterations";
    public static final String PROMPTALLOPHONESDIR = "HTKLabeler.promptallophonesDir";
    public static final String MAXSPITER = "HTKLabeler.maxshortPauseIteration";
    public static final String AWK = "HTKLabeler.awkbin";

    private static final String HTK_SO = "-A -D -V -T 1"; // standard options
    private static final String Extract_FEAT = "MFCC_0"; // MFCC_E
    private static final String Train_FEAT = "MFCC_0_D_A"; // MFCC_E_D_A
    private int Train_VECTSIZE = 13 * 3; // 13; //13 without D_A; 13*3 with D_A
    private int NUMStates = 5;
    private int[] num_mixtures_for_state = { 2, 1, 2 };
    private int[] current_number_of_mixtures = { 1, 1, 1 }; // this is the
                                                            // starting number
                                                            // of mixtures (must
                                                            // be all ones)

    private ArrayList<Double> logProbFrame_array = new ArrayList<Double>();
    private ArrayList<Double> epsilon_array = new ArrayList<Double>();
    private int PHASE_NUMBER = 0;
    private double[] epsilon_PHASE = { 0.2, 0.05, 0.001, 0.0005 }; // 0 1 2 3
    private Properties props;

    String etcDir;

    public HTKLabeler(Properties p) {
        props = p;
        etcDir = getProp(HTDIR) + FSEP + "etc" + FSEP;
        wavList = new DirectoryList(getProp(WAVDIR), getProp(WAVEXT));
        // load bnl
        // read allophones
        dictionary = new TreeMap<String, TreeMap<String, String>>();
        promtallophonesDir = new File(getProp(PROMPTALLOPHONESDIR));
        if (!promtallophonesDir.exists()) {
            System.out.print(PROMPTALLOPHONESDIR + " " + getProp(PROMPTALLOPHONESDIR) + " does not exist; ");
            // if (!promtallophonesDir.mkdir()) {
            // throw new Error("Could not create PROMPTALLOPHONESDIR");
            // }
            // System.out.print("Ready.\n");
        }
    }

    public String getProp(String s) {
        return props.getProperty(s);
    }

    // public SortedMap<String, String> getDefaultProps() {
    // SortedMap<String, String> props = new TreeMap<String, String>();
    // String htkdir = System.getProperty("HTKDIR");
    // locale = getProp(LOCALE);
    // if (htkdir == null) {
    // htkdir = "/usr/bin/";
    // }
    // props.put(HTKDIR, htkdir);
    // props.put(HTDIR, getProp(ROOTDIR) + FSEP + "htk" + FSEP);
    // props.put(PROMPTALLOPHONESDIR, getProp(ROOTDIR) + FSEP +
    // "prompt_allophones" + FSEP);
    // props.put(OUTLABDIR, getProp(ROOTDIR) + FSEP + "htk" + FSEP + "lab" +
    // FSEP);
    // props.put(MAXITER, Integer.toString(MAX_ITERATIONS));
    // props.put(MAXSPITER, Integer.toString(MAX_SP_ITERATION));
    // props.put(AWK, "/usr/bin/awk");
    //
    // return props;
    // }

    // protected void setupHelp() {
    // props2Help = new TreeMap();
    // props2Help.put(HTKDIR,
    // "directory containing the HTK binary files of HTK,  i.e (/usr/local/bin/)");
    // props2Help.put(HTDIR,
    // "directory containing all files used for training and labeling. Will be created if it does not exist.");
    // props2Help.put(PROMPTALLOPHONESDIR,
    // "directory containing the prompt allophones files.");
    // props2Help.put(OUTLABDIR,
    // "Directory to store generated labels from HTK.");
    // //
    // props2Help.put(INITHTKDIR,"If you provide a path to previous HTK Directory, Models will init with those models. 
    // otherwise HTK Models will build with Flat-Start Initialization");
    // //
    // props2Help.put(RETRAIN,"true - Do re-training by initializing with given models. false - Do just Decoding");
    // props2Help.put(MAXITER,
    // "Maximum number of iterations used for training");
    // props2Help.put(MAXSPITER,
    
    // "Iteration number at which short-pause model need to insert.");
    // props2Help.put(AWK, "Location of awk binary.");
    // }

    public boolean compute() throws Exception {

        File htkFile = new File(getProp(HTKDIR) + FSEP + "HInit");
        if (!htkFile.exists()) {
            throw new IOException("HTK path setting is wrong. Because file " + htkFile.getAbsolutePath()
                    + " does not exist");
        }

        MAX_ITERATIONS = Integer.valueOf((getProp(MAXITER)));
        MAX_SP_ITERATION = Integer.valueOf((getProp(MAXSPITER)));

        System.out.println("Preparing voice database for labelling using HTK :");
        // make new directories htk and etc
        htk = new File(getProp(HTDIR));
        // get the output directory of files used by HTK
        outputDir = htk.getAbsolutePath() + "/etc";
        allophoneSet = PhoneSet.getPhoneSet(getProp(ALLOPHONES));

        // part 1: HTK basic setup and create required files

        // setup the HTK directory
        System.out.println("Setting up HTK directory ...");
        setup();
        System.out.println(" ... done.");
        // create required files for HTK
        createRequiredFiles();
        // creating phone dictionary. phone to phone mapping
        createPhoneDictionary();
        // Extract phone sequence from prompt_allophones files
        getPhoneSequence();

        // This is necessary to remove multiple sp: TODO: implement a loop and
        // check the result
        delete_multiple_sp_in_PhoneMLFile(etcDir + "htk.phones3.mlf", etcDir + "htk.phones4.mlf");
        delete_multiple_sp_in_PhoneMLFile(etcDir + "htk.phones4.mlf", etcDir + "htk.phones5.mlf");
        delete_multiple_sp_in_PhoneMLFile(etcDir + "htk.phones5.mlf", etcDir + "htk.phones6.mlf");
        delete_multiple_sp_in_PhoneMLFile(etcDir + "htk.phones6.mlf", etcDir + "htk.phones7.mlf");
        delete_multiple_sp_in_PhoneMLFile(etcDir + "htk.phones7.mlf", etcDir + "htk.phones3.mlf");

        // part 2: Feature Extraction using HCopy
        System.out.println("Feature Extraction:");
        featureExtraction();
        System.out.println("... Done.");

        // Part 3: Initialize Flat-start initialization
        System.out.println("HTK Training:");
        initialiseHTKTrain();
        createTrainFile();

        // Part 4: training with HERest
        herestTraining();
        System.out.println("... Done.");

        // Part 5: Force align with HVite
        System.out.println("HTK Align:");
        hviteAligning();
        System.out.println("... Done.");

        // Part 6: Extra model statistics
        System.out.println("Generating Extra model statistics...");
        htkExtraModels();
        System.out.println("... Done.");

        // Part 6: Generate Labels in required format
        System.out.println("Generating Labels in required format...");
        getProperLabelFormat();
        System.out.println(" ... done.");
        System.out.println("Label file Generation Successfully completed using HTK !");

        return true;
    }

    private void setup() throws IOException, InterruptedException, Exception {

        htk.mkdir();
        File lab = new File(htk.getAbsolutePath() + "/lab");
        // call setup of HTK in this directory
        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        // go to htk directory and setup Directory Structure
        pw.print("( cd " + htk.getAbsolutePath() + "; mkdir -p hmm" + "; mkdir -p etc" + "; mkdir -p feat"
                + "; mkdir -p config" + "; mkdir -p lab" + "; exit )\n");
        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

    }

    private void createPhoneDictionary() throws Exception {
        PrintWriter transLabelOut = new PrintWriter(new FileOutputStream(new File(etcDir + "htk" + ".phone.dict")));
        PrintWriter phoneListOut = new PrintWriter(new FileOutputStream(new File(etcDir + "htk" + ".phone.list")));
        PrintWriter phoneListOut1 = new PrintWriter(new FileOutputStream(new File(etcDir + "htk" + ".phone2.list")));
        PrintWriter phoneListOut2 = new PrintWriter(new FileOutputStream(new File(etcDir + "htk" + ".phone3.list")));

        String phoneSeq;
        // transLabelOut.println("#!MLF!#");
        Set<String> phonesList = allophoneSet.getPhoneNames();
        Iterator<String> it = phonesList.iterator();
        while (it.hasNext()) {
            // System.out.println(it.next());
            String phon = it.next();

            if (phon.equals("_")) {
                continue;
                // phon = "sp";
            }
            phon = replaceTrickyPhones(phon);
            transLabelOut.println(phon + " " + phon);
            phoneListOut.println(phon);
            phoneListOut1.println(phon);
            phoneListOut2.println(phon);
        }
        transLabelOut.println("sil" + " " + "sil");
        phoneListOut.println("sil");
        phoneListOut1.println("sil");
        phoneListOut2.println("sil");

        transLabelOut.println("ssil" + " " + "ssil");
        phoneListOut1.println("ssil");
        phoneListOut2.println("ssil");

        transLabelOut.println("sp" + " " + "sp");
        phoneListOut2.println("sp");

        // commented G End Word
        // commented G Start Word
        /*
         * phoneListOut.println("GEW"); phoneListOut1.println("GEW");
         * phoneListOut2.println("GEW");
         * 
         * phoneListOut.println("GSW"); phoneListOut1.println("GSW");
         * phoneListOut2.println("GSW");
         * 
         * transLabelOut.println("sp"+" "+"GEW");
         * transLabelOut.println("sp"+" "+"GEW sp");
         * transLabelOut.println("sp"+" "+"sp GSW");
         * transLabelOut.println("sp"+" "+"GSW");
         * transLabelOut.println("sp"+" "+"GEW sp GSW");
         */

        transLabelOut.flush();
        transLabelOut.close();
        phoneListOut.flush();
        phoneListOut.close();
        phoneListOut1.flush();
        phoneListOut1.close();
        phoneListOut2.flush();
        phoneListOut2.close();
    }

    private void createRequiredFiles() throws Exception {

        // Creating mkphones0.led file, which insert and delete pauses
        // TODO: check is it used?
        File file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "mkphone0.led");
        PrintWriter pw = new PrintWriter(new FileWriter(file));
        pw.println("EX");
        pw.println("IS sil sil");
        // pw.println("DE sp"); // Short pause modeling
        pw.flush();
        pw.close();

        // Creating mkphones1.led file, which delete multiple sp pauses
        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "mkphone1.led");
        pw = new PrintWriter(new FileWriter(file));
        pw.println("ME sp sp sp");
        pw.println("ME sil sil sp");
        pw.println("ME sil sp sil");
        pw.println("ME ssil ssil sp");
        pw.println("ME ssil sp ssil");
        pw.flush();
        pw.close();

        // creating a HTK Feature Extraction config file
        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "featEx.conf");
        pw = new PrintWriter(new FileWriter(file));
        pw.println("SOURCEFORMAT = WAV             # Gives the format of speech files ");
        pw.println("TARGETKIND = " + Extract_FEAT + "        #Identifier of the coefficients to use");
        pw.println("WINDOWSIZE = 100000.0         # = 10 ms = length of a time frame");
        pw.println("TARGETRATE = 50000.0          # = 5 ms = frame periodicity");
        pw.println("NUMCEPS = 12                  # Number of MFCC coeffs (here from c1 to c12)");
        pw.println("USEHAMMING = T                # Use of Hamming funtion for windowing frames");
        pw.println("PREEMCOEF = 0.97              # Pre-emphasis coefficient");
        pw.println("NUMCHANS = 26                 # Number of filterbank channels");
        pw.println("CEPFILTER = 22                # Length of ceptral filtering");
        pw.println("ENORMALISE = F                # Energy measure normalization (sentence level)");

        pw.flush();
        pw.close();

        // creating a HTK Training initialize config file
        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "htkTrain.conf");
        pw = new PrintWriter(new FileWriter(file));

        pw.println("TARGETKIND = " + Train_FEAT + "        #Identifier of the coefficients to use");
        pw.println("PARAMETERKIND = " + Train_FEAT + "");
        pw.println("WINDOWSIZE = 100000.0         # = 10 ms = length of a time frame");
        pw.println("TARGETRATE = 50000.0          # = 5 ms = frame periodicity");
        pw.println("NUMCEPS = 12                  # Number of MFCC coeffs (here from c1 to c12)");
        pw.println("USEHAMMING = T                # Use of Hamming funtion for windowing frames");
        pw.println("PREEMCOEF = 0.97              # Pre-emphasis coefficient");
        pw.println("NUMCHANS = 26                 # Number of filterbank channels");
        pw.println("CEPFILTER = 22                # Length of ceptral filtering");
        pw.println("ENORMALISE = F                # Energy measure normalization (sentence level)");

        pw.flush();
        pw.close();

        // Create an input file to HTK for Feature Extraction
        file = new File(etcDir + "featEx.list");
        pw = new PrintWriter(new FileWriter(file));
        for (int i = 0; i < wavList.getLength(); i++) {
            // System.out.println( "    " + bnl.getName(i) );
            String input = getProp(WAVDIR) + FSEP + wavList.getName(i) + getProp(WAVEXT);
            String output = getProp(HTDIR) + FSEP + "feat" + FSEP + wavList.getName(i) + ".mfcc";
            pw.println(input + " " + output);
        }
        pw.flush();
        pw.close();

        // creating list of training files
        file = new File(etcDir + "htkTrain.list");
        pw = new PrintWriter(new FileWriter(file));
        for (int i = 0; i < wavList.getLength(); i++) {
            // System.out.println( "    " + bnl.getName(i) );
            String mFile = getProp(HTDIR) + FSEP + "feat" + FSEP + wavList.getName(i) + ".mfcc";
            pw.println(mFile);
        }
        pw.flush();
        pw.close();

        // creating a hmm protofile
        int vectorSize = Train_VECTSIZE;
        int numStates = NUMStates;

        if (num_mixtures_for_state.length != numStates - 2) {
            throw new RuntimeException("Mixture num_mixtures_for_state lenght does not correspond to numStates");
        }

        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "htk.proto");
        pw = new PrintWriter(new FileWriter(file));
        pw.println("<BeginHMM>");
        pw.println("<NumStates> " + numStates + " <VecSize> " + vectorSize + " <" + Train_FEAT + ">");

        for (int state = 2; state < numStates; state++) {

            pw.println("<State> " + state);
            // pw.println("<NumMixes> " + num_mixtures_for_state[state-2]);
            // for(int mix=1;mix<=num_mixtures_for_state[state-2];mix++){
            // pw.println("<Mixture> " + mix + " " +
            // 1.0/num_mixtures_for_state[state-2]);

            pw.println("<Mean> " + vectorSize);
            for (int j = 0; j < vectorSize; j++) {
                pw.print(" 0.0");
            }
            pw.println();
            pw.println("<Variance> " + vectorSize);
            for (int j = 0; j < vectorSize; j++) {
                pw.print(" 1.0");
            }
            pw.println();

        }
        // }
        pw.println("<TransP> " + numStates);
        pw.println("0.0 1.0 0.0 0.0 0.0");
        pw.println("0.0 0.6 0.4 0.0 0.0");
        pw.println("0.0 0.0 0.6 0.4 0.0");
        pw.println("0.0 0.0 0.0 0.7 0.3");
        pw.println("0.0 0.0 0.0 0.0 1.0");
        pw.println("<EndHMM>");
        pw.flush();
        pw.close();

        // Creating SSIL Silence modeling config file

        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "sil.hed");
        pw = new PrintWriter(new FileWriter(file));

        pw.println("AT 2 4 0.2 {sil.transP}");
        pw.println("AT 4 2 0.2 {sil.transP}");
        // pw.println("AT 1 3 0.3 {ssil.transP}");
        // pw.println("TI silst {sil.state[3],ssil.state[2]}");
        pw.println("AT 2 4 0.2 {ssil.transP}");
        pw.println("AT 4 2 0.2 {ssil.transP}");
        // added tied states...
        pw.println("TI silst2 {sil.state[2],ssil.state[2]}");
        pw.println("TI silst3 {sil.state[3],ssil.state[3]}");
        pw.println("TI silst4 {sil.state[4],ssil.state[4]}");

        pw.flush();
        pw.close();

        // Creating SP Silence modeling config file

        file = new File(getProp(HTDIR) + FSEP + "config" + FSEP + "sil_vp.hed");
        pw = new PrintWriter(new FileWriter(file));

        // sp 3 state case:
        // pw.println("AT 1 3 0.3 {sp.transP}");
        // pw.println("TI ssilst {ssil.state[3],sp.state[2]}");

        // sp 5 state case:
        pw.println("AT 1 5 0.3 {sp.transP}");
        pw.println("TI ssilst2 {ssil.state[2],sp.state[2]}");
        pw.println("TI ssilst3 {ssil.state[3],sp.state[3]}");
        pw.println("TI ssilst4 {ssil.state[4],sp.state[4]}");

        pw.flush();
        pw.close();

    }

    /**
     * delete sp repetition on htk.phones3.mlf
     */
    private void delete_multiple_sp_in_PhoneMLFile(String filein, String fileout) throws Exception {
        String hled = getProp(HTKDIR) + FSEP + "HLEd";
        File htkFile = new File(hled);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        // String phoneMLF3 = getProp(HTDIR)+FSEP
        // +"etc"+FSEP+"htk.phones3.mlf";

        // String phoneMLFtmpin = getProp(HTDIR)+FSEP
        // +"etc"+FSEP+"htk.phones3_tmp_in.mlf";

        // String phoneMLFtmpout = getProp(HTDIR)+FSEP
        // +"etc"+FSEP+"htk.phones3_tmp_out.mlf";

        String mkphoneLED = getProp(HTDIR) + FSEP + "config" + FSEP + "mkphone1.led";

        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( " + hled + " -l '*' -i " + fileout + " " + mkphoneLED + " " + filein + "; exit )\n");

        pw.print("( " + hled + " -l '*' -i " + fileout + " " + mkphoneLED + " " + filein + "; exit )\n");
        // +"; "

        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        process.exitValue();

    }

    /**
     * create phone master label file (Not used)
     */
    private void createPhoneMLFile() throws Exception {
        String hled = getProp(HTKDIR) + FSEP + "HLEd";
        File htkFile = new File(hled);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String dict = etcDir + "htk.dict";
        String phoneMLF = etcDir + "htk.phones.mlf";
        String wordsMLF = etcDir + "htk.words.mlf";
        String mkphoneLED = getProp(HTDIR) + FSEP + "config" + FSEP + "mkphone0.led";

        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( " + hled + " -l '*' -d " + dict + " -i " + phoneMLF + " " + mkphoneLED + " " + wordsMLF
                + "; exit )\n");

        pw.print("( " + hled + " -l '*' -d " + dict + " -i " + phoneMLF + " " + mkphoneLED + " " + wordsMLF
        // +"; "
                + "; exit )\n");
        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

    }

    /* Feature Extraction */
    private void featureExtraction() throws Exception {

        String hcopy = getProp(HTKDIR) + FSEP + "HCopy";
        File htkFile = new File(hcopy);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getProp(HTDIR) + FSEP + "config" + FSEP + "featEx.conf";
        String listFile = etcDir + "featEx.list";
        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( cd " + getProp(HTDIR) + "; " + hcopy + " -T 1 -C " + configFile + " -S " + listFile
                + " > log_featureExtraction.txt" + "; exit )\n");
        pw.print("( cd " + getProp(HTDIR) + "; " + hcopy + " -T 1 -C " + configFile + " -S " + listFile
                + " > log_featureExtraction.txt" + "; exit )\n");
        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }
    }

    /**
     * Initialize HTK Training process
     * 
     * @throws Exception
     */
    private void initialiseHTKTrain() throws Exception {

        String hcompv = getProp(HTKDIR) + FSEP + "HCompV";
        File htkFile = new File(hcompv);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getProp(HTDIR) + FSEP + "config" + FSEP + "htkTrain.conf";
        String listFile = etcDir + "htkTrain.list";
        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

        System.out.println("( cd " + getProp(HTDIR) + " ; mkdir hmm/hmm-dummy ; " + " mkdir hmm/hmm-final ; " + hcompv
                + " " + HTK_SO + " -C " + configFile + " -f 0.01 -m -S " + listFile + " -M " + getProp(HTDIR) + FSEP
                + "hmm/hmm-dummy " + getProp(HTDIR) + FSEP + "config" + FSEP + "htk.proto"
                + " > log_initialiseHTKTrain.txt" + "; exit )\n");
        pw.print("( cd " + getProp(HTDIR) + " ; mkdir hmm/hmm-dummy ; " + " mkdir hmm/hmm-final ; " + hcompv + " "
                + HTK_SO + " -C " + configFile + " -f 0.01 -m -S " + listFile + " -M " + getProp(HTDIR) + FSEP
                + "hmm/hmm-dummy " + getProp(HTDIR) + FSEP + "config" + FSEP + "htk.proto"
                + " > log_initialiseHTKTrain.txt" + "; exit )\n");
        pw.flush();
        // shut down
        pw.close();

        process.waitFor();

        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

    }

    /**
     * Create HMMs for each phone from Global HMMs
     */
    private void createTrainFile() throws Exception {

        String script;
        String hmmDir = getProp(HTDIR) + FSEP + "hmm" + FSEP;

        /**
         * TODO: Replace below 'gawk' script with Java method.
         */

        script = "mkdir hmm/hmm0\n" + "head -3 hmm/hmm-dummy/htk > hmm/hmm0/hmmdefs\n"
                + "for s in `cat etc/htk.phone.list`\n" + "do\n" + "echo \"~h \\\"$s\\\"\" >> hmm/hmm0/hmmdefs\n"
                + getProp(AWK) + " '/BEGINHMM/,/ENDHMM/ { print $0 }' hmm/hmm-dummy/htk >> hmm/hmm0/hmmdefs\n"
                + "done\n";
        // creating list of training files
        File file = new File(etcDir + "htkTrainScript.sh");
        PrintWriter pw = new PrintWriter(new FileWriter(file));
        pw.println(script);
        pw.flush();
        pw.close();

        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

        System.out.println("( cd " + getProp(HTDIR) + "; sh etc" + FSEP + "htkTrainScript.sh"
                + " > log_htkTrainScript.txt" + "; exit )\n");
        pw.print("( cd " + getProp(HTDIR) + "; sh etc" + FSEP + "htkTrainScript.sh" + " > log_htkTrainScript.txt"
                + "; exit )\n");

        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

        PrintWriter macroFile = new PrintWriter(new FileOutputStream(new File(hmmDir + "hmm0" + FSEP + "macros")));
        macroFile.println("~o\n" + "<VecSize> 13\n" + "<" + Train_FEAT + ">");
        macroFile.println(FileUtils.getFileAsString(new File(hmmDir + "hmm-dummy" + FSEP + "vFloors"), "ASCII"));
        macroFile.flush();
        macroFile.close();

    }

    // flat start
    private void herestTraining() throws Exception {

        String herest = getProp(HTKDIR) + FSEP + "HERest";
        String hhed = getProp(HTKDIR) + FSEP + "HHEd";

        File htkFile = new File(herest);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }

        String configFile = getProp(HTDIR) + FSEP + "config" + FSEP + "htkTrain.conf";
        String hhedconf = getProp(HTDIR) + FSEP + "config" + FSEP + "sil.hed";

        String hhedconf_vp = getProp(HTDIR) + FSEP + "config" + FSEP + "sil_vp.hed";

        String trainList = etcDir + "htkTrain.list";
        String phoneList = etcDir + "htk.phone.list";

        String hmmDir = getProp(HTDIR) + FSEP + "hmm" + FSEP;
        String phoneMlf = etcDir + "htk.phones.mlf";

        int BEST_ITERATION = MAX_ITERATIONS;
        int SP_ITERATION = -1;
        int VP_ITERATION = -1;
        int change_mix_iteration = -1;
        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {

            System.out.println("Iteration number: " + iteration);

            File hmmItDir = new File(hmmDir + "hmm" + iteration);
            if (!hmmItDir.exists())
                hmmItDir.mkdir();

            Runtime rtime = Runtime.getRuntime();
            // get a shell
            Process process = rtime.exec("/bin/bash");
            // get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

            if (PHASE_NUMBER == 0) {

                if (iteration == (SP_ITERATION + 1)) {

                    phoneMlf = etcDir + "htk.phones2.mlf";
                    phoneList = etcDir + "htk.phone2.list";

                    System.out.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.flush();
                    // shut down
                    pw.close();
                    process.waitFor();
                    // check exit value
                    if (process.exitValue() != 0) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        throw new Exception(errorReader.readLine());
                    }

                    // copy of logProbFrame_array in current iteration
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);

                    // now we enter in PHASE 1
                    PHASE_NUMBER = 1;
                    System.out.println("Now we enter in PHASE:" + PHASE_NUMBER);
                    continue;
                }

                // check epsilon_array
                if (iteration > 2) {
                    if (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER] || iteration == MAX_SP_ITERATION) {
                        SP_ITERATION = iteration;
                        insertShortPause(iteration);
                        String oldMacro = hmmDir + "hmm" + (iteration - 1) + FSEP + "macros";
                        String newMacro = hmmDir + "hmm" + iteration + FSEP + "macros";
                        FileUtils.copy(oldMacro, newMacro);

                        // copy of logProbFrame_array in current iteration
                        logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                        epsilon_array.add(100000000.0);
                        continue;
                    }
                }
            }

            // /-----------------
            if (PHASE_NUMBER == 1) {
                if (iteration == (VP_ITERATION + 1)) {
                    phoneMlf = etcDir + "htk.phones3.mlf";
                    phoneList = etcDir + "htk.phone3.list";

                    System.out.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf_vp + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf_vp + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.flush();
                    // shut down
                    pw.close();
                    process.waitFor();
                    // check exit value
                    if (process.exitValue() != 0) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        throw new Exception(errorReader.readLine());
                    }

                    // copy of logProbFrame_array in current iteration
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);

                    // now we enter in PHASE 2
                    PHASE_NUMBER = 2;
                    System.out.println("Now we enter in PHASE:" + PHASE_NUMBER);
                    continue;
                }

                // check epsilon_array
                if (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER] || iteration == MAX_VP_ITERATION) {
                    VP_ITERATION = iteration;
                    insertVirtualPauseThreeStates(iteration);
                    String oldMacro = hmmDir + "hmm" + (iteration - 1) + FSEP + "macros";
                    String newMacro = hmmDir + "hmm" + iteration + FSEP + "macros";
                    FileUtils.copy(oldMacro, newMacro);

                    // copy of logProbFrame_array in current iteration
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);
                    continue;
                }
            }

            // /-----------------
            if (PHASE_NUMBER == 2) {
                // check epsilon_array
                // the following change_mix_iteration + 2 is used to allow more
                // than one re-estimation after insertion of new
                // mixture
                // Because just after the insertion the delta can be negative

                if (((iteration != change_mix_iteration + 2) && (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER]))
                        || iteration == MAX_MIX_ITERATION) {
                    change_mix_iteration = iteration;
                    MAX_MIX_ITERATION = -1;

                    // Creating Increasing mixture config file dynamic iteration
                    String hhedconf_mix = getProp(HTDIR) + FSEP + "config" + FSEP + "sil_mix_" + iteration + ".hed";
                    File file = new File(hhedconf_mix);
                    PrintWriter hhed_conf_pw = new PrintWriter(new FileWriter(file));

                    // MU 3 {*.state[2].mix}
                    Boolean need_other_updates = false;
                    for (int state = 0; state < num_mixtures_for_state.length; state++) {
                        if (current_number_of_mixtures[state] < num_mixtures_for_state[state]) {
                            int wanted_mix = current_number_of_mixtures[state] + 1;
                            int state_to_print = state + 2;
                            hhed_conf_pw.println("MU " + wanted_mix + "{*.state[" + state_to_print + "].mix}");

                            current_number_of_mixtures[state] = wanted_mix;

                            if (current_number_of_mixtures[state] < num_mixtures_for_state[state]) {
                                need_other_updates = true;
                            }
                        }
                    }

                    if (!need_other_updates) {
                        // copy of logProbFrame_array in current iteration
                        // logProbFrame_array.add(logProbFrame_array.get(iteration-2));
                        // epsilon_array.add(100000000.0);
                        // now we enter in PHASE 3
                        PHASE_NUMBER = 3;
                        System.out.println("Now we enter in PHASE:" + PHASE_NUMBER);
                        // continue;
                    }

                    hhed_conf_pw.flush();
                    hhed_conf_pw.close();

                    System.out.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf_mix + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.println("( cd " + getProp(HTDIR) + "; " + hhed + " " + HTK_SO + " -H " + hmmDir + "hmm"
                            + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP
                            + "hmmdefs" + " -M " + hmmDir + "hmm" + iteration + " " + hhedconf_mix + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
                    pw.flush();
                    // shut down
                    pw.close();
                    process.waitFor();
                    // check exit value
                    if (process.exitValue() != 0) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        throw new Exception(errorReader.readLine());
                    }

                    // copy of logProbFrame_array in current iteration
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);
                    continue;
                }
            }

            // /-----------------
            if (PHASE_NUMBER == 3) {
                // check epsilon_array
                if (((iteration != change_mix_iteration + 2) && (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER]))
                        || iteration == MAX_ITERATIONS) {
                    int last = iteration - 1;
                    int previus_last = iteration - 2;

                    System.out
                            .println("Average log prob per frame has not beeen increased too much respect the previus iteration:");
                    System.out.println("Average log prob per frame at last HREST iteration (" + last + ")-> "
                            + logProbFrame_array.get(iteration - 2));
                    System.out.println("Average log prob per frame at previus HREST iteration (" + previus_last
                            + ")-> " + logProbFrame_array.get(iteration - 3));
                    System.out.println("Delta -> " + epsilon_array.get(iteration - 2));
                    System.out.println("Suggested Action -> stop the iterations.");

                    if (logProbFrame_array.get(iteration - 3) > logProbFrame_array.get(iteration - 2)) {
                        BEST_ITERATION = iteration - 2;
                    } else {
                        BEST_ITERATION = iteration - 1;
                    }
                    break;
                }
            }

            // Normal HEREST:
            System.out.println("( cd " + getProp(HTDIR) + "; " + herest + " " + HTK_SO + " -C " + configFile + " -I "
                    + phoneMlf + " -t 250.0 150.0 1000.0" + " -S " + trainList + " -H " + hmmDir + "hmm"
                    + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP + "hmmdefs"
                    + " -M " + hmmDir + "hmm" + iteration + " " + phoneList + " >> log_herestTraining_" + iteration
                    + ".txt" + "; exit )\n");

            pw.println("( cd " + getProp(HTDIR) + "; " + herest + " " + HTK_SO + " -C " + configFile + " -I "
                    + phoneMlf + " -t 250.0 150.0 1000.0" + " -S " + trainList + " -H " + hmmDir + "hmm"
                    + (iteration - 1) + FSEP + "macros" + " -H " + hmmDir + "hmm" + (iteration - 1) + FSEP + "hmmdefs"
                    + " -M " + hmmDir + "hmm" + iteration + " " + phoneList + " >> log_herestTraining_" + iteration
                    + ".txt" + "; exit )\n");
            pw.flush();
            // shut down
            pw.close();
            process.waitFor();
            // check exit value
            if (process.exitValue() != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                throw new Exception(errorReader.readLine());
            }

            // update average_log_prob_per_frame and deltas
            check_average_log_prob_per_frame(iteration);

            System.out.println("Delta average log prob per frame to respect previus iteration-> "
                    + epsilon_array.get(iteration - 1));
            System.out.println("Current PHASE: " + PHASE_NUMBER);
            System.out.println("Current state and number of mixtures (for each phoneme): "
                    + Arrays.toString(current_number_of_mixtures));

            System.out.println("---------------------------------------");
        }

        System.out.println("***********\n");
        System.out.println("BEST ITERATION: " + BEST_ITERATION);
        System.out.println("COPYNING BEST ITERATION FILES IN hmm-final directory");
        System.out.println("logProbFrame_array:" + logProbFrame_array.toString());

        System.out.println("epsilon_array:" + epsilon_array.toString());

        System.out.println("***********\n");

        String oldMacro = hmmDir + "hmm" + BEST_ITERATION + FSEP + "macros";
        String newMacro = hmmDir + "hmm-final" + FSEP + "macros";
        FileUtils.copy(oldMacro, newMacro);

        String oldHmmdefs = hmmDir + "hmm" + BEST_ITERATION + FSEP + "hmmdefs";
        String newHmmdefs = hmmDir + "hmm-final" + FSEP + "hmmdefs";
        FileUtils.copy(oldHmmdefs, newHmmdefs);

    }

    private void check_average_log_prob_per_frame(int iteration) throws IOException {
        // TODO Auto-generated method stub

        String filename = getProp(HTDIR) + FSEP + "log_herestTraining_" + iteration + ".txt";

        // Reestimation complete - average log prob per frame = xxx
        Pattern p = Pattern.compile("^.*average log prob per frame = (.*)$");

        FileReader fr = new FileReader(filename);

        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        Matcher m;
        Boolean found = false;

        while ((st = reader.readLine()) != null) {
            // System.out.println(st);
            m = p.matcher(st);
            if (m.find()) {
                Double logProbFrame = Double.parseDouble(m.group(1));
                logProbFrame_array.add(logProbFrame);

                System.out.println("Average log prob per frame at iteration " + iteration + " from file is "
                        + m.group(1) + " equal to " + logProbFrame);

                found = true;
                break;
            }
        }
        System.out.flush();

        if (!found) {
            throw new RuntimeException("No match of average log prob per frame in " + filename);
        }

        // double epsilon = 0.0001;
        double delta;

        if (iteration > 1)
            delta = logProbFrame_array.get(iteration - 1) - logProbFrame_array.get(iteration - 2);
        else
            delta = 10000000.0;

        epsilon_array.add(delta);

    }

    private void insertShortPause(int i) throws Exception {
        String hmmDir = getProp(HTDIR) + FSEP + "hmm" + FSEP;
        boolean okprint = false;
        boolean silprint = false;
        System.out.println("F1:" + hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs");
        System.out.println("F2:" + hmmDir + "hmm" + i + FSEP + "hmmdefs");

        String line, spHmmDef = "";
        // File hmmDef = new File(hmmDir+"hmm"+(i-1)+FSEP+"hmmdefs");
        BufferedReader hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        while ((line = hmmDef.readLine()) != null) {

            if (line.matches("^.*\"sil\".*$")) {
                okprint = true;
                spHmmDef += "~h \"ssil\"\n";
                continue;
            }

            if (okprint && line.matches("^.*ENDHMM.*$")) {
                spHmmDef += line + "\n";
                continue;
            }

            if (okprint) {
                spHmmDef += line + "\n";
            }
        }
        hmmDef.close();

        hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        PrintWriter newHmmDef = new PrintWriter(new FileWriter(hmmDir + "hmm" + i + FSEP + "hmmdefs"));

        while ((line = hmmDef.readLine()) != null) {
            newHmmDef.println(line.trim());
        }
        newHmmDef.println(spHmmDef);
        newHmmDef.flush();
        newHmmDef.close();
        hmmDef.close();

    }

    /*
     * Add sp model copying the centre state of ssil
     */
    private void insertVirtualPause(int i) throws Exception {
        String hmmDir = getProp(HTDIR) + FSEP + "hmm" + FSEP;
        boolean okprint = false;
        boolean okprint2 = false;
        boolean silprint = false;
        System.out.println("F1:" + hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs");
        System.out.println("F2:" + hmmDir + "hmm" + i + FSEP + "hmmdefs");

        String line, spHmmDef = "";
        // File hmmDef = new File(hmmDir+"hmm"+(i-1)+FSEP+"hmmdefs");
        BufferedReader hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        while ((line = hmmDef.readLine()) != null) {

            if (line.matches("^.*\"ssil\".*$")) {
                okprint = true;
                spHmmDef += "~h \"sp\"\n";
                spHmmDef += "<BeginHMM>\n";
                spHmmDef += "<NumStates> 3\n";
                spHmmDef += "<State> 2\n";
                continue;
            }
            // TODO: add
            if (okprint && line.matches("^.*<STATE> 3.*$")) {
                okprint2 = true;
                continue;
            }

            if (okprint && okprint2 & line.matches("^.*<STATE> 4.*$")) {
                okprint = false;
                okprint2 = false;
                continue;
            }

            if (okprint && okprint2) {
                spHmmDef += line + "\n";
            }

        }

        spHmmDef += "<TRANSP> 3\n";
        spHmmDef += "0.   1.   0.\n";
        spHmmDef += "0.   0.9  0.1\n";
        spHmmDef += "0.   0.   0. \n";
        spHmmDef += "<ENDHMM>\n";

        hmmDef.close();

        hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        PrintWriter newHmmDef = new PrintWriter(new FileWriter(hmmDir + "hmm" + i + FSEP + "hmmdefs"));

        while ((line = hmmDef.readLine()) != null) {
            newHmmDef.println(line.trim());
        }
        newHmmDef.println(spHmmDef);
        newHmmDef.flush();
        newHmmDef.close();
        hmmDef.close();
    }

    /*
     * Add sp model copying the 3 states of ssil remeber to use appropiate AT
     * and TI
     */
    private void insertVirtualPauseThreeStates(int i) throws Exception {
        String hmmDir = getProp(HTDIR) + FSEP + "hmm" + FSEP;
        boolean okprint = false;
        boolean okprint2 = false;
        boolean silprint = false;
        System.out.println("F1:" + hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs");
        System.out.println("F2:" + hmmDir + "hmm" + i + FSEP + "hmmdefs");

        String line, spHmmDef = "";
        // File hmmDef = new File(hmmDir+"hmm"+(i-1)+FSEP+"hmmdefs");
        BufferedReader hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        while ((line = hmmDef.readLine()) != null) {

            if (line.matches("^.*\"ssil\".*$")) {
                okprint = true;
                spHmmDef += "~h \"sp\"\n";
                spHmmDef += "<BeginHMM>\n";
                spHmmDef += "<NumStates> 5\n";
                spHmmDef += "<State> 2\n";
                continue;
            }
            // TODO: add
            if (okprint && line.matches("^.*<STATE> 2.*$")) {
                okprint2 = true;
                continue;
            }

            if (okprint && okprint2 & line.matches("^.*<ENDHMM>.*$")) {
                okprint = false;
                okprint2 = false;
                continue;
            }

            if (okprint && okprint2) {
                spHmmDef += line + "\n";
            }

        }

        /*
         * spHmmDef += "<TRANSP> 3\n"; spHmmDef += "0.   1.   0.\n"; spHmmDef +=
         * "0.   0.9  0.1\n"; spHmmDef += "0.   0.   0. \n"; spHmmDef +=
         * "<ENDHMM>\n";
         */
        spHmmDef += "<ENDHMM>\n";

        hmmDef.close();

        hmmDef = new BufferedReader(new FileReader(hmmDir + "hmm" + (i - 1) + FSEP + "hmmdefs"));
        PrintWriter newHmmDef = new PrintWriter(new FileWriter(hmmDir + "hmm" + i + FSEP + "hmmdefs"));

        while ((line = hmmDef.readLine()) != null) {
            newHmmDef.println(line.trim());
        }
        newHmmDef.println(spHmmDef);
        newHmmDef.flush();
        newHmmDef.close();
        hmmDef.close();
    }

    /**
     * Force Align database for Automatic labels
     * 
     * @throws Exception
     */
    private void hviteAligning() throws Exception {

        String hvite = getProp(HTKDIR) + FSEP + "HVite"; // -A -D -V
                                                         // -T 1 "; //
                                                         // to add -A
                                                         // -D -V -T 1
                                                         // in every
                                                         // function
        File htkFile = new File(hvite);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getProp(HTDIR) + FSEP + "config" + FSEP + "htkTrain.conf";
        String listFile = etcDir + "htkTrain.list";

        // Virtual sp change_ phoneList should be a member?
        // Without sp:
        /*
         * String phoneList = getProp(HTDIR)+FSEP +"etc"+FSEP+"htk.phone2.list";
         */

        // Whit sp:

        String phoneList = etcDir + "htk.phone3.list";

        String hmmDef = getProp(HTDIR) + FSEP + "hmm" + FSEP + "hmm-final" + FSEP + "hmmdefs";
        String macros = getProp(HTDIR) + FSEP + "hmm" + FSEP + "hmm-final" + FSEP + "macros";

        // Virtual sp change_ phoneMlf should be a member?

        // Without sp:
        /*
         * String phoneMlf = getProp(HTDIR)+FSEP +"etc"+FSEP+"htk.phones2.mlf";
         */
        // Whit sp:
        String phoneMlf = etcDir + "htk.phones3.mlf";

        String alignedMlf = getProp(HTDIR) + FSEP + "aligned.mlf";
        String phoneDict = etcDir + "htk.phone.dict";
        String labDir = getProp(HTDIR) + FSEP + "lab";

        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell

        // when no sp use (-m)!

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( cd " + getProp(HTDIR) + "; " + hvite + " " + HTK_SO + " -b sil -l " + labDir
                + " -o W -C " + configFile + " -a -H " + macros + " -H " + hmmDef + " -i " + alignedMlf
                + " -t 250.0 -y lab" + " -I " + phoneMlf + " -S " + listFile + " " + phoneDict + " " + phoneList
                + " > log_hviteAligning.txt" + "; exit )\n");

        pw.println("( cd " + getProp(HTDIR) + "; " + hvite + " " + HTK_SO + " -b sil -l " + labDir + " -o W -C "
                + configFile + " -a -H " + macros + " -H " + hmmDef + " -i " + alignedMlf + " -t 250.0 -y lab" + " -I "
                + phoneMlf + " -S " + listFile + " " + phoneDict + " " + phoneList + " > log_hviteAligning.txt"
                + "; exit )\n");

        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

    }

    private void htkExtraModels() throws Exception {

        String hlstats = getProp(HTKDIR) + FSEP + "HLStats";
        String hbuild = getProp(HTKDIR) + FSEP + "HBuild";

        File htkFile = new File(hlstats);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getProp(HTDIR) + FSEP + "config" + FSEP + "htkTrain.conf";
        String bigFile = etcDir + "htk.phones.big";
        String phoneList = etcDir + "htk.phone.list";
        String phoneMlf = etcDir + "htk.phones.mlf";
        String phoneDict = etcDir + "htk.phone.dict";
        String phoneAugDict = etcDir + "htk.aug.phone.dict";
        String phoneAugList = etcDir + "htk.aug.phone.list";

        String netFile = etcDir + "htk.phones.net";

        Runtime rtime = Runtime.getRuntime();
        // get a shell
        Process process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( cd " + getProp(HTDIR) + "; " + hlstats + " -T 1 -C " + configFile + " -b " + bigFile
                + " -o " + phoneList + " " + phoneMlf + " > log_hlstats.txt" + "; exit )\n");

        pw.println("( cd " + getProp(HTDIR) + "; " + hlstats + " -T 1 -C " + configFile + " -b " + bigFile + " -o "
                + phoneList + " " + phoneMlf + " > log_hlstats.txt" + "; exit )\n");

        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

        String fileDict = FileUtils.getFileAsString(new File(phoneDict), "ASCII");
        PrintWriter augPhoneDict = new PrintWriter(new FileWriter(phoneAugDict));
        augPhoneDict.println("!ENTER sil");
        augPhoneDict.print(fileDict);
        augPhoneDict.println("!EXIT sil");
        augPhoneDict.flush();
        augPhoneDict.close();

        String fileList = FileUtils.getFileAsString(new File(phoneList), "ASCII");
        PrintWriter augPhoneList = new PrintWriter(new FileWriter(phoneAugList));
        augPhoneList.println("!ENTER");
        augPhoneList.print(fileList);
        augPhoneList.println("!EXIT");
        augPhoneList.flush();
        augPhoneList.close();

        rtime = Runtime.getRuntime();
        // get a shell
        process = rtime.exec("/bin/bash");
        // get an output stream to write to the shell
        pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        System.out.println("( cd " + getProp(HTDIR) + "; " + hbuild + " -T 1 -C " + configFile + " -n " + bigFile + " "
                + phoneAugList + " " + netFile + " > log_hbuild.txt" + "; exit )\n");

        pw.println("( cd " + getProp(HTDIR) + "; " + hbuild + " -T 1 -C " + configFile + " -n " + bigFile + " "
                + phoneAugList + " " + netFile + " > log_hbuild.txt" + "; exit )\n");

        pw.flush();
        // shut down
        pw.close();
        process.waitFor();
        // check exit value
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        }

    }

    /**
     * Create phone sequence file, which is used for Alignment
     * 
     * @throws Exception
     */

    private void getPhoneSequence() throws Exception {

        // open transcription file used for labeling
        PrintWriter transLabelOut = new PrintWriter(new FileOutputStream(new File(outputDir + "/" + "htk.phones.mlf")));
        PrintWriter transLabelOut1 = new PrintWriter(
                new FileOutputStream(new File(outputDir + "/" + "htk.phones2.mlf")));
        PrintWriter transLabelOut2 = new PrintWriter(
                new FileOutputStream(new File(outputDir + "/" + "htk.phones3.mlf")));

        String phoneSeq;
        transLabelOut.println("#!MLF!#");
        transLabelOut1.println("#!MLF!#");
        transLabelOut2.println("#!MLF!#");
        for (int i = 0; i < wavList.getLength(); i++) {
            transLabelOut.println("\"*/" + wavList.getName(i) + labExt + "\"");
            transLabelOut1.println("\"*/" + wavList.getName(i) + labExt + "\"");
            transLabelOut2.println("\"*/" + wavList.getName(i) + labExt + "\"");
            // phoneSeq = getSingleLine(bnl.getName(i));
            phoneSeq = getLineFromXML(wavList.getName(i), false, false);
            transLabelOut.println(phoneSeq.trim());
            phoneSeq = getLineFromXML(wavList.getName(i), true, false);
            transLabelOut1.println(phoneSeq.trim());
            phoneSeq = getLineFromXML(wavList.getName(i), true, true);
            transLabelOut2.println(phoneSeq.trim());

            // System.out.println( "    " + bnl.getName(i) );

        }
        transLabelOut.flush();
        transLabelOut.close();
        transLabelOut1.flush();
        transLabelOut1.close();
        transLabelOut2.flush();
        transLabelOut2.close();
    }

    private String getLineFromXML(String basename, boolean spause, boolean vpause) throws Exception {

        String line;
        String phoneSeq;
        Matcher matcher;
        Pattern pattern;
        StringBuilder alignBuff = new StringBuilder();
        // alignBuff.append(basename);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(getProp(PROMPTALLOPHONESDIR) + "/" + basename + xmlExt));
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList tokens = (NodeList) xpath.evaluate("//t | //boundary", doc, XPathConstants.NODESET);

        alignBuff.append(collectTranscription(tokens));
        phoneSeq = alignBuff.toString();
        pattern = Pattern.compile("pau ssil ");
        matcher = pattern.matcher(phoneSeq);
        phoneSeq = matcher.replaceAll("sil ");

        pattern = Pattern.compile(" ssil pau$");
        matcher = pattern.matcher(phoneSeq);
        phoneSeq = matcher.replaceAll(" sil");

        if (!vpause) {
            /*
             * TODO: Extra code need to write to maintain minimum number of
             * short sil. or consider word boundaries as ssil.
             */
            /*
             * virtual silence on word boundaries are matched in sp
             */
            pattern = Pattern.compile("vssil");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("");
        } else {
            /*
             * virtual silence on word boundaries are matched in sp
             */
            pattern = Pattern.compile("vssil");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("sp");
        }

        // checking
        if (!spause) {
            pattern = Pattern.compile("ssil");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("");
        }

        phoneSeq += " .";

        pattern = Pattern.compile("\\s+");
        matcher = pattern.matcher(phoneSeq);
        phoneSeq = matcher.replaceAll("\n");

        // System.out.println(phoneSeq);
        return phoneSeq;
    }

    private String collectTranscription(NodeList tokens) {

        // TODO: make delims argument
        // String Tokenizer devides transcriptions into syllables
        // syllable delimiters and stress symbols are retained
        String delims = "',-";

        // String storing the original transcription begins with a pause
        String orig = " pau ";

        // get original phone String
        for (int tNr = 0; tNr < tokens.getLength(); tNr++) {

            Element token = (Element) tokens.item(tNr);

            // only look at it if there is a sampa to change
            if (token.hasAttribute("ph")) {

                String sampa = token.getAttribute("ph");

                List<String> sylsAndDelims = new ArrayList<String>();
                StringTokenizer sTok = new StringTokenizer(sampa, delims, true);

                while (sTok.hasMoreElements()) {
                    String currTok = sTok.nextToken();

                    if (delims.indexOf(currTok) == -1) {
                        // current Token is no delimiter
                        for (PhoneEl ph : allophoneSet.splitIntoPhones(currTok)) {
                            // orig += ph.name() + " ";
                            if (ph.name().trim().equals("_"))
                                continue;
                            orig += replaceTrickyPhones(ph.name().trim()) + " ";
                        }// ... for each phone
                    }// ... if no delimiter
                }// ... while there are more tokens
            }

            // TODO: simplify
            if (token.getTagName().equals("t")) {

                // if the following element is no boundary, insert a non-pause
                // delimiter
                if (tNr == tokens.getLength() - 1 || !((Element) tokens.item(tNr + 1)).getTagName().equals("boundary")) {
                    orig += "vssil "; // word boundary

                }

            } else if (token.getTagName().equals("boundary")) {

                orig += "ssil "; // phrase boundary

            } else {
                // should be "t" or "boundary" elements
                assert (false);
            }

        }// ... for each t-Element
        orig += "pau";
        return orig;
    }

    private void convertSingleLabelFile(String basename) throws Exception {

        String line;
        String previous, current;
        String regexp = "\\spau|\\sssil";

        // Compile regular expression
        Pattern pattern = Pattern.compile(regexp);

        File labDir = new File(getProp(OUTLABDIR));
        if (!labDir.exists()) {
            labDir.mkdir();
        }

        File labelFile = new File(getProp(HTDIR) + FSEP + "tmplab" + FSEP + basename + labExt);
        if (!labelFile.exists()) {
            System.err.println("WARNING: " + basename + " label file not created with HTK.");
            return;
        }

        BufferedReader labelIn = new BufferedReader(new InputStreamReader(new FileInputStream(labelFile)));

        PrintWriter labelOut = new PrintWriter(new FileOutputStream(new File(labDir + "/" + basename + labExt)));

        previous = labelIn.readLine();

        while ((line = labelIn.readLine()) != null) {

            // Replace all occurrences of pattern in input
            Matcher matcher = pattern.matcher(line);
            current = matcher.replaceAll(" _");

            if (previous.endsWith("_") && current.endsWith("_")) {
                previous = current;
                continue;
            }

            labelOut.println(previous);
            previous = current;

        }

        labelOut.println(previous);
        labelOut.flush();
        labelOut.close();
        labelIn.close();

    }

    private void getProperLabelFormat() throws Exception {
        String alignedMlf = getProp(HTDIR) + FSEP + "aligned.mlf";
        BufferedReader htkLab = new BufferedReader(new FileReader(alignedMlf));
        // File labDir = new File(getProp(OUTLABDIR));
        File labDir = new File(getProp(HTDIR) + FSEP + "tmplab");
        if (!labDir.exists())
            labDir.mkdir();

        String header = htkLab.readLine().trim();
        if (!header.equals("#!MLF!#")) {
            System.err.println("Header format not supported");
            throw new RuntimeException("Header format not supported");
        }
        String line;
        while ((line = htkLab.readLine()) != null) {
            line = line.trim();
            String MLFfileName = line.substring(1, line.length() - 1);
            String Basename = new File(MLFfileName).getName();
            System.out.println("Basename: " + Basename);
            String fileName = labDir.getCanonicalPath() + FSEP + Basename;

            // line.replaceAll("\"", "");
            // System.err.println("LINE: "+fileName);

            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            pw.println("#");
            while (true) {
                String nline = htkLab.readLine().trim();
                if (nline.equals("."))
                    break;
                StringTokenizer st = new StringTokenizer(nline);
                Double tStart = Double.parseDouble(st.nextToken().trim());
                Double tStamp = Double.parseDouble(st.nextToken().trim());
                String phoneSeg = replaceBackTrickyPhones(st.nextToken().trim());

                // System.out.println( "start " + tStart + " stop " + tStamp +
                // " " + phoneSeg);
                Double dur = tStamp - tStart;
                Double durms = dur / 10000;
                if (phoneSeg.equals("sp")) {
                    if (dur == 0) {
                        // System.out.println("sp to delete!!!");
                        continue;
                    }

                    /*
                     * else if (dur <= 150000) //150000 = 15 ms { //TODO: A
                     * better post processing should be done: i.e. check the
                     * previous and the next phone ...
                     * System.out.println("sp <= 15 ms to delete!!!"); continue;
                     * }
                     */
                    else {
                        System.out.println(fileName + ": a sp (virtual) pause with duration: " + durms
                                + " ms, has been detected at " + tStart + " " + tStamp);
                        /*
                         * The following gawk lines cab be used to inspect very
                         * long sp pause: gawk 'match($0, /^(.*): a
                         * sp.*duration: ([0-9]+\.[0-9]+) ms.*$/, arr) {if
                         * (arr[2]>200) {print "file:" arr[1] " duration:"
                         * arr[2]} }' nohup.out gawk 'match($0, /^(.*): a
                         * sp.*duration: ([0-9]+\.[0-9]+) ms.*$/, arr) {if
                         * (arr[2]>400) {print $0} }' nohup.out
                         */

                    }

                }

                if (phoneSeg.equals("sil") || phoneSeg.equals("ssil") || phoneSeg.equals("sp"))
                    phoneSeg = "_";

                pw.println(tStamp / 10000000 + " 125 " + phoneSeg);
            }

            pw.flush();
            pw.close();
        }

        for (int i = 0; i < wavList.getLength(); i++) {

            convertSingleLabelFile(wavList.getName(i));
            // System.out.println( "    " + bnl.getName(i) );

        }
    }

    public static String getMaryXMLHeaderWithInitialBoundary(String locale) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<maryxml version=\"0.4\"\n"
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" + "xml:lang=\"" + locale + "\">\n"
                + "<boundary duration=\"100\"/>\n";

    }

    public int getProgress() {
        return progress;
    }

    public String replaceTrickyPhones(String lab) {
        String s = lab;

        /** the replace is done for the labels: phone, prev_phone and next_phone */

        /** DE (replacements in German phone set) */
        if (lab.contentEquals("6"))
            s = "ER6";
        else if (lab.contentEquals("=6"))
            s = "ER66";
        else if (lab.contentEquals("2:"))
            s = "EU22";
        else if (lab.contentEquals("2"))
            s = "EU2";
        else if (lab.contentEquals("9"))
            s = "EU9";
        else if (lab.contentEquals("9~"))
            s = "UM9";
        else if (lab.contentEquals("e~"))
            s = "IMe";
        else if (lab.contentEquals("a~"))
            s = "ANa";
        else if (lab.contentEquals("o~"))
            s = "ONo";
        else if (lab.contentEquals("?"))
            s = "gstop";
        /** EN (replacements in English phone set) */
        // else if (lab.contentEquals("r=") )
        // s = "rr";

        return s;

    }

    public String replaceBackTrickyPhones(String lab) {
        String s = lab;
        /** DE (replacements in German phone set) */
        if (lab.contentEquals("ER6"))
            s = "6";
        else if (lab.contentEquals("ER66")) /* CHECK ??? */
            s = "=6";
        else if (lab.contentEquals("EU2"))
            s = "2";
        else if (lab.contentEquals("EU22"))
            s = "2:";
        else if (lab.contentEquals("EU9"))
            s = "9";
        else if (lab.contentEquals("UM9"))
            s = "9~";
        else if (lab.contentEquals("IMe"))
            s = "e~";
        else if (lab.contentEquals("ANa"))
            s = "a~";
        else if (lab.contentEquals("ONo"))
            s = "o~";
        else if (lab.contentEquals("gstop"))
            s = "?";
        /** EN (replacements in English phone set) */
        // else if (lab.contentEquals("rr") )
        // s = "r=";

        // System.out.println("LAB=" + s);

        return s;

    }

}
