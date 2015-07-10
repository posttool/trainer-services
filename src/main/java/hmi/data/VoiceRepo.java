package hmi.data;


import hmi.util.FileList;

import java.io.File;
import java.io.IOException;

public class VoiceRepo {
    public final static String BD = "/Users/posttool/Documents/github/hmi-www/app/build/data/";

    static {
        System.out.println("VOICE REPO INIT " + BD);
    }

    String dataDir;
    FileList wavFiles;
    FileList textFiles;

    // TODO force constructor to declare requirements ["wav", "sm", "etc"]
    public VoiceRepo(String dataDir) throws IOException {
        this.dataDir = BD + dataDir;
        File wav = getFile("wav");
        File txt = getFile("text");
        if (!wav.exists() || !txt.exists() || !wav.isDirectory() || !txt.isDirectory()) {
            throw new IOException("Requires /wav and /text directories [" + this.dataDir + "].");
        }
        System.out.println("VoiceRepo [" + this.dataDir + "]");
        wavFiles = new FileList(this.dataDir + "/wav", ".wav");
        textFiles = new FileList(this.dataDir + "/text", ".txt");
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
        b.append(dataDir);
        for (String s : path) {
            b.append(File.separator);
            b.append(s);
        }
        return b.toString();
    }

}
