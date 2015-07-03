package hmi.data;

import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class Paragraph implements Container, IsContained {
    Document container;
    List<Sentence> sentences;

    public Paragraph() {
        sentences = new ArrayList<Sentence>();
    }

    public Document getContainer() {
        return container;
    }

    public void addSentence(Sentence s) {
        s.container = this;
        sentences.add(s);
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public boolean isEmpty() {
        return sentences.isEmpty();
    }

    public JSONArray toJSON(){
        JSONArray a = new JSONArray();
        for (Sentence s : sentences) {
            a.add(s.toJSON());
        }
        return a;
    }

    public void fromJSON(JSONArray a) {
        for (Object o : a) {
            Sentence s = new Sentence();
            s.fromJSON((JSONArray) o);
            addSentence(s);
        }
    }



}
