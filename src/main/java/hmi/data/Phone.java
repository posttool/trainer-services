package hmi.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
        b.append("        Phone [" + text + "/" + begin + "-" + end + "]\n");
        return b.toString();
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.put("text", text);
        o.put("begin", begin);
        o.put("end", end);
        o.put("duration", duration);
        if (f0 != null && f0.length != 0) {
            JSONArray a = new JSONArray();
            for (float f : f0)
                a.add(f);
            o.put("f0", a);
        }
        return o;
    }

    public void fromJSON(JSONObject o) {
        text = (String) o.get("text");
        begin = (float) (double) o.get("begin");
        end = (float) (double) o.get("end");
        duration = (float) (double) o.get("duration");
        if (o.get("f0") != null) {
            JSONArray a = (JSONArray) o.get("f0");
            f0 = new float[a.size()];
            for (int i = 0; i < a.size(); i++) {
                f0[i] = (float) (double) a.get(i);
            }
        }
    }

}
