package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

    public JSONObject toJSON(){
        JSONObject o = new JSONObject();
        JSONArray a = new JSONArray();
        for (Sentence s : sentences) {
            a.add(s.toJSON());
        }
        o.put("sentences", a);
        return o;
    }

    public void fromJSON(JSONObject a) {
        for (Object o : (JSONArray) a.get("sentences")) {
            Sentence s = new Sentence();
            s.fromJSON((JSONObject) o);
            addSentence(s);
        }
    }



}
