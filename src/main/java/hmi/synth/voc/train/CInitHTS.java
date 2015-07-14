package hmi.synth.voc.train;


import hmi.data.VoiceRepo;
import hmi.phone.PhoneSet;
import hmi.util.Command;
import hmi.util.FileUtils;
import hmi.util.Resource;

import java.io.File;
import java.io.IOException;

public class CInitHTS {

    VoiceRepo repo;
    String tlcPath, sptkPath, htsPath, htsEnginePath, soxPath;
    String configureFile;
    String speaker, dataset, lowerF0, upperF0, ver, qnum, frameLen, frameShift, windowType, normalize, fftLen, freqWarp,
            gamma, mgcOrder, strOrder, strFilterName, lnGain, sampFreq, nState, nIter, mgcBandWidth, strBandWith, lf0BandWidth,
            adaptTrainingSpkr, adaptSpeaker, adaptF0Ranges, adaptSpkrMask, adaptHead, numTestFiles, adaptTreeKind, adaptTransKind;
    boolean adaptScripts;

    public CInitHTS(VoiceRepo repo, int sampRate, String tlcPath, String sptkPath, String htsPath, String htsEnginePath, String soxPath) {
        this.repo = repo;
        this.tlcPath = tlcPath;
        this.sptkPath = sptkPath;
        this.htsPath = htsPath;
        this.htsEnginePath = htsEnginePath;
        this.soxPath = soxPath;
        configureFile = repo.path("hts/configure");
        speaker = "slt";
        dataset = "cmu_us_arctic";
        lowerF0 = "110";
        upperF0 = "280";
        numTestFiles = "10";
        ver = "1";
        qnum = "001";
        sampFreq = Integer.toString(sampRate);
        frameLen = Integer.toString((int) Math.round(sampRate * 0.025));
        frameShift = Integer.toString((int) Math.round(sampRate * 0.005));
        if (sampRate >= 48000) {
            fftLen = "2048";
            freqWarp = "0.55";
        } else if (sampRate >= 44100) {
            fftLen = "2048";
            freqWarp = "0.53";
        } else if (sampRate >= 22050) {
            fftLen = "1024";
            freqWarp = "0.45";
        } else if (sampRate >= 16000) {
            fftLen = "512";
            freqWarp = "0.42"; // default HTS-2.1
        } else if (sampRate >= 12000) {
            fftLen = "512";
            freqWarp = "0.37";
        } else if (sampRate >= 10000) {
            fftLen = "512";
            freqWarp = "0.35";
        } else {// sampRate >= 8000)
            fftLen = "256";
            freqWarp = "0.31";
        }
        windowType = "1";
        normalize = "1";
        gamma = "0";
        mgcOrder = "34";
        strOrder = "5";
        if (sampRate >= 48000) {
            strFilterName = "filters/mix_excitation_5filters_199taps_48Kz.txt";
        } else {
            strFilterName = "filters/mix_excitation_5filters_99taps_16Kz.txt";
        }
        mgcBandWidth = "35";
        strBandWith = "5";
        lf0BandWidth = "1";
        lnGain = "1";
        nState = "5";
        nIter = "5";
        adaptTrainingSpkr = "'bdl clb jmk rms'";
        adaptSpeaker = "slt";
        // "'awb 40 280  bdl 40 280  clb 80 350  jmk 40 280  rms 40 280  slt 80 350'";
        adaptF0Ranges = "'bdl 40 210 clb 130 260 jmk 50 180 rms 40 200 slt 110 280'";
        adaptSpkrMask = "*/cmu_us_arctic_%%%_*";
        adaptHead = "b05";
        numTestFiles = "5";
        adaptTreeKind = "dec";
        adaptTransKind = "feat";

        adaptScripts = false;
    }

    public void init() throws Exception {
        FileUtils.copyFolderRecursive(Resource.path("/HTS-1"), repo.path("hts"), false);
        convertWav2Raw(repo.path("hts/data/scripts"), repo.path("wav"), repo.path("hts/data/raw"));
    }

    private void convertWav2Raw(String scriptsDir, String wavDirName, String rawDirName) throws Exception {
        String cmdLine;
        String wav2rawCmd = scriptsDir + "/wav2raw.sh";
        System.out.println("Converting wav files to raw from: " + wavDirName + "  to: " + rawDirName);
        File rawDir = new File(rawDirName);
        if (!rawDir.exists())
            rawDir.mkdir();
        cmdLine = "chmod +x " + wav2rawCmd;
        Command.bash(cmdLine);
        cmdLine = wav2rawCmd + " " + soxPath + " " + wavDirName + " " + rawDirName;
        Command.bash(cmdLine);
    }

