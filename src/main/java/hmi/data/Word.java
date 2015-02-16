package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Word implements Container, IsContained {
    Phrase container;
    List<Syllable> syllables;
    String text;
    String accent;
    String g2p_method;
    String ph;
    private String pos;

    public Word() {
        syllables = new ArrayList<Syllable>();
    }

    public Word(String text) {
        this();
        this.text = text;
    }

    public Phrase getContainer() {
        return container;
    }

    public void addSyllable(Syllable s) {
        s.container = this;
        syllables.add(s);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("    Word");
        if (text != null) {
            b.append(" [" + text);
            if (pos != null) {
                b.append("/" + pos);
            }
            b.append("]\n");
        }
        for (Syllable syllable : syllables) {
            b.append(syllable.toString());
        }
        return b.toString();
    }

    public List<Segment> getSegments() {
        List<Segment> segs = new ArrayList<Segment>();
        for (Syllable s : syllables) {
            for (Phone p : s.phones) {
                segs.add(p);
            }
        }
        return segs;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

}
