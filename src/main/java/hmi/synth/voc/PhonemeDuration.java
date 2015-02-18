package hmi.synth.voc;

public class PhonemeDuration {
    private String phone;
    private float duration;

    public PhonemeDuration(String ph, float dur) {
        phone = ph;
        duration = dur;
    }

    public void setPhoneme(String str) {
        phone = str;
    }

    public void setDuration(float fval) {
        duration = fval;
    }

    public String getPhoneme() {
        return phone;
    }

    public float getDuration() {
        return duration;
    }

}