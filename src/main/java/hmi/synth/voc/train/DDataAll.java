package hmi.synth.voc.train;

import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.data.VoiceRepo;
import hmi.features.*;
import hmi.phone.PhoneSet;
import hmi.util.Command;
import hmi.util.FileUtils;
import hmi.util.Resource;

import java.io.*;
import java.util.*;

public class DDataAll {

    public final boolean MGC = false;
    public final boolean LF0 = false;
    public final boolean STR = false;
    public final boolean CMP = false;
    public final boolean LABEL = true;
    public final boolean QUESTIONS = true;
    public final boolean LIST = true;
    public final boolean SCP = true;
    public final boolean ADAPTSCRIPTS = false;

    public boolean compute(VoiceRepo repo, PhoneSet phoneSet) throws Exception {
        String voiceDir = repo.path("/");

        if (MGC) {
            Command.bash("cd " + voiceDir + "hts/data; make mgc;");
        }
        if (LF0) {
            Command.bash("cd " + voiceDir + "hts/data; make lf0;");
        }
        if (STR) {
            Command.bash("cd " + voiceDir + "hts/data; make str;");
        }
        if (CMP) {
            System.out.println("Concatenating mgc, lf0 and str data:");
            File dirMgc = new File(voiceDir + "hts/data/mgc");
            if (dirMgc.exists() && dirMgc.list().length == 0)
                throw new Exception("Error: directory " + voiceDir + "hts/data/mgc ");

            File dirLf0 = new File(voiceDir + "hts/data/lf0");
            if (dirLf0.exists() && dirLf0.list().length == 0)
                throw new Exception("Error: directory " + voiceDir + "hts/data/lf0 ");

            File dirStr = new File(voiceDir + "hts/data/str");
            if (dirStr.exists() && dirStr.list().length == 0)
                throw new Exception("Error: directory " + voiceDir + "hts/data/str ");

            Command.bash("cd " + voiceDir + "hts/data; make cmp;");
        }
        if (LABEL) {
            if (!ADAPTSCRIPTS)
                makeLabels(repo);
//            else
//                makeLabelsAdapt(voiceDir);
        }
        if (QUESTIONS) {
            makeQuestions(repo, phoneSet);
        }
        if (LIST) {
            Command.bash("cd " + voiceDir + "hts/data\nmake list\n");
        }
        if (SCP) {
            Command.bash("cd " + voiceDir + "hts/data\nmake scp\n");
        }

        return true;
    }

    private void makeQuestions(VoiceRepo repo, PhoneSet ps) throws IOException {
        File qdir = new File(repo.path("/hts/data/questions"));
        if (!qdir.exists())
            qdir.mkdir();
        FeatureAlias fa = new FeatureAlias();
        PhoneFeatures fp = new PhoneFeatures(ps);
        SentenceFeatures sf = new SentenceFeatures(fa);
        FileWriter qfile = new FileWriter(repo.path("hts/data/questions/questions_qst001.hed"));
        qfile.write(fp.get_questions_qst001_hed());
        qfile.write(sf.questions_qst001_hed());
        qfile.close();

        qfile = new FileWriter(repo.path("hts/data/questions/questions_utt_qst001.hed"));
        qfile.write(sf.questions_utt_qst001_hed());
        qfile.close();
    }

    private void makeLabels(VoiceRepo repo) throws Exception {

        System.out.println("\n Making labels:");
        FeatureAlias fa = new FeatureAlias();
        SentenceFeatures sf = new SentenceFeatures(fa);

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

    private void monoAndFullLabels(SentenceFeatures sf, SpeechMarkup sm, String outFeaFileName,
                                   String outLabFileName) throws Exception {

        FileWriter outFea = new FileWriter(outFeaFileName);
        FileWriter outLab = new FileWriter(outLabFileName);

        List<SegmentFeatures> segfs = sf.getFeatures(sm);

        for (SegmentFeatures feat : segfs) {

            Segment s = feat.getSegment();
            List<FeatureValue> values = feat.getValues();

            // HTK time units, hundreds of nanoseconds.
            outLab.write((s.getBegin() * 1E7) + "  " + (s.getEnd() * 1E7) + " " + s.getPhone() + "\n");

            Segment pp = s.getPrevPrevSegment();
            Segment p = s.getPrevSegment();
            Segment n = s.getNextSegment();
            Segment nn = s.getNextNextSegment();

            outFea.write((s.getBegin() * 1E7) + "  " + (s.getEnd() * 1E7) + " "
                    + (pp != null ? pp.getPhone() : "_") + "^"
                    + (p != null ? p.getPhone() : "_") + "-"
                    + s.getPhone() + "+"
                    + (n != null ? n.getPhone() : "_") + "="
                    + (nn != null ? nn.getPhone() : "_") + "|");

            for (FeatureValue fv : values) {
                if (fv.hasValue())
                    outFea.write("|" + fv.getName() + "=" + fv.getValue());
            }
            outFea.write("||\n");
        }

        outLab.close();
        outFea.close();

    }


    public static void main(String[] args) throws Exception {
        VoiceRepo repo = new VoiceRepo("jbw-vocb");
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        DDataAll data = new DDataAll();
        data.compute(repo, phoneSet);
//        data.makeQuestions(repo, phoneSet);
//        data.makeLabels(repo);
    }

}
