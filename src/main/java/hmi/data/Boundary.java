package hmi.data;

import org.json.simple.JSONObject;

public class Boundary extends Segment implements IsContained {
    Phrase container;
    int breakIndex;
    float begin;
    float end;
    float duration;

    public Boundary() {
    }

    public Phrase getContainer() {
        return container;
    }

    public String getPhone() {
        return "_"; // should come from somewhere
    }

    public void setDuration(float newDuration) {
        this.duration = newDuration;
    }

    public float getDuration() {
        return duration;
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

    public int getBreakIndex() {
        return breakIndex;
    }

    public void setBreakIndex(int breakIndex) {
        this.breakIndex = breakIndex;
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.put("duration", duration);
        o.put("breakIndex", breakIndex);
        return o;
    }

    public void fromJSON(JSONObject o) {
        duration = (float) (double) o.get("duration");
        breakIndex = (int) (long) o.get("breakIndex");
    }

}
