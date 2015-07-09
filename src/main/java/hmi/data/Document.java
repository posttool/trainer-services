package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Document implements Container {
    List<Paragraph> paragraphs;

    public Document() {
        paragraphs = new ArrayList<Paragraph>();
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        JSONArray a = new JSONArray();
        for (Paragraph p : paragraphs) {
            a.add(p.toJSON());
        }
        o.put("paragraphs", a);
        return o;
    }

    public void fromJSON(JSONObject a) {
        for (Object o : (JSONArray) a.get("paragraphs")) {
            Paragraph p = new Paragraph();
            p.fromJSON((JSONObject) o);
            p.container = this;
            paragraphs.add(p);
        }
    }

}
