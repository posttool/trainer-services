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

    public Boundary getBoundary() {
        return boundary;
    }

    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        JSONArray a = new JSONArray();
        o.put("words", a);
        for (Word w : words) {
            a.add(w.toJSON());
        }
        if (boundary != null)
            o.put("boundary", boundary.toJSON());
        return o;
    }

    public void fromJSON(JSONObject o) {
        JSONArray a = (JSONArray) o.get("words");
        for (Object wo : a) {
            Word w = new Word();
            w.fromJSON((JSONObject) wo);
            addWord(w);
        }
        if (o.get("boundary") != null) {
            Boundary b = new Boundary();
            b.fromJSON((JSONObject) o.get("boundary"));
        }
    }

}
