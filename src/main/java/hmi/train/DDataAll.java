package hmi.train;

import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.features.*;
import hmi.phone.PhoneSet;
import hmi.util.Command;
import hmi.util.FileUtils;
import hmi.util.Resource;

import java.io.*;
import java.util.*;

public class DDataAll {

    public final boolean MGC = true;
    public final boolean LF0 = true;
    public final boolean STR = true;
    public final boolean MAG = true;
    public final boolean CMP = true;
    public final boolean LABEL = true;
    public final boolean QUESTIONS = true;
    public final boolean LIST = true;
    public final boolean SCP = true;
    public final boolean ADAPTSCRIPTS = false;

    VoiceRepo repo;

    public DDataAll(VoiceRepo repo) {
        this.repo = repo;

    }

    public boolean compute(PhoneSet phoneSet) throws Exception {
        String voiceDir = repo.path("/");
        if (LABEL) {
            if (!ADAPTSCRIPTS)
                makeLabels();
//            else
//                makeLabelsAdapt(voiceDir);
        }
        if (QUESTIONS) {
            makeQuestions(phoneSet);
        }

        if (MGC)
            Command.bash("cd " + voiceDir + "hts/data; make mgc;");
        if (LF0)
            Command.bash("cd " + voiceDir + "hts/data; make lf0;");
        if (STR)
            Command.bash("cd " + voiceDir + "hts/data; make str;");
        if (MAG)
            Command.bash("cd " + voiceDir + "hts/data; make mag;");
        if (CMP)
            Command.bash("cd " + voiceDir + "hts/data; make cmp3;");
        if (LIST)
            Command.bash("cd " + voiceDir + "hts/data; make list;");
        if (SCP)
            Command.bash("cd " + voiceDir + "hts/data; make scp;");

        return true;
    }

    public void makeQuestions(PhoneSet ps) throws IOException {
        File qdir = new File(repo.path("/hts/data/questions"));
        if (!qdir.exists())
            qdir.mkdir();
        FeatureAlias fa = new FeatureAlias();
        PhoneFeatures fp = new PhoneFeatures(ps);
        SpeechMarkupFeatures sf = new SpeechMarkupFeatures(fa);
        FileWriter qfile = new FileWriter(repo.path("hts/data/questions/questions_qst001.hed"));
        qfile.write(fp.get_questions_qst001_hed());
        qfile.write(sf.questions_qst001_hed());
        qfile.close();

        qfile = new FileWriter(repo.path("hts/data/questions/questions_utt_qst001.hed"));
        qfile.write(sf.questions_utt_qst001_hed());
        qfile.close();
    }

    public void makeLabels() throws Exception {

        System.out.println("\n Making labels:");
        FeatureAlias fa = new FeatureAlias();
        SpeechMarkupFeatures sf = new SpeechMarkupFeatures(fa);

        // hts/data/labels/full
        // hts/data/labels/mono
        // hts/data/labels/gen (10 examples)
        File labelsDir = new File(repo.path("/hts/data/labels"));
        if (!labelsDir.exists())
            labelsDir.mkdir();
        File monoDir = new File(repo.path("/hts/data/labels/mono"));
        if (!monoDir.exists())
            monoDir.mkdir();
        File fullDir = new File(repo.path("/hts/data/labels/full"));
        if (!fullDir.exists())
            fullDir.mkdir();
        File genDir = new File(repo.path("/hts/data/labels/gen"));
        if (!genDir.exists())
            genDir.mkdir();

        for (int i = 0; (i < repo.files().length()); i++) {
            String basename = repo.files().name(i);
            SpeechMarkup sm = repo.getSpeechMarkup(i);
            monoAndFullLabels(sf, sm,
                    repo.path("/hts/data/labels/full/" + basename + ".lab"),
                    repo.path("/hts/data/labels/mono/" + basename + ".lab"));
        }

        // hts/data/labels/full.mlf
        // hts/data/labels/mono.mlf
        FileWriter fullMlf = new FileWriter(repo.path("/hts/data/labels/full.mlf"));
        fullMlf.write("#!MLF!#\n");
        fullMlf.write("\"*/*.lab\" -> \"" + repo.path("/hts/data/labels/full") + "\"\n");
        fullMlf.close();

        FileWriter monoMlf = new FileWriter(repo.path("/hts/data/labels/mono.mlf"));
        monoMlf.write("#!MLF!#\n");
        monoMlf.write("\"*/*.lab\" -> \"" + repo.path("/hts/data/labels/mono") + "\"\n");
        monoMlf.close();

        for (int i = 0; i < 10; i++) {
            String basename = repo.files().name(i);
            FileUtils.copy(
                    repo.path("/hts/data/labels/full/" + basename + ".lab"),
                    repo.path("/hts/data/labels/gen/gen_" + basename + ".lab"));
        }
    }

    private void monoAndFullLabels(SpeechMarkupFeatures sf, SpeechMarkup sm, String outFeaFileName,
                                   String outLabFileName) throws Exception {

        FileWriter outLab = new FileWriter(outLabFileName);
        FileWriter outFea = new FileWriter(outFeaFileName);

        List<SegmentFeatures> segfs = sf.getFeatures(sm);

        for (SegmentFeatures feat : segfs) {
            Segment s = feat.getSegment();
            outLab.write((s.getBegin() * 1E7) + "  " + (s.getEnd() * 1E7) + " " + s.getPhone() + "\n");
            outFea.write(feat.fullLabels());
        }

        outLab.close();

        outFea.close();

    }


    public static void main(String[] args) throws Exception {
        VoiceRepo repo = new VoiceRepo("jbw-vocb");
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        DDataAll data = new DDataAll(repo);
        data.compute(phoneSet);
//        data.makeQuestions(phoneSet);
//        data.makeLabels();
    }

}
