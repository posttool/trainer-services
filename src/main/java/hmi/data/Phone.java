package hmi.data;

import org.json.simple.JSONArray;

public class Phone extends Segment implements IsContained {
    Syllable container;
    String text;
    float begin;
    float end;
    float duration;
    float[] f0;


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

    public float getDuration() {
        return duration;
    }

    public void setDuration(float d) {
        this.duration = d;
    }

    public float getBegin() {
        return begin;
    }

    public void setBegin(float begin) {
        this.begin = begin;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    public float[] getF0() {
        return f0;
    }

    public void setF0(float[] f0) {
        this.f0 = f0;
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
