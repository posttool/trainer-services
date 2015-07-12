package hmi.synth.voc.train;


import hmi.annotate.SpeechMarkupAnnotater;
import hmi.data.SpeechMarkup;
import hmi.data.VoiceRepo;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.IOException;

public class ASpeechMarkup {
    VoiceRepo root;
    SpeechMarkupAnnotater ann;

    public ASpeechMarkup(String dataDir) throws IOException {
        root = new VoiceRepo(dataDir);
        root.init("/sm");
        this.ann = new SpeechMarkupAnnotater("en_US");
    }

    public void compute() {
        FileList textFiles = root.textFiles();
        int s = textFiles.length();
        for (int i = 0; i < s; i++) {
            String fn = textFiles.name(i);
            String smof = root.path("sm", fn + ".json");
            try {
                String t = FileUtils.getFile(textFiles.file(i));
                SpeechMarkup sm = ann.annotate(t);
                sm.writeJSON(smof);
            } catch (Exception e) {
                System.err.println("CANT PROCESS " + fn);
            }
        }
    }

    public static void main(String... args) throws Exception {
        String dataDir = "jbw-vocb";
        ASpeechMarkup asm = new ASpeechMarkup(dataDir);
        asm.compute();
    }
}
