package hmi.synth.voc.train;

import hmi.annotate.SpeechMarkupAnnotater;
import hmi.data.*;
import hmi.phone.PhoneSet;
import hmi.util.FileList;
import hmi.util.FileUtils;
import hmi.util.Resource;
import hmi.util.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BAlign {
    private boolean DEBUG = true; // TODO add logger

    private String htkBinDir = "//";
    private VoiceRoot root;

    private String outputDir;
    protected String labExt = ".lab";

    protected int MAX_ITERATIONS = 150;
    protected int MAX_SP_ITERATION = 10;
    protected int MAX_VP_ITERATION = 20;
    protected int MAX_MIX_ITERATION = 30;

    protected int noIterCompleted = 0;

    private String htkStandardOptions = "-A -D -V -T 1"; // Main HTK standard Options htkStandardOptions
    private String Extract_FEAT = "MFCC_0"; // MFCC_E
    private String Train_FEAT = "MFCC_0_D_A"; // MFCC_E_D_A
    // 13; //13 without D_A; 13*3 with D_A
    private int Train_VECTSIZE = 13 * 3;
    private int NUMStates = 5;
    private int[] num_mixtures_for_state = {2, 1, 2};
    private int[] current_number_of_mixtures = {1, 1, 1};

    private ArrayList<Double> logProbFrame_array = new ArrayList<Double>();
    private ArrayList<Double> epsilon_array = new ArrayList<Double>();
    private int PHASE_NUMBER = 0;
    private double[] epsilon_PHASE = {0.2, 0.05, 0.001, 0.0005}; // 0 1 2 3


    public BAlign(String htkBinDir, String dataDir) throws Exception {
        this.htkBinDir = htkBinDir;
        this.root = new VoiceRoot(dataDir);
        File htkFile = new File(getHTKBinDir() + File.separator + "HInit");
        if (!htkFile.exists()) {
            throw new Exception("HTK path setting is wrong. Because file " + htkFile.getAbsolutePath()
                    + " does not exist");
        }
    }


    public boolean compute(Set<String> phones) throws Exception {
        outputDir = getHTKPath("etc");

        System.out.println("Preparing voice database for labelling using HTK :");
        System.out.println("Setting up HTK directories ...");
        new File(getHTKDataDir()).mkdir();
        Command.bash("( cd " + getHTKDataDir() + "; mkdir -p hmm; mkdir -p etc; mkdir -p feat; "
                + "mkdir -p config; mkdir -p lab; mkdir -p logs; exit )\n");

        createRequiredFiles();
        createPhoneDictionary(phones);
        getPhoneSequence();

        // remove multiple sp
        for (int i = 3; i < 8; i++) {
            int x = i + 1;
            if (x > 7)
                x = 3;
            delete_multiple_sp_in_PhoneMLFile(
                    getHTKPath("etc", "htk.phones" + i + ".mlf"),
                    getHTKPath("etc", "htk.phones" + x + ".mlf"));
        }

        // part 2: Feature Extraction using HCopy
        System.out.println("Feature Extraction:");
        featureExtraction();

        // Part 3: Flat-start initialization
        System.out.println("HTK Training:");
        initializeHTKTrain();
        createTrainFile();

        // Part 4: HERest training
        System.out.println("HERest Training:");
        herestTraining();

        // Part 5: Force align with HVite
        System.out.println("HTK Align:");
        hviteAligning();

        // Part 6: Extra model statistics
        System.out.println("Generating Extra model statistics...");
        htkExtraModels();
        convertLabels();

        return true;
    }

    // etc/htk.phone.dict
    // etc/htk.phone.list
    private void createPhoneDictionary(Set<String> phonesList) throws Exception {
        PrintWriter transLabelOut = getPW(getHTKPath("etc", "htk.phone.dict"));
        PrintWriter phoneListOut = getPW(getHTKPath("etc", "htk.phone.list"));
        PrintWriter phoneListOut1 = getPW(getHTKPath("etc", "htk.phone2.list"));
        PrintWriter phoneListOut2 = getPW(getHTKPath("etc", "htk.phone3.list"));

        // transLabelOut.println("#!MLF!#");
        Iterator<String> it = phonesList.iterator();
        while (it.hasNext()) {
            // System.out.println(it.next());
            String phon = it.next();

            if (phon.equals("_")) {
                continue;
                // phon = "sp";
            }
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

        transLabelOut.close();
        phoneListOut.close();
        phoneListOut1.close();
        phoneListOut2.close();
    }

    // config/mkphone.led
    // config/featEx.conf
    // config/htkTrain.conf
    // etc/featEx.list
    // etc/htkTrain.list
    // config/proto
    // config/sil.hed
    // config/sil_vp.hed
    private void createRequiredFiles() throws Exception {
        PrintWriter pw = getPW(getHTKPath("config", "mkphone0.led"));
        pw.println("EX");
        pw.println("IS sil sil");
        // pw.println("DE sp"); // Short pause modeling?
        pw.close();

        pw = getPW(getHTKPath("config", "mkphone1.led"));
        pw.println("ME sp sp sp");
        pw.println("ME sil sil sp");
        pw.println("ME sil sp sil");
        pw.println("ME ssil ssil sp");
        pw.println("ME ssil sp ssil");
        pw.close();

        pw = getPW(getHTKPath("config", "featEx.conf"));
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
        pw.close();

        pw = getPW(getHTKPath("config", "htkTrain.conf"));
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
        pw.close();

        pw = getPW(getHTKPath("etc", "featEx.list"));
        for (int i = 0; i < files().length(); i++) {
            String input = joinPath(getWavDir(), files().name(i) + ".wav");
            String output = getHTKPath("feat", files().name(i) + ".mfcc");
            pw.println(input + " " + output);
        }
        pw.close();

        pw = getPW(getHTKPath("etc", "htkTrain.list"));
        for (int i = 0; i < files().length(); i++) {
            String mFile = getHTKPath("feat", files().name(i) + ".mfcc");
            pw.println(mFile);
        }
        pw.close();

        // creating a hmm protofile
        int vectorSize = Train_VECTSIZE;
        int numStates = NUMStates;

        if (num_mixtures_for_state.length != numStates - 2) {
            throw new RuntimeException("Mixture num_mixtures_for_state lenght does not correspond to numStates");
        }

        pw = getPW(getHTKPath("config", "htk.proto"));
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
        pw.println("<TransP> " + numStates);
        pw.println("0.0 1.0 0.0 0.0 0.0");
        pw.println("0.0 0.6 0.4 0.0 0.0");
        pw.println("0.0 0.0 0.6 0.4 0.0");
        pw.println("0.0 0.0 0.0 0.7 0.3");
        pw.println("0.0 0.0 0.0 0.0 1.0");
        pw.println("<EndHMM>");
        pw.close();

        pw = getPW(getHTKPath("config", "sil.hed"));
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
        pw.close();

        // Creating SP Silence modeling config file
        pw = getPW(getHTKPath("config", "sil_vp.hed"));
        // sp 3 state case:
        // pw.println("AT 1 3 0.3 {sp.transP}");
        // pw.println("TI ssilst {ssil.state[3],sp.state[2]}");
        // sp 5 state case:
        pw.println("AT 1 5 0.3 {sp.transP}");
        pw.println("TI ssilst2 {ssil.state[2],sp.state[2]}");
        pw.println("TI ssilst3 {ssil.state[3],sp.state[3]}");
        pw.println("TI ssilst4 {ssil.state[4],sp.state[4]}");
        pw.close();
    }

    private void delete_multiple_sp_in_PhoneMLFile(String filein, String fileout) throws Exception {
        String hled = getHTKBinDir() + File.separator + "HLEd";
        String mkphoneLED = getHTKPath("config", "mkphone1.led");
        Command.bash("( " + hled + " -l '*' -i " + fileout + " " + mkphoneLED + " " + filein + "; exit )\n");
    }

    private void featureExtraction() throws Exception {
        String hcopy = getHTKBinDir() + File.separator + "HCopy";
        String configFile = getHTKPath("config", "featEx.conf");
        String listFile = getHTKPath("etc", "featEx.list");
        Command.bash("( cd " + getHTKDataDir() + "; " + hcopy + " -T 1 -C " + configFile + " -S " + listFile
                + " > logs/featureExtraction.txt" + "; exit )\n");
    }

    private void initializeHTKTrain() throws Exception {
        String HCompV = getHTKBinDir() + File.separator + "HCompV";
        String configFile = getHTKPath("config", "htkTrain.conf");
        String listFile = getHTKPath("etc", "htkTrain.list");
        Command.bash("( cd " + getHTKDataDir() + " ; mkdir hmm/hmm-dummy ; " + " mkdir hmm/hmm-final ; " + HCompV + " "
                + htkStandardOptions + " -C " + configFile + " -f 0.01 -m -S " + listFile + " -M " + getHTKPath("hmm", "hmm-dummy")
                + " " + getHTKPath("config", "htk.proto") + " > logs/initialiseHTKTrain.txt" + "; exit )\n");
    }

    // etc/htkTrainScript.sh
    // hmm/hmm0/macros
    // hmm/hmm-dummy/vFloors
    private void createTrainFile() throws Exception {
        PrintWriter pw = getPW(getHTKPath("etc", "htkTrainScript.sh"));
        pw.println("mkdir hmm/hmm0\n" + "head -3 hmm/hmm-dummy/htk > hmm/hmm0/hmmdefs\n"
                + "for s in `cat etc/htk.phone.list`\n" + "do\n" + "echo \"~h \\\"$s\\\"\" >> hmm/hmm0/hmmdefs\n"
                + getAwkBinPath() + " '/BEGINHMM/,/ENDHMM/ { print $0 }' hmm/hmm-dummy/htk >> hmm/hmm0/hmmdefs\n"
                + "done\n");
        pw.close();
        Command.bash("( cd " + getHTKDataDir() + "; sh etc/htkTrainScript.sh" + " > logs/htkTrainScript.txt" + "; exit )\n");

        PrintWriter macroFile = getPW(getHTKPath("hmm", "hmm0", "macros"));
        macroFile.println("~o\n" + "<VecSize> 13\n" + "<" + Train_FEAT + ">");
        macroFile.println(FileUtils.getFileAsString(new File(getHTKPath("hmm", "hmm-dummy", "vFloors")), "ASCII"));
        macroFile.close();
    }

    private void herestTraining() throws Exception {

        String HERest = getHTKBinDir() + File.separator + "HERest";
        String HHEd = getHTKBinDir() + File.separator + "HHEd";

        String configFile = getHTKPath("config", "htkTrain.conf");
        String hhedconf = getHTKPath("config", "sil.hed");
        String hhedconf_vp = getHTKPath("config", "sil_vp.hed");
        String trainList = getHTKPath("etc", "htkTrain.list");
        String phoneList = getHTKPath("etc", "htk.phone.list");
        String phoneMlf = getHTKPath("etc", "htk.phones.mlf");

        int BEST_ITERATION = MAX_ITERATIONS;
        int SP_ITERATION = -1;
        int VP_ITERATION = -1;
        int change_mix_iteration = -1;
        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            System.out.println("Iteration number: " + iteration + "/" + PHASE_NUMBER);
            String hmm0last = "hmm" + (iteration - 1);
            String hmm1now = "hmm" + iteration;

            File hmmItDir = new File(getHTKPath("hmm", hmm1now));
            if (!hmmItDir.exists())
                hmmItDir.mkdir();

            if (PHASE_NUMBER == 0) {

                if (iteration == (SP_ITERATION + 1)) {
                    phoneMlf = getHTKPath("etc", "htk.phones2.mlf");
                    phoneList = getHTKPath("etc", "htk.phone2.list");
                    Command.bash("( cd " + getHTKDataDir() + "; " + HHEd + " " + htkStandardOptions + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf + " " + phoneList
                            + " >> logs/herestTraining_" + iteration + ".txt" + "; exit )\n");
                    // copy of logProbFrame_array in current iteration
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);

                    // PHASE 1
                    PHASE_NUMBER = 1;
                    System.out.println("PHASE -" + PHASE_NUMBER);
                    continue;
                }

                if (iteration > 2) {
                    if (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER] || iteration == MAX_SP_ITERATION) {
                        SP_ITERATION = iteration;
                        insertShortPause(iteration);
                        String oldMacro = getHTKPath("hmm", hmm0last, "macros");
                        String newMacro = getHTKPath("hmm", hmm1now, "macros");
                        FileUtils.copy(oldMacro, newMacro);
                        logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                        epsilon_array.add(100000000.0);
                        continue;
                    }
                }
            }

            if (PHASE_NUMBER == 1) {
                if (iteration == (VP_ITERATION + 1)) {
                    phoneMlf = getHTKPath("etc", "htk.phones3.mlf");
                    phoneList = getHTKPath("etc", "htk.phone3.list");
                    Command.bash("( cd " + getHTKDataDir() + "; " + HHEd + " " + htkStandardOptions + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf_vp + " " + phoneList
                            + " >> logs/herestTraining_" + iteration + ".txt" + "; exit )\n");
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);
                    // PHASE 2
                    PHASE_NUMBER = 2;
                    System.out.println("PHASE - " + PHASE_NUMBER);
                    continue;
                }

                if (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER] || iteration == MAX_VP_ITERATION) {
                    VP_ITERATION = iteration;
                    insertVirtualPauseThreeStates(iteration);
                    String oldMacro = getHTKPath("hmm", hmm0last, "macros");
                    String newMacro = getHTKPath("hmm", hmm1now, "macros");
                    FileUtils.copy(oldMacro, newMacro);
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);
                    continue;
                }
            }

            // /-----------------
            if (PHASE_NUMBER == 2) {
                // check epsilon_array
                // the following change_mix_iteration + 2 is used to allow more
                // than one re-estimation after insertion of new mixture because
                // just after the insertion the delta can be negative

                if (((iteration != change_mix_iteration + 2) && (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER]))
                        || iteration == MAX_MIX_ITERATION) {
                    change_mix_iteration = iteration;
                    MAX_MIX_ITERATION = -1;

                    // Creating Increasing mixture config file dynamic iteration
                    String hhedconf_mix = getHTKPath("config", "sil_mix_" + iteration + ".hed");
                    PrintWriter hhed_conf_pw = getPW(hhedconf_mix);

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
                        // PHASE 3
                        PHASE_NUMBER = 3;
                        System.out.println("PHASE:" + PHASE_NUMBER);
                        // continue;
                    }
                    hhed_conf_pw.close();

                    Command.bash("( cd " + getHTKDataDir() + "; " + HHEd + " " + htkStandardOptions + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf_mix + " " + phoneList
                            + " >> logs/herestTraining_" + iteration + ".txt" + "; exit )\n");
                    logProbFrame_array.add(logProbFrame_array.get(iteration - 2));
                    epsilon_array.add(100000000.0);
                    continue;
                }
            }

            // /-----------------
            if (PHASE_NUMBER == 3) {
                if (((iteration != change_mix_iteration + 2) && (epsilon_array.get(iteration - 2) < epsilon_PHASE[PHASE_NUMBER]))
                        || iteration == MAX_ITERATIONS) {
                    int last = iteration - 1;
                    int prior = iteration - 2;
                    System.out.println("Average log prob per frame has not increased much.");
                    System.out.println("   at HREST iteration (" + last + ") - "
                            + logProbFrame_array.get(iteration - 2));
                    System.out.println("   at REST iteration (" + prior + ") - "
                            + logProbFrame_array.get(iteration - 3));
                    System.out.println("Delta - " + epsilon_array.get(iteration - 2));
                    if (logProbFrame_array.get(iteration - 3) > logProbFrame_array.get(iteration - 2)) {
                        BEST_ITERATION = iteration - 2;
                    } else {
                        BEST_ITERATION = iteration - 1;
                    }
                    break;
                }
            }

            // Normal HEREST
            Command.bash("( cd " + getHTKDataDir() + "; " + HERest + " " + htkStandardOptions + " -C " + configFile + " -I " + phoneMlf
                    + " -t 250.0 150.0 1000.0" + " -S " + trainList + " -H " + getHTKPath("hmm", hmm0last, "macros")
                    + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs") + " -M " + getHTKPath("hmm", hmm1now) + " "
                    + phoneList + " >> logs/herestTraining_" + iteration + ".txt" + "; exit )\n");

            check_average_log_prob_per_frame(iteration);

            System.out.println("Delta average log prob per frame to respect prior iteration - "
                    + epsilon_array.get(iteration - 1));
            System.out.println("Current PHASE - " + PHASE_NUMBER);
            System.out.println("Current state and number of mixtures (for each phoneme) - "
                    + Arrays.toString(current_number_of_mixtures));

            System.out.println("---------------------------------------");
        }

        System.out.println("***********\n");
        System.out.println("BEST ITERATION: " + BEST_ITERATION);
        System.out.println("COPYNING BEST ITERATION FILES IN hmm-final directory");
        System.out.println("logProbFrame_array:" + logProbFrame_array.toString());
        System.out.println("epsilon_array:" + epsilon_array.toString());
        System.out.println("***********\n");

        String oldMacro = getHTKPath("hmm", "hmm" + BEST_ITERATION, "macros");
        String newMacro = getHTKPath("hmm", "hmm-final", "macros");
        FileUtils.copy(oldMacro, newMacro);

        String oldHmmdefs = getHTKPath("hmm", "hmm" + BEST_ITERATION, "hmmdefs");
        String newHmmdefs = getHTKPath("hmm", "hmm-final", "hmmdefs");
        FileUtils.copy(oldHmmdefs, newHmmdefs);

    }

    private void check_average_log_prob_per_frame(int iteration) throws IOException {
        String filename = getHTKPath("logs/herestTraining_" + iteration + ".txt");

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
                System.out.println("Average log prob per frame at iteration " + iteration + " is " + m.group(1)
                        + " equal to " + logProbFrame);
                found = true;
                break;
            }
        }
        reader.close();
        System.out.flush();
        if (!found) {
            throw new RuntimeException("No match of average log prob per frame in " + filename);
        }

        double delta;
        if (iteration > 1)
            delta = logProbFrame_array.get(iteration - 1) - logProbFrame_array.get(iteration - 2);
        else
            delta = 10000000.0;
        epsilon_array.add(delta);
    }

    private void insertShortPause(int i) throws Exception {
        boolean okprint = false;
        // boolean silprint = false;
        String pdef = getHTKPath("hmm", "hmm" + (i - 1), "hmmdefs");
        String def = getHTKPath("hmm", "hmm" + i, "hmmdefs");
        String line, spHmmDef = "";
        BufferedReader hmmDef = new BufferedReader(new FileReader(pdef));
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
        hmmDef = new BufferedReader(new FileReader(pdef));
        PrintWriter newHmmDef = new PrintWriter(new FileWriter(def));
        while ((line = hmmDef.readLine()) != null) {
            newHmmDef.println(line.trim());
        }
        newHmmDef.println(spHmmDef);
        newHmmDef.close();
        hmmDef.close();

    }

    private void insertVirtualPauseThreeStates(int i) throws Exception {
        boolean okprint = false;
        boolean okprint2 = false;
        String pdef = getHTKPath("hmm", "hmm" + (i - 1), "hmmdefs");
        String def = getHTKPath("hmm", "hmm" + i, "hmmdefs");
        String line, spHmmDef = "";
        BufferedReader hmmDef = new BufferedReader(new FileReader(pdef));
        while ((line = hmmDef.readLine()) != null) {
            if (line.matches("^.*\"ssil\".*$")) {
                okprint = true;
                spHmmDef += "~h \"sp\"\n";
                spHmmDef += "<BeginHMM>\n";
                spHmmDef += "<NumStates> 5\n";
                spHmmDef += "<State> 2\n";
                continue;
            }
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

        hmmDef = new BufferedReader(new FileReader(pdef));
        PrintWriter newHmmDef = new PrintWriter(new FileWriter(def));
        while ((line = hmmDef.readLine()) != null) {
            newHmmDef.println(line.trim());
        }
        newHmmDef.println(spHmmDef);
        newHmmDef.close();
        hmmDef.close();
    }

    private void hviteAligning() throws Exception {

        String hvite = getHTKBinDir() + File.separator + "HVite";
        // -A -D -V -T 1 "; // to add -A -D -V -T 1 in every function
        File htkFile = new File(hvite);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getHTKPath("config", "htkTrain.conf");
        String listFile = getHTKPath("etc", "htkTrain.list");

        // String phoneList = getHTKPath("etc","htk.phone2.list"); // without sp
        String phoneList = getHTKPath("etc", "htk.phone3.list");
        String hmmDef = getHTKPath("hmm", "hmm-final", "hmmdefs");
        String macros = getHTKPath("hmm", "hmm-final", "macros");

        // String phoneMlf = getHTKPath("etc", "htk.phone2.mlf"); // without sp
        String phoneMlf = getHTKPath("etc", "htk.phones3.mlf");
        String alignedMlf = getHTKPath("aligned.mlf");
        String phoneDict = getHTKPath("etc", "htk.phone.dict");
        String labDir = getHTKPath("lab");

        Command.bash("( cd " + getHTKDataDir() + "; " + hvite + " " + htkStandardOptions + " -b sil -l " + labDir + " -o W -C "
                + configFile + " -a -H " + macros + " -H " + hmmDef + " -i " + alignedMlf + " -t 250.0 -y lab" + " -I "
                + phoneMlf + " -S " + listFile + " " + phoneDict + " " + phoneList + " > logs/hviteAligning.txt"
                + "; exit )\n");
    }

    private void htkExtraModels() throws Exception {

        String hlstats = getHTKBinDir() + File.separator + "HLStats";
        String hbuild = getHTKBinDir() + File.separator + "HBuild";

        File htkFile = new File(hlstats);
        if (!htkFile.exists()) {
            throw new RuntimeException("File " + htkFile.getAbsolutePath() + " does not exist");
        }
        String configFile = getHTKPath("config", "htkTrain.conf");
        String bigFile = getHTKPath("etc", "htk.phones.big");
        String phoneList = getHTKPath("etc", "htk.phone.list");
        String phoneMlf = getHTKPath("etc", "htk.phones.mlf");
        String phoneDict = getHTKPath("etc", "htk.phone.dict");
        String phoneAugDict = getHTKPath("etc", "htk.aug.phone.dict");
        String phoneAugList = getHTKPath("etc", "htk.aug.phone.list");
        String netFile = getHTKPath("etc", "htk.phones.net");

        Command.bash("( cd " + getHTKDataDir() + "; " + hlstats + " -T 1 -C " + configFile + " -b " + bigFile + " -o "
                + phoneList + " " + phoneMlf + " > logs/hlstats.txt" + "; exit )\n");

        String fileDict = FileUtils.getFileAsString(new File(phoneDict), "ASCII");
        PrintWriter augPhoneDict = new PrintWriter(new FileWriter(phoneAugDict));
        augPhoneDict.println("!ENTER sil");
        augPhoneDict.print(fileDict);
        augPhoneDict.println("!EXIT sil");
        augPhoneDict.close();

        String fileList = FileUtils.getFileAsString(new File(phoneList), "ASCII");
        PrintWriter augPhoneList = new PrintWriter(new FileWriter(phoneAugList));
        augPhoneList.println("!ENTER");
        augPhoneList.print(fileList);
        augPhoneList.println("!EXIT");
        augPhoneList.close();

        Command.bash("( cd " + getHTKDataDir() + "; " + hbuild + " -T 1 -C " + configFile + " -n " + bigFile + " "
                + phoneAugList + " " + netFile + " > logs/hbuild.txt" + "; exit )\n");

    }

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
        for (int i = 0; i < files().length(); i++) {
            transLabelOut.println("\"*/" + files().name(i) + labExt + "\"");
            transLabelOut1.println("\"*/" + files().name(i) + labExt + "\"");
            transLabelOut2.println("\"*/" + files().name(i) + labExt + "\"");

            phoneSeq = getLineFromSM(files().name(i), false, false);
            transLabelOut.println(phoneSeq.trim());
            phoneSeq = getLineFromSM(files().name(i), true, false);
            transLabelOut1.println(phoneSeq.trim());
            phoneSeq = getLineFromSM(files().name(i), true, true);
            transLabelOut2.println(phoneSeq.trim());

        }
        transLabelOut.close();
        transLabelOut1.close();
        transLabelOut2.close();
    }

    private String getLineFromSM(String filename, boolean spause, boolean vpause) throws Exception {

        SpeechMarkup sm = new SpeechMarkup();
        sm.readJSON(getSmDir() + "/" + filename + ".json");

        StringBuilder b = new StringBuilder();
        b.append("sil\n");
        for (Sentence sentence : sm.getSentences()) {
            for (Phrase ph : sentence.getPhrases()) {
                for (Word w : ph.getWords()) {
                    for (Syllable s : w.getSyllables()) {
                        for (Phone phone : s.getPhones()) {
                            b.append(phone.getPhone());
                            b.append("\n");
                        }
                    }
                    if (vpause)
                        b.append("sp\n");
                }
                if (spause)
                    b.append("ssil\n");
            }
        }
        b.append("sil\n");
        b.append(".");

        String s = b.toString();
        s = s.replaceAll("sp\nssil\n", "ssil\n");
        s = s.replaceAll("ssil\nsil\n", "sil\n");

        return s;
    }

    private void convertLabels() throws Exception {
        String alignedMlf = getHTKPath("aligned.mlf");
        BufferedReader htkLab = new BufferedReader(new FileReader(alignedMlf));
        File labDir = new File(getHTKPath("tmplab"));
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
            String fileName = labDir.getCanonicalPath() + File.separator + Basename;

            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            pw.println("#");
            while (true) {
                String nline = htkLab.readLine().trim();
                if (nline.equals("."))
                    break;
                StringTokenizer st = new StringTokenizer(nline);
                Double tStart = Double.parseDouble(st.nextToken().trim());
                Double tStamp = Double.parseDouble(st.nextToken().trim());
                String phoneSeg = st.nextToken().trim();

                Double dur = tStamp - tStart;
                Double durms = dur / 10000;
                if (phoneSeg.equals("sp")) {
                    if (dur == 0) {
                        // System.out.println("sp to delete!!!");
                        continue;
                    }

					/*
                     * else if (dur <= 150000) //150000 = 15 ms { //TODO: A better post Command.bashing should be done: i.e. check the
					 * previous and the next phone ... System.out.println("sp <= 15 ms to delete!!!"); continue; }
					 */
                    else {
                        System.out.println(fileName + ": a sp (virtual) pause with duration: " + durms
                                + " ms, has been detected at " + tStart + " " + tStamp);
                        /*
                         * The following gawk lines cab be used to inspect very long sp pause: gawk 'match($0, /^(.*): a
						 * sp.*duration: ([0-9]+\.[0-9]+) ms.*$/, arr) {if (arr[2]>200) {print "file:" arr[1] " duration:" arr[2]}
						 * }' nohup.out gawk 'match($0, /^(.*): a sp.*duration: ([0-9]+\.[0-9]+) ms.*$/, arr) {if (arr[2]>400)
						 * {print $0} }' nohup.out
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

    }

    public void copyToSpeechMarkup() {
        int s = files().length();
        for (int i = 0; i < 1; i++) {
            String smjson = root.path("sm", files().name(i) + ".json");
            SpeechMarkup sm = new SpeechMarkup();
            sm.readJSON(smjson);
            List<Segment> segs = sm.getSegments();
            int segi = 0;
            String o = root.path("htk", "tmplab", files().name(i) + ".lab");
            try {
                String fs = FileUtils.getFile(new File(o));
                String[] lines = fs.split("\n");
                for (int c = 1; c < lines.length; c++) {
                    String[] line = lines[c].split(" ");
                    float t = Float.parseFloat(line[0]);
                    String phstr = line[2];
                    float duration = 0, nt = 0;
                    if (c != lines.length - 1) {
                        String[] nextline = lines[c + 1].split(" ");
                        nt = Float.parseFloat(nextline[0]);
                        duration = nt - t;
                    }
                    if (segi < segs.size()) {
                        Segment seg = segs.get(segi);
                        if (seg instanceof Phone) {
                            Phone ph = (Phone) seg;
                            if (ph.getPhone().equals(phstr)) {
                                ph.setBegin(t);
                                ph.setEnd(nt);
                                ph.setDuration(duration);
                                segi++;
                            }
                        } else {
                            Boundary b = (Boundary) seg;
                            if (phstr.equals("_")) {
                                b.setDuration(duration);
                                b.setBegin(t);
                                b.setEnd(nt);
                                segi++;
                            }
                        }
                    }
                }
                sm.writeJSON(smjson);
            } catch (IOException e) {
                System.err.println("COULDNT READ " + o);
            }
        }
    }

    private String getAwkBinPath() {
        return "/usr/bin/awk";
    }

    private String getHTKBinDir() {
        return htkBinDir;
    }

    private String getHTKDataDir() {
        return root.path("htk");
    }

    private String getWavDir() {
        return root.path("wav");
    }

    private String getSmDir() {
        return root.path("sm");
    }

    private FileList files() {
        return root.wavFiles();
    }

    private String getHTKPath(String dir) {
        return joinPath(getHTKDataDir(), dir);
    }

    private String getHTKPath(String dir, String file) {
        return joinPath(getHTKDataDir(), dir, file);
    }

    private String getHTKPath(String dir, String cdir, String file) {
        return joinPath(getHTKDataDir(), dir, cdir, file);
    }

    private String joinPath(String... path) {
        StringBuilder b = new StringBuilder();
        for (String s : path) {
            b.append(s);
            b.append(File.separator);
        }
        return b.substring(0, b.length() - 1);
    }

    private PrintWriter getPW(String f) throws FileNotFoundException {
        return new PrintWriter(new FileOutputStream(new File(f)));
    }


    public static void main(String... args) throws Exception {
        String htkBinDir = "/usr/local/HTS-2.2beta/bin";
        String dataDir = "/Users/posttool/Documents/github/hmi-www/app/build/data/jbw-vocb";
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        BAlign aligner = new BAlign(htkBinDir, dataDir);
        aligner.compute(phoneSet.getPhones());
        aligner.copyToSpeechMarkup();
    }

}
