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

    public String fullLabels() {
        StringBuilder b = new StringBuilder();
        Segment pp = seg.getPrevPrevSegment();
        Segment p = seg.getPrevSegment();
        Segment n = seg.getNextSegment();
        Segment nn = seg.getNextNextSegment();
        b.append(seg.getBegin() * 1E7);
        b.append(" ");
        b.append(seg.getEnd() * 1E7);
        b.append(" ");
        b.append(pp != null ? pp.getPhone() : "_");
        b.append("^");
        b.append(p != null ? p.getPhone() : "_");
        b.append("-");
        b.append(seg.getPhone());
        b.append("+");
        b.append(n != null ? n.getPhone() : "_");
        b.append("=");
        b.append(nn != null ? nn.getPhone() : "_");
        b.append("|");
        for (FeatureValue fv : values) {
            if (fv.hasValue())
                b.append("|" + fv.getName() + "=" + fv.getValue());
        }
        b.append("||\n");
        return b.toString();
    }
}
