package hmi.train;

import hmi.data.*;
import hmi.phone.PhoneSet;
import hmi.phone.Syllabifier;
import hmi.util.FileList;
import hmi.util.FileUtils;
import hmi.util.PraatReader;
import hmi.util.Resource;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Train2 {
    public static void main(String... args) throws Exception {
        VoiceRepo repo = new VoiceRepo("gri-voca");
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        compile(repo, phoneSet, "/Users/posttool/Documents/github/jibo", getScripts());
        BAlign aligner = new BAlign(repo);
        aligner.compute(phoneSet);
        aligner.copyToSpeechMarkup();
//        CInitHTS hts = new CInitHTS(repo);
//        hts.init();
//        hts.compute();
        DDataAll data = new DDataAll(repo);
        data.makeQuestions(phoneSet);
        data.makeLabels(phoneSet);
    }

    public static List<String> getScripts() {
        List<String> s = new ArrayList<>();
        for (int i = 1; i < 50; i++) {
            s.add("script" + ln(i*100, 5));
        }
        return s;
    }

    public static Map<String, String> compile(VoiceRepo repo, PhoneSet phoneSet, String bp, List<String> pkgs) throws IOException {
        Map<String, String> tm = new TreeMap<>();
        int c = 0;
        for (String pkg : pkgs) {
            System.out.println(bp + "/" + pkg);
            FileList tgs = new FileList(bp + "/" + pkg, ".TextGrid");
            int tgslen = tgs.length();
            for (int i = 0; i < tgslen; i++) {
                PraatReader pr = new PraatReader(tgs.file(i));
                List<PraatReader.Line> lines = pr.getSection("ARPABET");
                if (lines == null) {
                    //System.out.println("? no arpabet " + f);
                    continue;
                }
                SpeechMarkup sm = new SpeechMarkup();
                Paragraph pp = new Paragraph();
                Sentence s = new Sentence();
                Phrase phrase = new Phrase();
                s.addPhrase(phrase);
                pp.addSentence(s);
                sm.addParagraph(pp);
                for (PraatReader.Line line : lines) {
                    //System.out.println("!! " + line.text() + " " + line.xmax());
                    Word w = new Word();
                    try {
                        List<Syllable> syllables = Syllabifier.syllabify(phoneSet, line.text().toLowerCase());
                        for (Syllable syl : syllables) {
                            w.addSyllable(syl);
                        }
                    } catch (Exception e) {
                        //System.out.println("CANT SYLLABIFY '" + line.text());
                        Syllable sy = new Syllable();
                        w.addSyllable(sy);
                        String[] phs = line.text().split(" ");
                        for (String ph : phs) {
                            String sp = ph.toLowerCase().trim();
                            if (sp.equals(""))
                                continue;
                            Phone p = new Phone();
                            p.setPhone(ph.toLowerCase());
                            sy.addPhone(p);
                        }
                    }
                    if (!w.isEmpty())
                        phrase.addWord(w);
                }
                //System.out.println(sm);
                String name = tgs.name(i);
                if (name.indexOf("[")!=-1)
                    continue;
                String newName = repo.prop("dataset") + "_" + repo.prop("speaker") + "_" + ln(c, 5);
                FileUtils.copy(bp + "/" + pkg + "/" + name + ".wav", repo.path("wav", newName + ".wav"));
                sm.writeJSON(repo.path("sm", newName + ".json"));
                System.out.println(newName);
                c++;
            }
        }
        return tm;
    }

    private static String ln(int c, int l) {
        String s = "000000000000000" + c;
        return s.substring(s.length() - l);
    }
}