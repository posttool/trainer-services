package hmi.data;

import org.json.simple.JSONArray;

public class Phone extends Segment implements IsContained {
    Syllable container;
    String text;
    float d;
    float end;
    String f0;

    public Phone() {
    }

    public Phone(String ph) {
        this.text = ph;
    }

    public String getPhone() {
        return text;
    }

    public String name() {
        return text;
    }

    public void setPhone(String ph) {
        this.text = ph;
    }

    public Syllable getContainer() {
        return container;
    }

    public void setDuration(float newDuration) {
        this.d = newDuration;
    }

    public float getDuration() {
        return d;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("        Phone [" + text + "]\n");
        return b.toString();
    }

    public String toJSON() {
        return text;
    }

    public void fromJSON(String s) {
        text = s;
    }

}
