package hmi.train;


import com.google.common.primitives.Floats;
import hmi.data.Phone;
import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.data.VoiceRepo;
import hmi.util.Command;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DDataF0 {
    String reaperBin;
    VoiceRepo root;
    FileList files;

    public DDataF0(String reaperBin, String voiceId) throws Exception {
        this.reaperBin = reaperBin;
        this.root = new VoiceRepo(voiceId);
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

    public void copyToSpeechMarkup() throws IOException {
        int s = files.length();
        for (int i = 0; i < s; i++) {
            String smof = root.path("sm", files.name(i) + ".json");
            SpeechMarkup sm = new SpeechMarkup();
            sm.readJSON(smof);
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
                            //System.out.println(ph + " " + floats);
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
                sm.writeJSON(smof);
            } catch (IOException e) {
                System.err.println("COULDNT READ " + files.name(i));
            }
        }
    }

    public static void main(String... args) throws Exception {
        String reaperBin = "/Users/posttool/Documents/github/REAPER/build/reaper";
        DDataF0 r = new DDataF0(reaperBin, "jbw-vocb");
//        r.compute();
        r.copyToSpeechMarkup();
    }
}
