package hmi.train;


import hmi.util.Command;
import hmi.util.FileUtils;
import hmi.util.Resource;

import java.io.File;

public class CInitHTS {

    VoiceRepo repo;
    String configureFile;
    int sampRate;
    String frameLen, frameShift, fftLen, freqWarp, strFilterName;

    public CInitHTS(VoiceRepo repo) {
        this.repo = repo;
        this.repo.init("hts");
        configureFile = repo.path("hts/configure");
        sampRate = Integer.parseInt(repo.prop("sampRate"));
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

        if (sampRate >= 48000) {
            strFilterName = "filters/mix_excitation_5filters_199taps_48Kz.txt";
        } else {
            strFilterName = "filters/mix_excitation_5filters_99taps_16Kz.txt";
        }

    }

    public void init() throws Exception {
//        FileUtils.copyFolderRecursive(Resource.path("/HTS-1"), repo.path("hts"), false);
        FileUtils.copyFolderRecursive(Resource.path("/hts-q/hts-1"), repo.path("hts"), false);
        convertWav2Raw(repo.path("wav"), repo.path("hts/data/raw"));
    }

    private void convertWav2Raw(String wavDirName, String rawDirName) throws Exception {
        File rawDir = new File(rawDirName);
        if (!rawDir.exists())
            rawDir.mkdir();
        File wavDir = new File(wavDirName);
        for (File f : wavDir.listFiles()) {
            String n = f.getName();
            n = n.substring(0, n.lastIndexOf('.'));
            Command.bash(repo.prop("soxPath") + "/sox " + f.getAbsolutePath() + " " + rawDirName + "/" + n + ".raw"); //-v 0.7
        }
    }

    public void compute() throws Exception {
        String configureFile = repo.path("hts/configure");
//        if (!repo.prop("adaptScripts").equals("true")) {
                /* if previous files and directories exist then run configure */
                /* first it should go to the hts directory and there run ./configure */
            System.out.println("Running make configure: ");
            Command.bash("chmod +x " + configureFile);
            Command.bash("cd " + repo.path("hts") + "\n"
                    + configureFile
                    + " --with-sptk-search-path=" + repo.prop("sptkPath")
                    + " --with-hts-search-path=" + repo.prop("htsPath")
                    + " --with-hts-engine-search-path=" + repo.prop("htsEnginePath")
                    + " SPEAKER=" + repo.prop("speaker")
                    + " DATASET=" + repo.prop("dataset")
                    + " LOWERF0=" + repo.prop("lowerF0")
                    + " UPPERF0=" + repo.prop("upperF0")
                    + " VER=" + repo.prop("ver")
                    + " QNUM=" + repo.prop("qnum")
                    + " FRAMELEN=" + frameLen
                    + " FRAMESHIFT=" + frameShift
                    + " WINDOWTYPE=" + repo.prop("windowType")
                    + " NORMALIZE=" + repo.prop("normalize")
                    + " FFTLEN=" + fftLen
                    + " FREQWARP=" + freqWarp
                    + " GAMMA=" + repo.prop("gamma")
                    + " MGCORDER=" + repo.prop("mgcOrder")
                    + " STRORDER=" + repo.prop("strOrder")
                    + " STRFILTERNAME=" + strFilterName
                    + " LNGAIN=" + repo.prop("lnGain")
                    + " SAMPFREQ=" + sampRate
                    + " NSTATE=" + repo.prop("nState")
                    + " NITER=" + repo.prop("nIter"));

//        } else {
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
//        }

    }


    public static void main(String[] args) throws Exception {
        VoiceRepo repo = new VoiceRepo("jbw-vocb");
        CInitHTS hts = new CInitHTS(repo);
        hts.init();
        hts.compute();
    }
}
