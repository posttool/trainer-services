package hmi.features;

import hmi.data.Phone;
import hmi.data.Segment;

import java.util.ArrayList;
import java.util.List;

public class SegmentFeatures {

    Segment seg;
    List<FeatureValue> values;

    public SegmentFeatures(Segment seg) {
        this.seg = seg;
        values = new ArrayList<>();
    }

    public void add(FeatureValue fv) {
        values.add(fv);
    }

    public Segment getSegment() {
        return seg;
    }

    public void setSegment(Segment seg) {
        this.seg = seg;
    }

    public List<FeatureValue> getValues() {
        return values;
    }

    public void setValues(List<FeatureValue> values) {
        this.values = values;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        if (seg instanceof Phone) {
            Phone p = (Phone) seg;
            b.append(p.getBegin() + " " + p.getEnd() + " " + p.getPhone());
        } else {
            b.append("boundary");
        }
        b.append(" ");
        for (FeatureValue fv : values)
            b.append(fv.name + "=" + fv.value + " ");
        return b.toString();
    }
}
