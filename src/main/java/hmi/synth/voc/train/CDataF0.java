package hmi.synth.voc.train;


import hmi.data.SpeechMarkup;
import hmi.util.Command;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class CDataF0 {
    String reaperBin;
    String dataDir;
    FileList files;

    public CDataF0(String reaperBin, String dataDir) throws Exception {
        this.reaperBin = reaperBin;
        this.dataDir = dataDir;
        File f = new File(reaperBin);
        if (!f.exists())
            throw new Exception("No REAPER: " + reaperBin);
        new File(dataDir + "/reaper/").mkdir();
        files = new FileList(dataDir + "/wav", ".wav");
    }

    public void compute() {
        int s = files.length();
        for (int i = 0; i < s; i++) {
            String w = dataDir + "/wav/" + files.name(i) + ".wav";
            String o = dataDir + "/reaper/" + files.name(i) + ".f0";
            try {
                Command.bash(reaperBin + " -i " + w + " -f " + o + " -a");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void copyToSpeechMarkup() {
        int s = files.length();
        for (int i = 0; i < s; i++) {
            SpeechMarkup sm = new SpeechMarkup();
            sm.readJSON(dataDir + "/sm/" + files.name(i) + ".json");
            String o = dataDir + "/reaper/" + files.name(i) + ".f0";
            try {
                String fs = FileUtils.getFileAsString(new File(o), "UTF-8");
                String[] lines = fs.split("\n");
                for (int c = 8; c < lines.length; c++) {
                    String[] line = lines[c].split(" ");
                    float t = Float.parseFloat(line[0]);
                    int v = Integer.parseInt(line[1]);
                    float f0 = Float.parseFloat(line[2]);
                }

            } catch (IOException e) {
                System.err.println("COULDNT READ " + files.name(i));
            }
        }
    }

    public static void main(String... args) throws Exception {
        String reaperBin = "/Users/posttool/Documents/github/REAPER/build/reaper";
        String dataDir = "/Users/posttool/Documents/github/hmi-www/app/build/data/jbw-vocb";
        CDataF0 r = new CDataF0(reaperBin, dataDir);
//        r.compute();
        r.copyToSpeechMarkup();
    }
}