    public void compute() throws Exception {
        String configureFile = repo.path("hts/configure");
        if (!adaptScripts) {
                /* if previous files and directories exist then run configure */
                /* first it should go to the hts directory and there run ./configure */
            System.out.println("Running make configure: ");
            String cmdLine = "chmod +x " + configureFile;
            Command.bash(cmdLine);

            cmdLine = "cd " + repo.path("hts") + "\n"
                    + configureFile
//                    + " --with-tcl-search-path=" + tlcPath
                    + " --with-sptk-search-path=" + sptkPath
                    + " --with-hts-search-path=" + htsPath
                    + " --with-hts-engine-search-path=" + htsEnginePath
//                    + " --with-sox-search-path=" + soxPath
                    + " SPEAKER=" + speaker
                    + " DATASET=" + dataset
                    + " LOWERF0=" + lowerF0
                    + " UPPERF0=" + upperF0
                    + " VER=" + ver
                    + " QNUM=" + qnum
                    + " FRAMELEN=" + frameLen
                    + " FRAMESHIFT=" + frameShift
                    + " WINDOWTYPE=" + windowType
                    + " NORMALIZE=" + normalize
                    + " FFTLEN=" + fftLen
                    + " FREQWARP=" + freqWarp
                    + " GAMMA=" + gamma
                    + " MGCORDER=" + mgcOrder
                    + " STRORDER=" + strOrder
                    + " STRFILTERNAME=" + strFilterName
                    + " LNGAIN=" + lnGain
                    + " SAMPFREQ=" + sampFreq
                    + " NSTATE=" + nState
                    + " NITER=" + nIter;
            Command.bash(cmdLine);

        } else {
//            System.out.println("Running make configure: ");
//            cmdLine = "chmod +x " + getProp(CONFIGUREFILE);
//            General.launchProc(cmdLine, "configure", filedir);
//
//            cmdLine = "cd " + filedir + "hts\n" + getProp(CONFIGUREFILE) + " --with-tcl-search-path="
//                    + db.getExternal(DatabaseLayout.TCLPATH) + " --with-sptk-search-path="
//                    + db.getExternal(DatabaseLayout.SPTKPATH) + " --with-hts-search-path="
//                    + db.getExternal(DatabaseLayout.HTSPATH) + " --with-hts-engine-search-path="
//                    + db.getExternal(DatabaseLayout.HTSENGINEPATH) + " --with-sox-search-path="
//                    + db.getExternal(DatabaseLayout.SOXPATH) +
//                    " SPEAKER=" + getProp(SPEAKER) +
//                    " DATASET=" + getProp(DATASET) +
//                    " TRAINSPKR=" + getProp(ADAPTTRAINSPKR) +
//                    " ADAPTSPKR=" + getProp(ADAPTSPKR) +
//                    " F0_RANGES=" + getProp(ADAPTF0_RANGES) +
//                    " SPKRMASK=" + getProp(ADAPTSPKRMASK) +
//                    " ADAPTHEAD=" + getProp(ADAPTHEAD) +
//                    " VER=" + getProp(VER) +
//                    " QNUM=" + getProp(QNUM) +
//                    " FRAMELEN=" + getProp(FRAMELEN) +
//                    " FRAMESHIFT=" + getProp(FRAMESHIFT) +
//                    " WINDOWTYPE=" + getProp(WINDOWTYPE) +
//                    " NORMALIZE=" + getProp(NORMALIZE) +
//                    " FFTLEN=" + getProp(FFTLEN) +
//                    " FREQWARP=" + getProp(FREQWARP) +
//                    " GAMMA=" + getProp(GAMMA) +
//                    " MGCORDER=" + getProp(MGCORDER) +
//                    " STRORDER=" + getProp(STRORDER) +
//                    " STRFILTERNAME=" + getProp(STRFILTERNAME) +
//                    " LNGAIN=" + getProp(LNGAIN) +
//                    " SAMPFREQ=" + getProp(SAMPFREQ) +
//                    " NSTATE=" + getProp(NSTATE) +
//                    " NITER=" + getProp(NITER) +
//                    " MGCBANDWIDTH=" + getProp(MGCBANDWIDTH) +
//                    " STRBANDWIDTH=" + getProp(STRBANDWIDTH) +
//                    " LF0BANDWIDTH=" + getProp(LF0BANDWIDTH) +
//                    " TREEKIND=" + getProp(ADAPTTREEKIND) +
//                    " TRANSKIND=" + getProp(ADAPTTRANSKIND);
        }

    }


    public static void main(String[] args) throws Exception {
        VoiceRepo repo = new VoiceRepo("jbw-vocb");
        CInitHTS hts = new CInitHTS(repo, 16000,
                "/Library/Tcl/",
                "/usr/local",
                "/usr/local/HTS-2.2beta/bin",
                "/Users/posttool/Documents/github/la/deploy/install/hts_engine_API-1.09/bin",
                "/usr/local/bin");
        hts.init();
        hts.compute();
    }
}
