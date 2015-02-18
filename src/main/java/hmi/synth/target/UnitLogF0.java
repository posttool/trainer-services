package hmi.synth.target;

import hmi.data.Phone;
import hmi.data.Segment;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

public class UnitLogF0 /* implements FeatureProcessor */{
    public String getName() {
        return "UnitLogF0";
    }

    public float process(Target target) {
        return process(target, false);
    }

    protected float process(Target target, boolean delta) {
        // Note: all variables in this method with "f0" in their name
        // actually represent log f0 values.
        if (target instanceof DiphoneTarget) {
            DiphoneTarget diphone = (DiphoneTarget) target;
            return (process(diphone.left) + process(diphone.right)) / 2;
        }
        // Idea: find the closest f0 targets in the current syllable, left and
        // right of our middle;
        // linearly interpolate between them to find the value in the middle of
        // this unit.
        Segment seg = target.sm;
        if (seg == null) {
            return 0;
        }
        if (!(seg instanceof Phone)) {
            return 0;
        }
        // get mid position of segment wrt phone start (phone start = 0, phone
        // end = phone duration)
        float mid;
        float phoneDuration = getDuration(seg);
        if (target instanceof HalfPhoneTarget) {
            if (((HalfPhoneTarget) target).isLeftHalf()) {
                mid = .25f;
            } else {
                mid = .75f;
            }
        } else { // phone target
            mid = .5f;
        }

        // Now mid is the middle of the unit relative to the phone start, in
        // percent
        Float lastPos = null; // position relative to mid, in milliseconds
                              // (negative)
        float lastF0 = 0;
        Float nextPos = null; // position relative to mid, in milliseconds
        float nextF0 = 0;
        Float[] f0values = getLogF0Values(seg);

        assert f0values != null;
        // values are position, f0, position, f0, etc.;
        // position is in percent of phone duration between 0 and 1, f0 is in Hz
        for (int i = 0; i < f0values.length; i += 2) {
            float pos = f0values[i];
            if (pos <= mid) {
                lastPos = (pos - mid) * phoneDuration; // negative or zero
                lastF0 = f0values[i + 1];
            } else if (pos > mid) {
                nextPos = (pos - mid) * phoneDuration; // positive
                nextF0 = f0values[i + 1];
                break; // no point looking further to the right
            }
        }
        if (lastPos == null) { // need to look to the left
            float msBack = -mid * phoneDuration;
            Element e = seg;

            // get all phone units in the same phrase
            Element phraseElement = (Element) getAncestor(seg, PHRASE);
            TreeWalker tw = createTreeWalker(seg.getOwnerDocument(), phraseElement, PHONE);
            Element en;
            while ((en = (Element) tw.nextNode()) != null) {
                if (en == seg) {
                    break;
                }
            }

            while ((e = (Element) tw.previousNode()) != null) {
                float dur = getDuration(e);
                f0values = getLogF0Values(e);
                if (f0values.length == 0) {
                    msBack -= dur;
                    continue;
                }
                assert f0values.length > 1;
                float pos = f0values[f0values.length - 2];
                lastPos = msBack - (1 - pos) * dur;
                lastF0 = f0values[f0values.length - 1];
                break;
            }
        }

        if (nextPos == null) { // need to look to the right
            float msForward = (1 - mid) * phoneDuration;
            Element e = seg;

            // get all phone units in the same phrase
            Element phraseElement = (Element) getAncestor(seg, PHRASE);
            TreeWalker tw = createTreeWalker(seg.getOwnerDocument(), phraseElement, PHONE);
            Element en;
            while ((en = (Element) tw.nextNode()) != null) {
                if (en == seg) {
                    break;
                }
            }

            while ((e = (Element) tw.nextNode()) != null) {
                float dur = getDuration(e);
                f0values = getLogF0Values(e);
                if (f0values.length == 0) {
                    msForward += dur;
                    continue;
                }
                assert f0values.length > 1;
                float pos = f0values[0];
                nextPos = msForward + pos * dur;
                nextF0 = f0values[1];
                break;
            }
        }

        if (lastPos == null && nextPos == null) {
            // no info
            return 0;
        } else if (lastPos == null) {
            // have only nextF0;
            if (delta)
                return 0;
            else
                return nextF0;
        } else if (nextPos == null) {
            // have only lastF0
            if (delta)
                return 0;
            else
                return lastF0;
        }
        assert lastPos <= 0 && 0 <= nextPos : "unexpected: lastPos=" + lastPos + ", nextPos=" + nextPos;
        // build a linear function (f(x) = slope*x+intersectionYAxis)
        float f0;
        float slope;
        if (lastPos - nextPos == 0) {
            f0 = (lastF0 + nextF0) / 2;
            slope = 0;
        } else {
            slope = (nextF0 - lastF0) / (nextPos - lastPos);
            // calculate the pitch
            f0 = lastF0 + slope * (-lastPos);
        }
        assert !Float.isNaN(f0) : "f0 is not a number";
        assert lastF0 <= f0 && nextF0 >= f0 || lastF0 >= f0 && nextF0 <= f0 : "f0 should be between last and next values";

        if (delta)
            return slope;
        else
            return f0;
    }

    private Float[] getLogF0Values(Element ph) {
        String mbrTargets = ph.getAttribute("f0");
        if (mbrTargets.equals("")) {
            return new Float[0];
        }
        ArrayList<Float> values = new ArrayList<Float>();
        try {
            // mbrTargets contains one or more pairs of numbers,
            // either enclosed by (a,b) or just separated by whitespace.
            StringTokenizer st = new StringTokenizer(mbrTargets, " (,)");
            while (st.hasMoreTokens()) {
                String posString = "";
                while (st.hasMoreTokens() && posString.equals(""))
                    posString = st.nextToken();
                String f0String = "";
                while (st.hasMoreTokens() && f0String.equals(""))
                    f0String = st.nextToken();

                float pos = Float.parseFloat(posString) * 0.01f;
                assert 0 <= pos && pos <= 1 : "invalid position:" + pos + " (pos string was '" + posString
                        + "' coming from '" + mbrTargets + "')";
                float f0 = Float.parseFloat(f0String);
                float logF0 = (float) Math.log(f0);
                values.add(pos);
                values.add(logF0);
            }
        } catch (Exception e) {
            return new Float[0];
        }
        return values.toArray(new Float[0]);
    }

    private float getDuration(Element ph) {
        float phoneDuration = 0;
        String sDur = ph.getAttribute("d");
        if (!sDur.equals("")) {
            try {
                phoneDuration = Float.parseFloat(sDur);
            } catch (NumberFormatException nfe) {
            }
        }
        return phoneDuration;
    }
}