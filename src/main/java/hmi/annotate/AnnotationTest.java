package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.data.Syllable;
import hmi.data.Word;
import hmi.nlp.NLP;
import hmi.nlp.POSTagger;
import hmi.phone.PhoneSet;
import hmi.phone.Phonetizer;
import hmi.phone.Syllabifier;

import java.util.List;

public class AnnotationTest {
    static String BP = "/Users/posttool/Documents/github/la/src/test/resources";

    public static void main(String[] args) throws Exception {
        // initialize "services"
        NLP nlp = new NLP("en_US");
        POSTagger posTagger = new POSTagger(nlp);
        Phonetizer phonetizer = new Phonetizer(nlp, BP + "/en_US/dict.txt");
        PhoneSet phoneSet = PhoneSet.getPhoneSet(BP + "/en_US/phones.xml");
        // process a document
        SpeechMarkup sm = posTagger
                .process("This is one. Tis is two. How do you do, if you don't mind me asking? "
                        + "Furthermore, it stands to reason that I wouldnt use a comma here but would in San Francisco, California. "
                        + "Why wouldn't you eat out when you could eat on Mars? A sentence without a break -- like this one here -- "
                        + "is a weird sentence.\n\nThis is even more material for the test.");
        for (Word w : sm.getWords()) {
            phonetizer.addTranscript(w);
            if (w.isVoiced()) {
                List<Syllable> syllables = Syllabifier.syllabify(phoneSet, w.getPh().toLowerCase());
                for (Syllable syl : syllables)
                    w.addSyllable(syl);
            }
        }
        System.out.println(sm);
    }
}
