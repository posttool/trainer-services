package hmi.phone;

import hmi.data.Word;
import hmi.nlp.NLPipeline;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class Phonetizer {
    private HashMap<String, String> lex = new HashMap<String, String>();
    private NLPipeline pipeline;

    public Phonetizer(NLPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Phonetizer(NLPipeline pipeline, String lexfile) throws FileNotFoundException, IOException {
        this(pipeline);
        addLexFile(lexfile);
    }

    private int addLexFile(String lexfile) throws FileNotFoundException, IOException {
        int c = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(lexfile))) {
            for (String line; (line = br.readLine()) != null;) {
                if (!line.startsWith(";;;")) {
                    int i = line.indexOf(" ");
                    String w = line.substring(0, i);
                    String t = line.substring(i + 2);
                    lex.put(w, t);
                    c++;
                }
            }
        }
        return c;
    }

    Pattern puncpatt = Pattern.compile("\\p{Punct}+");

    public List<Word> getTranscript(String t) {
        List<Word> ss = new ArrayList<Word>();
        Annotation document = pipeline.annotate(t);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
                Word w = new Word(word.word());
                addTranscript(w);
                ss.add(w);
            }
        }
        return ss;
    }

    public Word addTranscript(Word word) {
        String tr = lex.get(word.getText().toUpperCase());
        String src = "lex";
        if (tr == null) {
            if (puncpatt.matcher(word.getText()).matches()) {
                tr = word.getText();
                src = "";
            } else {
                // rnn g2p
                src = "g2p";
            }
        }
        word.setPh(tr);
        word.setG2P(src);
        return word;
    }

    public static void main(String[] args) throws Exception {
        NLPipeline nlp = new NLPipeline("en_US");
        Phonetizer p = new Phonetizer(nlp, "/Users/posttool/Documents/github/la/src/test/resources/en_US/dict.txt");
        List<Word> words = p.getTranscript("This is a sentence, about nothing but biazibeetri -- I think.");
        for (Word w : words) {
            System.out.println(w.getText() + " / " + w.getPh() + " / " + w.getG2P());
        }
    }
}
