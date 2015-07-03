package hmi.data;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Phrase implements Container, IsContained {
    Sentence container;
    List<Word> words;
    Boundary boundary;
    String text;
    String prosodyRange;
    String prosodyPitch;

    public Phrase() {
        words = new ArrayList<Word>();
    }

    public Sentence getContainer() {
        return container;
    }

    public void addWord(Word w) {
        w.container = this;
        words.add(w);
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("  Phrase");
        if (text != null) {
            b.append("[" + text + "]");
        }
        b.append("\n");
        for (Word word : words) {
            b.append(word.toString());
        }
        return b.toString();
    }

    public List<Word> getWords() {
        return words;
    }

    public Word getLastWord() {
        return words.get(words.size() - 1);
    }

    public JSONArray toJSON() {
        JSONArray a = new JSONArray();
        for (Word w : words) {
            a.add(w.toJSON());
        }
        return a;
    }

    public void fromJSON(JSONArray a) {
        for (Object o : a) {
            Word w = new Word();
            w.fromJSON((JSONObject) o);
            addWord(w);
        }
    }

}
