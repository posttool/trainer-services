package hmi.synth.target;

import hmi.data.Segment;

public class HalfPhoneTarget extends Target {
    protected boolean isLeftHalf;

    public HalfPhoneTarget(String name, Segment s, boolean isLeftHalf) {
        super(name, s);
        this.isLeftHalf = isLeftHalf;
    }

    public boolean isLeftHalf() {
        return isLeftHalf;
    }

    public boolean isRightHalf() {
        return !isLeftHalf;
    }

}
