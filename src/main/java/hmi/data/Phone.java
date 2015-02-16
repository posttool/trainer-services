package hmi.data;

public class Phone extends Segment implements IsContained {
    Syllable container;
    String text;
    int d;
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

    public float getDuration() {
        return d;
    }

    public Syllable getContainer() {
        return container;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("        Phone [" + text + "]\n");
        return b.toString();
    }

}
