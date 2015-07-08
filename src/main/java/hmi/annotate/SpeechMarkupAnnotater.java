package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.data.Syllable;
import hmi.data.Word;
import hmi.nlp.NLPipeline;
import hmi.nlp.SpeechMarkupProcessor;
import hmi.phone.PhoneSet;
import hmi.phone.Phonetizer;
import hmi.phone.Syllabifier;
import hmi.util.Resource;

import java.io.IOException;
import java.util.List;

public class SpeechMarkupAnnotater {
    NLPipeline nlp;
    SpeechMarkupProcessor markup;
    Phonetizer phonetizer;
    PhoneSet phoneSet;

    public SpeechMarkupAnnotater(String lng) throws IOException {
        nlp = new NLPipeline(lng);
        markup = new SpeechMarkupProcessor(nlp);
        phonetizer = new Phonetizer(nlp, Resource.path("/" + lng + "/dict.txt"));
        phoneSet = new PhoneSet(Resource.path("/" + lng + "/phones.xml"));
    }

    public SpeechMarkup annotate(String text) {
        SpeechMarkup sm = markup.from(text);
        for (Word w : sm.getWords()) {
            phonetizer.addTranscript(w);
            if (w.isVoiced()) {
                try {
                    List<Syllable> syllables = Syllabifier.syllabify(phoneSet, w.getPh().toLowerCase());
                    syllables.stream().forEach((syl) -> w.addSyllable(syl));
                } catch (Exception e) {
                    System.out.println("CANT SYLLABIFY '"+w.getPh()+"' ("+text+")");
                }
            }
        }
        return sm;
    }

}