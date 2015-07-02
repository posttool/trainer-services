package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Document implements Container {
    List<Paragraph> paragraphs;
    Paragraph currentParagraph;

    public Document() {
        paragraphs = new ArrayList<Paragraph>();
    }

    public JSONArray toJSON(){
        JSONArray a = new JSONArray();
        for (Paragraph p : paragraphs) {
            a.add(p.toJSON());
        }
        return a;
    }

}
