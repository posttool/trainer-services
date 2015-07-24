package hmi.train;


import hmi.annotate.SpeechMarkupAnnotater;
import hmi.data.*;
import hmi.util.FileList;
import hmi.util.FileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ASpeechMarkup {
    VoiceRepo root;
    SpeechMarkupAnnotater ann;

    public ASpeechMarkup(String dataDir) throws IOException {
        this(new VoiceRepo(dataDir));
    }

    public ASpeechMarkup(VoiceRepo root) throws IOException {
        this.root = root;
        root.init("/sm");
    }

    public void compute() throws IOException {
        if (ann == null)
            ann = new SpeechMarkupAnnotater("en_US");

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

    public void impose() throws Exception {
        Map<String, String> mlf2nn = new HashMap<>();
        FileList mlfs = new FileList(root.path("mlf"), ".txt");
        for (int c = 0; c < mlfs.length(); c++) {
            String mlf = FileUtils.getFile(mlfs.file(c));
            Sentence s = getSetenceFromMlf(mlf.split("\n"));
            SpeechMarkup sm = new SpeechMarkup();
            Paragraph pp = new Paragraph();
            pp.addSentence(s);
            sm.addParagraph(pp);
            String name = mlfs.name(c);
            String newName = "hmi_us_a0_xxx_" + ln(c, 5);
            mlf2nn.put(name, newName);
            sm.writeJSON(root.path("sm", newName + ".json"));
            FileUtils.copy(root.path("wavm", name + ".wav"), root.path("wav", newName + ".wav"));
            if (c % 100 == 0)
                System.out.println(s);
        }
    }

    private String ln(int c, int l) {
        String s = "000000000000000" + c;
        return s.substring(s.length() - l);
    }

    public Sentence getSetenceFromMlf(String[] lines) {
        Sentence s = new Sentence();
        Phrase phrase = new Phrase();
        s.addPhrase(phrase);
        Word word = null;
        Syllable syll = null;
        for (String line : lines) {
            String[] ben = line.split("\t");
            if (ben.length < 3) {
                System.out.println("Short line " + line);
                continue;
            }
            float b = Float.parseFloat(ben[0]);
            float e = Float.parseFloat(ben[1]);
            String n = ben[2].trim();
            if (b == 0 && e == 0) {
                if (n.startsWith("_ ")) {
                    word = new Word();
                    word.setText(n.substring(2));
                    syll = new Syllable();
                    word.addSyllable(syll);
                    phrase.addWord(word);
                } else if (n.equals("-")) {
                    syll = new Syllable();
                    word.addSyllable(syll);
                }
            } else {
                Phone ph = new Phone();
                ph.setBegin(b);
                ph.setEnd(e);
                ph.setDuration(e - b);
                ph.setPhone(n);
                syll.addPhone(ph);
            }
        }
        return s;
    }

    public static void main(String... args) throws Exception {
        String dataDir = "tom-src";
        ASpeechMarkup asm = new ASpeechMarkup(dataDir);
        //asm.compute();
        asm.impose();
    }
}
