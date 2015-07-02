package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.data.Syllable;
import hmi.data.Word;
import hmi.nlp.NLPipeline;
import hmi.nlp.SpeechMarkupProcessor;
import hmi.phone.PhoneSet;
import hmi.phone.Phonetizer;
import hmi.phone.Syllabifier;

import java.util.List;

public class AnnotationTest {
    static String BP = "/Users/posttool/Documents/github/la/src/main/resources";
    static String S = "This is one. Tis is two. How do you do, if you don't mind me asking? "
            + "Furthermore, it stands to reason that I wouldn't use a comma here but would in San Francisco, California. "
            + "Why would you eat out when you could eat on Mars? A sentence without a break -- like this one here -- "
            + "is a weird sentence.\n\nThis is even more material for the test.";

    public static void main(String[] args) throws Exception {
        // initialize "services"
        NLPipeline nlp = new NLPipeline("en_US");
        SpeechMarkupProcessor markup = new SpeechMarkupProcessor(nlp);
        Phonetizer phonetizer = new Phonetizer(nlp, BP + "/en_US/dict.txt");
        PhoneSet phoneSet = new PhoneSet(BP + "/en_US/phones.xml");
        // process a document
        SpeechMarkup sm = markup.process(S);
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
