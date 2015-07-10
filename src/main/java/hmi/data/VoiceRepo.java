package hmi.data;


import hmi.util.FileList;

import java.io.File;
import java.io.IOException;

public class VoiceRepo {
    public final static String BD = "/Users/posttool/Documents/github/hmi-www/app/build/data/";

    static {
        System.out.println("VOICE REPO INIT " + BD);
    }

    String voiceId;
    FileList wavFiles;
    FileList textFiles;

    // TODO  constructor to declare requirements ["wav", "sm", "etc"]
    public VoiceRepo(String voiceId) throws IOException {
        this.voiceId = voiceId;
        File wav = getFile("wav");
        File txt = getFile("text");
        if (!wav.exists() || !txt.exists() || !wav.isDirectory() || !txt.isDirectory()) {
            throw new IOException("Requires /wav and /text directories [" + this.voiceId + "].");
        }
        System.out.println("VoiceRepo [" + this.voiceId + "]");
        wavFiles = new FileList(path("wav"), ".wav");
        textFiles = new FileList(path("text"), ".txt");
    }

    public boolean init(String dir) {
        File d = getFile(dir);
        if (!d.exists()) {
            return d.mkdir();
        } else {
            return false;
        }
    }

    public File getFile(String... path) {
        return new File(path(path));
    }

    public FileList wavFiles() {
        return wavFiles;
    }

    public FileList textFiles() {
        return textFiles;
    }

    public String path(String... path) {
        StringBuilder b = new StringBuilder();
        b.append(BD);
        b.append(voiceId);
        for (String s : path) {
            b.append(File.separator);
            b.append(s);
        }
        return b.toString();
    }

}
