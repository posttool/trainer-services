package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Sentence implements Container, IsContained {
    Paragraph container;
    List<Phrase> phrases;
    String text;

    public Sentence() {
        phrases = new ArrayList<Phrase>();
    }

    public Sentence(boolean addDefaultPhrase) {
        this();
        addPhrase(new Phrase());
    }

    public Paragraph getContainer() {
        return container;
    }

    public void addPhrase(Phrase p) {
        p.container = this;
        phrases.add(p);
    }

    public List<Phrase> getPhrases() {
        return phrases;
    }

    public void addWord(Word w) {
        if (!phrases.isEmpty())
            phrases.get(phrases.size() - 1).addWord(w);
    }

    public List<Word> getWords() {
        List<Word> words = new ArrayList<Word>();
        for (Phrase ph : phrases)
            for (Word w : ph.words)
                words.add(w);
        return words;
    }


    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Sentence");
        if (text != null) {
            b.append("[" + text + "]");
        }
        b.append("\n");
        for (Phrase phrase : phrases) {
            b.append(phrase.toString());
        }
        return b.toString();
    }

    public List<Segment> getSegments() {
        List<Segment> segs = new ArrayList<Segment>();
        for (Phrase phrase : phrases) {
            for (Word w : phrase.words) {
                segs.addAll(w.getSegments());
            }
            if (phrase.boundary != null)
                segs.add(phrase.boundary);
        }
        return segs;
    }

    public List<Syllable> getSyllables() {
        List<Syllable> sylls = new ArrayList<Syllable>();
        for (Phrase phrase : phrases) {
            for (Word w : phrase.words) {
                sylls.addAll(w.syllables);
            }
        }
        return sylls;
    }

    public JSONArray toJSON() {
        JSONArray a = new JSONArray();
        for (Phrase p : phrases) {
            a.add(p.toJSON());
        }
        return a;
    }

    public void fromJSON(JSONArray a) {
        for (Object o : a) {
            Phrase p = new Phrase();
            p.fromJSON((JSONObject) o);
            addPhrase(p);
        }
    }


}
