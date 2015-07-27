package hmi.data;


import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VoiceRepo {
    public final static String BD = "/Users/posttool/Documents/github/hmi-www/app/build/data/";

    static {
        System.out.println("VOICE REPO INIT " + BD);
    }

    String voiceId;
    FileList wavFiles;
    // FileList textFiles;
    Properties properties;

    public VoiceRepo(String voiceId) throws IOException {
        this.voiceId = voiceId;
        File wav = getFile("wav");
        if (!wav.exists() || !wav.isDirectory()) {
            throw new IOException("Requires /wav  directory [" + this.voiceId + "].");
        }
        File props = getFile("voice.properties");
        if (!props.exists()) {
            throw new IOException("Requires voice.properties [\" + this.voiceId + \"].");
        }
        System.out.println("VoiceRepo [" + this.voiceId + "]");
        wavFiles = new FileList(path("wav"), ".wav");
        //textFiles = new FileList(path("text"), ".txt");
        // compare...
        properties = new Properties();
        InputStream in = new FileInputStream(props);
        properties.load(in);
        in.close();
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

    public FileList files() {
        return wavFiles;
    }

    public FileList wavFiles() {
        return wavFiles;
    }

    public FileList textFiles() {
        return new FileList(path("text"), ".txt");
    }

    public String prop(String key) {
        return (String) properties.get(key);
    }

    public String path(String... path) {
        StringBuilder b = new StringBuilder();
        b.append(BD);
        b.append(voiceId);
        for (String s : path) {
            if (s.charAt(0) != File.separatorChar)
                b.append(File.separatorChar);
            b.append(s);
        }
        return b.toString();
    }

    //
    public SpeechMarkup getSpeechMarkup(int idx) throws IOException {
        SpeechMarkup sm = new SpeechMarkup();
        sm.readJSON(path("sm", wavFiles.name(idx) + ".json"));
        return sm;
    }

}
