package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.nlp.PosTagger;

public class PosAnnotator implements Annotator {

    PosTagger tagger = new PosTagger();

    /*
     * assumes sm.text is ready to be processed. this means utf-8 double returns
     * for paragraphs. no html if possible... entities expanded please! returns
     * a SpeechMarkup instance w/ sentences each with a single phrase of words
     * words have pos
     */
    public SpeechMarkup annotate(SpeechMarkup sm) {
        return tagger.process(sm.getText());
    }

    public SpeechMarkup annotate(String text) {
        return tagger.process(text);
    }

}
