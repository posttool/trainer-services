package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.data.Syllable;
import hmi.data.Word;
import hmi.ml.nlp.NLPipeline;
import hmi.ml.nlp.SpeechMarkupProcessor;
import hmi.phone.PhoneSet;
import hmi.phone.Phonetizer;
import hmi.phone.Syllabifier;
import hmi.util.NumberToText;
import hmi.util.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeechMarkupAnnotator {
    NLPipeline nlp;
    SpeechMarkupProcessor markup;
    Phonetizer phonetizer;
    PhoneSet phoneSet;

    public SpeechMarkupAnnotator(String lng) throws IOException {
        nlp = new NLPipeline(lng);
        markup = new SpeechMarkupProcessor(nlp);
        phonetizer = new Phonetizer(nlp, Resource.path("/" + lng + "/dict.txt"));
        phoneSet = new PhoneSet(Resource.path("/" + lng + "/phones.xml"));
    }

    public SpeechMarkup annotate(String text) {
        SpeechMarkup sm = markup.from(text);
        for (Word w0 : sm.getWords()) {
            List<Word> words = preprocess(w0);
            if (words != null) {
                w0.replaceWith(words);
            } else {
                words = new ArrayList<>();
                words.add(w0);
            }
            for (Word w : words) {
                phonetizer.addTranscript(w);
                if (w.isVoiced()) {
                    try {
                        List<Syllable> syllables = Syllabifier.syllabify(phoneSet, w.getPh().toLowerCase());
                        for (Syllable syl : syllables) {
                            w.addSyllable(syl);
                        }
                    } catch (Exception e) {
                        System.out.println("CANT SYLLABIFY '" + w.getPh() + "' (" + text + ")");
                    }
                }
            }
        }
        return sm;
    }

    private static Pattern ACRONYM = Pattern.compile("[A-Z]{2,4}");
    private static Pattern NUMBER = Pattern.compile("-?\\d+\\.?\\d+?");

    private List<Word> preprocess(Word w) {
        String txt = w.getText();
        Matcher m = ACRONYM.matcher(txt);
        if (m.matches()) {
            List<Word> words = new ArrayList<>();
            for (int i = 0; i < txt.length(); i++) {
                String c = txt.substring(i, i + 1);
                Word nw = new Word(c);
                nw.setPartOfSpeech(w.getPartOfSpeech());
                nw.setDepth(w.getDepth());
                words.add(nw);
            }
            System.out.println(words);
            return words;
        }
        m = NUMBER.matcher(txt);
        if (m.matches()) {
            List<Word> words = new ArrayList<>();
            String[] ws = NumberToText.number(txt).split(" ");
            for (String c : ws) {
                Word nw = new Word(c);
                nw.setPartOfSpeech(w.getPartOfSpeech());
                nw.setDepth(w.getDepth());
                words.add(nw);
            }
            return words;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        SpeechMarkupAnnotator annotater = new SpeechMarkupAnnotator("en_US");
        SpeechMarkup sm = annotater.annotate("This is one. NLP is crazy, you know? Tis is two. How do you do, if you don't mind me asking? "
                + "Furthermore, it stands to reason that I wouldn't use a comma here but would in San Francisco, California, USA especially while drive 60. "
                + "Why would you eat out when you could eat on Mars? A sentence without a break -- like this one here -- "
                + "is a weird sentence.\n\nThis is even more material for the test. How about a phone number. Ha!");
        System.out.println(sm);
    }

}
