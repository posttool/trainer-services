package hmi.synth.voc.train;


import com.google.common.primitives.Floats;
import hmi.data.Phone;
import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.util.Command;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CDataF0 {
    String reaperBin;
    VoiceRoot root;
    FileList files;

    public CDataF0(String reaperBin, String dataDir) throws Exception {
        this.reaperBin = reaperBin;
        this.root = new VoiceRoot(dataDir);
        root.init("reaper");
        files = root.wavFiles();
        File f = new File(reaperBin);
        if (!f.exists())
            throw new Exception("No REAPER: " + reaperBin);
    }

    public void compute() {
        int s = files.length();
        for (int i = 0; i < s; i++) {
            String w = root.path("wav", files.name(i) + ".wav");
            String o = root.path("reaper", files.name(i) + ".f0");
            try {
                Command.bash(reaperBin + " -i " + w + " -f " + o + " -a");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void copyToSpeechMarkup() {
        int s = files.length();
        for (int i = 0; i < 1; i++) {
            SpeechMarkup sm = new SpeechMarkup();
            sm.readJSON(root.path("sm", files.name(i) + ".json"));
            List<Segment> segs = sm.getSegments();
            int segi = 0;
            Segment cur = segs.get(segi);
            List<Float> floats = new ArrayList<>();
            String o = root.path("reaper", files.name(i) + ".f0");
            try {
                String fs = FileUtils.getFile(new File(o));
                String[] lines = fs.split("\n");
                for (int c = 8; c < lines.length; c++) {
                    String[] line = lines[c].split(" ");
                    float t = Float.parseFloat(line[0]);
                    int v = Integer.parseInt(line[1]);
                    float f0 = Float.parseFloat(line[2]);
                    if (t >= cur.getEnd()) {
                        if (cur instanceof Phone) {
                            Phone ph = (Phone) cur;
                            ph.setF0(Floats.toArray(floats));
                            System.out.println(ph + " " + floats);
                        }
                        segi++;
                        if (segi < segs.size()) {
                            cur = segs.get(segi);
                            floats = new ArrayList<>();
                        } else
                            break;
                    }
                    if (t >= cur.getBegin()) {
                        floats.add(f0);
                    }
                }
                sm.writeJSON(root.path("sm", files.name(i) + ".json"));
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
