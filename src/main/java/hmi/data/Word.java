package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Word implements Container, IsContained {
    Phrase container;
    List<Syllable> syllables;
    String text;
    String accent;
    String g2p_method;
    String ph;
    private String pos;
    private String entity;
    private int depth;

    public Word() {
        syllables = new ArrayList<Syllable>();
    }

    public Word(String text) {
        this();
        this.text = text;
    }

    public Phrase getContainer() {
        return container;
    }

    public void addSyllable(Syllable s) {
        s.container = this;
        syllables.add(s);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("    Word");
        if (text != null) {
            b.append(" [" + text);
            if (pos != null)
                b.append("/" + pos);
            if (entity != null)
                b.append("/" + entity);
            // if (ph != null)
            // b.append("/" + ph);
            b.append("]\n");
        }
        for (Syllable syllable : syllables) {
            b.append(syllable.toString());
        }
        return b.toString();
    }

    public List<Syllable> getSyllables() {
        return syllables;
    }

    public List<Segment> getSegments() {
        List<Segment> segs = new ArrayList<Segment>();
        for (Syllable s : syllables) {
            for (Phone p : s.phones) {
                segs.add(p);
            }
        }
        return segs;
    }

    public String getPartOfSpeech() {
        return pos;
    }

    public void setPartOfSpeech(String pos) {
        this.pos = pos;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int d) {
        this.depth = d;
    }

    public String getPh() {
        return ph;
    }

    public void setPh(String phs) {
        this.ph = phs;
    }

    public String getG2P() {
        return g2p_method;
    }

    public void setG2P(String g2p) {
        this.g2p_method = g2p;
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        this.text = t;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String t) {
        this.entity = t;
    }

    public boolean isVoiced() {
        return ph != null && !text.equals(ph); // transcriptions that match the
        // text are not voiced
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.put("text", text);
        o.put("pos", pos);
        if (entity != null && !entity.equals("O"))
            o.put("entity", entity);
        if (!syllables.isEmpty()) {
            JSONArray a = new JSONArray();
            for (Syllable s : syllables) {
                a.add(s.toJSON());
            }
            o.put("syllables", a);
        }
        return o;
    }

    public void fromJSON(JSONObject o) {
        Word w = new Word();
        w.text = (String) o.get("text");
        w.pos = (String) o.get("text");
        w.entity = (String) o.get("text");
        JSONArray sylls = (JSONArray) o.get("syllables");
        if (sylls != null) {
            for (Object so : sylls) {
                Syllable s = new Syllable();
                s.fromJSON((JSONArray) so);
                addSyllable(s);
            }
        }
    }


}
