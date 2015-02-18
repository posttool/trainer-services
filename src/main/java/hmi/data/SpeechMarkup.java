package hmi.data;

import java.util.ArrayList;
import java.util.List;

// sentence
//   phrase
//     text
//       syllable
//         ph

public class SpeechMarkup {
    Document document;
    String text;

    public SpeechMarkup() {
        document = new Document();
    }

    public SpeechMarkup(String text) {
        this();
        this.text = text;
    }

    public void addSentence(Sentence s) {
        document.add(s);
    }

    public List<Sentence> getSentences() {
        List<Sentence> sentences = new ArrayList<Sentence>();
        for (Paragraph p : document.paragraphs) {
            for (Sentence sentence : p.sentences) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("HMML 0.1\n");
        for (Paragraph p : document.paragraphs) {
            for (Sentence sentence : p.sentences) {
                b.append(sentence.toString());
            }
        }
        return b.toString();
    }

    public String getLocale() {
        // TODO
        return "en";
    }

    public String getVoice() {
        // TODO
        return "X";
    }

    public String getText() {
        return text;
    }

}
