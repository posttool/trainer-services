package hmi.synth.voc.train;

import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTKLabeler {
    private boolean DEBUG = true; // TODO add logger

    private String outputDir;
    protected String labExt = ".lab";

    protected int MAX_ITERATIONS = 150;
    protected int MAX_SP_ITERATION = 10;
    protected int MAX_VP_ITERATION = 20;
    protected int MAX_MIX_ITERATION = 30;

    protected int noIterCompleted = 0;

    private String HTK_SO = "-A -D -V -T 1"; // Main HTK standard Options HTK_SO
    private String Extract_FEAT = "MFCC_0"; // MFCC_E
    private String Train_FEAT = "MFCC_0_D_A"; // MFCC_E_D_A
    // 13; //13 without D_A; 13*3 with D_A
    private int Train_VECTSIZE = 13 * 3;
    private int NUMStates = 5;
    private int[] num_mixtures_for_state = { 2, 1, 2 };
    private int[] current_number_of_mixtures = { 1, 1, 1 };

    private ArrayList<Double> logProbFrame_array = new ArrayList<Double>();
    private ArrayList<Double> epsilon_array = new ArrayList<Double>();
    private int PHASE_NUMBER = 0;
    private double[] epsilon_PHASE = { 0.2, 0.05, 0.001, 0.0005 }; // 0 1 2 3

    public void checkHTK() throws IOException {
        File htkFile = new File(getHTKBinDir() + File.separator + "HInit");
        if (!htkFile.exists()) {
            throw new IOException("HTK path setting is wrong. Because file " + htkFile.getAbsolutePath()
                    + " does not exist");
        }
    }

    public boolean compute(Set<String> phones) throws Exception {
        checkHTK();
        outputDir = getHTKPath("etc");

        System.out.println("Preparing voice database for labelling using HTK :");
        System.out.println("Setting up HTK directories ...");
        new File(getHTKDataDir()).mkdir();
        process("( cd " + getHTKDataDir() + "; mkdir -p hmm" + "; mkdir -p etc" + "; mkdir -p feat"
                + "; mkdir -p config" + "; mkdir -p lab" + "; exit )\n");
        createRequiredFiles();
        createPhoneDictionary(phones);
        getPhoneSequence();

        // remove multiple sp
        for (int i = 3; i < 8; i++) {
            int x = i + 1;
            if (x > 7)
                x = 3;
            delete_multiple_sp_in_PhoneMLFile(getHTKDataDir() + File.separator + "etc" + File.separator + "htk.phones"
                    + i + ".mlf", getHTKDataDir() + File.separator + "etc" + File.separator + "htk.phones" + x + ".mlf");
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
            String input = joinPath(getWavDir(), files().get(i) + ".wav");
            String output = getHTKPath("feat", files().get(i) + ".mfcc");
            pw.println(input + " " + output);
        }
        pw.close();

        pw = getPW(getHTKPath("etc", "htkTrain.list"));
        for (int i = 0; i < files().length(); i++) {
            String mFile = getHTKPath("feat", files().get(i) + ".mfcc");
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
        process("( " + hled + " -l '*' -i " + fileout + " " + mkphoneLED + " " + filein + "; exit )\n");
    }

    private void featureExtraction() throws Exception {
        String hcopy = getHTKBinDir() + File.separator + "HCopy";
        String configFile = getHTKPath("config", "featEx.conf");
        String listFile = getHTKPath("config", "featEx.list");
        process("( cd " + getHTKDataDir() + "; " + hcopy + " -T 1 -C " + configFile + " -S " + listFile
                + " > log_featureExtraction.txt" + "; exit )\n");
    }

    private void initializeHTKTrain() throws Exception {
        String hcompv = getHTKBinDir() + File.separator + "HCompV";
        String configFile = getHTKPath("config", "htkTrain.conf");
        String listFile = getHTKPath("etc", "htkTrain.list");
        process("( cd " + getHTKDataDir() + " ; mkdir hmm/hmm-dummy ; " + " mkdir hmm/hmm-final ; " + hcompv + " "
                + HTK_SO + " -C " + configFile + " -f 0.01 -m -S " + listFile + " -M " + getHTKPath("hmm", "hmm-dummy")
                + " " + getHTKPath("config", "htk.proto") + " > log_initialiseHTKTrain.txt" + "; exit )\n");
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
        process("( cd " + getHTKDataDir() + "; sh etc/htkTrainScript.sh" + " > log_htkTrainScript.txt" + "; exit )\n");

        PrintWriter macroFile = getPW(getHTKPath("hmm", "hmm0", "macros"));
        macroFile.println("~o\n" + "<VecSize> 13\n" + "<" + Train_FEAT + ">");
        macroFile.println(FileUtils.getFileAsString(new File(getHTKPath("hmm", "hmm-dummy", "vFloors")), "ASCII"));
        macroFile.close();
    }

    private void herestTraining() throws Exception {

        String herest = getHTKBinDir() + File.separator + "HERest";
        String hhed = getHTKBinDir() + File.separator + "HHEd";

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
            System.out.println("Iteration number: " + iteration);
            String hmm0last = "hmm" + (iteration - 1);
            String hmm1now = "hmm" + iteration;

            File hmmItDir = new File(getHTKPath("hm", hmm1now));
            if (!hmmItDir.exists())
                hmmItDir.mkdir();

            if (PHASE_NUMBER == 0) {

                if (iteration == (SP_ITERATION + 1)) {
                    phoneMlf = getHTKPath("etc", "htk.phones2.mlf");
                    phoneList = getHTKPath("etc", "htk.phone2.list");
                    process("( cd " + getHTKDataDir() + "; " + hhed + " " + HTK_SO + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm1now, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
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
                    process("( cd " + getHTKDataDir() + "; " + hhed + " " + HTK_SO + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf_vp + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
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

                    process("( cd " + getHTKDataDir() + "; " + hhed + " " + HTK_SO + " -H "
                            + getHTKPath("hmm", hmm0last, "macros") + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs")
                            + " -M " + getHTKPath("hmm", hmm1now) + " " + hhedconf_mix + " " + phoneList
                            + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");
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
            process("( cd " + getHTKDataDir() + "; " + herest + " " + HTK_SO + " -C " + configFile + " -I " + phoneMlf
                    + " -t 250.0 150.0 1000.0" + " -S " + trainList + " -H " + getHTKPath("hmm", hmm0last, "macros")
                    + " -H " + getHTKPath("hmm", hmm0last, "hmmdefs") + " -M " + getHTKPath("hmm", hmm1now) + " "
                    + phoneList + " >> log_herestTraining_" + iteration + ".txt" + "; exit )\n");

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
        String filename = getHTKPath("log_herestTraining_" + iteration + ".txt");

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

        process("( cd " + getHTKDataDir() + "; " + hvite + " " + HTK_SO + " -b sil -l " + labDir + " -o W -C "
                + configFile + " -a -H " + macros + " -H " + hmmDef + " -i " + alignedMlf + " -t 250.0 -y lab" + " -I "
                + phoneMlf + " -S " + listFile + " " + phoneDict + " " + phoneList + " > log_hviteAligning.txt"
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

        process("( cd " + getHTKDataDir() + "; " + hlstats + " -T 1 -C " + configFile + " -b " + bigFile + " -o "
                + phoneList + " " + phoneMlf + " > log_hlstats.txt" + "; exit )\n");

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

        process("( cd " + getHTKDataDir() + "; " + hbuild + " -T 1 -C " + configFile + " -n " + bigFile + " "
                + phoneAugList + " " + netFile + " > log_hbuild.txt" + "; exit )\n");

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
            transLabelOut.println("\"*/" + files().get(i) + labExt + "\"");
            transLabelOut1.println("\"*/" + files().get(i) + labExt + "\"");
            transLabelOut2.println("\"*/" + files().get(i) + labExt + "\"");
            //TODO
//            phoneSeq = getLineFromXML(files().get(i), false, false);
//            transLabelOut.println(phoneSeq.trim());
//            phoneSeq = getLineFromXML(files().get(i), true, false);
//            transLabelOut1.println(phoneSeq.trim());
//            phoneSeq = getLineFromXML(files().get(i), true, true);
//            transLabelOut2.println(phoneSeq.trim());

            // System.out.println( "    " + getFiles().getName(i) );

        }
        transLabelOut.close();
        transLabelOut1.close();
        transLabelOut2.close();
    }

    private String re(String txt, String a, String b) {
        Pattern pattern = Pattern.compile(a);
        Matcher matcher = pattern.matcher(txt);
        return matcher.replaceAll(b);
    }

    private String getLineFromXML(SpeechMarkup sm, boolean spause, boolean vpause) throws Exception {

        String line;
        String phoneSeq;
        Matcher matcher;
        Pattern pattern;
        StringBuilder alignBuff = new StringBuilder();
        
        List<Segment> segments = sm.getSentences().get(0).getSegments();

//        alignBuff.append(collectTranscription(segments));
        phoneSeq = alignBuff.toString();
        phoneSeq = re(phoneSeq, "pau ssil ", "sil ");
        phoneSeq = re(phoneSeq, " ssil pau$", " sil");
        if (!vpause) {
            phoneSeq = re(phoneSeq, "vssil", "");
        } else {
            phoneSeq = re(phoneSeq, "vssil", "sp");
        }
        if (!spause) {
            phoneSeq = re(phoneSeq, "ssil", "");
        }
        phoneSeq += " .";
        phoneSeq = re(phoneSeq, "\\s+", "\n");
        // System.out.println(phoneSeq);
        return phoneSeq;
    }

    private String getHTKBinDir() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getAwkBinPath() {
        return "/usr/bin/awk";
    }

    private String getWavDir() {
        // TODO Auto-generated method stub
        return null;
    }

    private FileList files() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getHTKDataDir() {
        // TODO Auto-generated method stub
        return null;
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

    private int process(String s) throws IOException, InterruptedException, Exception {
        Runtime rtime = Runtime.getRuntime();
        Process process = rtime.exec("/bin/bash");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        pw.print(s);
        if (DEBUG)
            System.out.println(s);
        pw.close();
        process.waitFor();
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        } else {
            return process.exitValue();
        }
    }

}
