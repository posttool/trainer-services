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
    private int depth;

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
            if (pos != null)
                b.append("/" + pos);
            // if (ph != null)
            // b.append("/" + ph);
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

    public int getDepth() {
        return depth;
    }

    public void setDepth(int d) {
        this.depth = d;
    }

    public String getPh() {
        return ph;
    }

    public void setPh(String phs) {
        this.ph = phs;
    }

    public String getG2P() {
        return g2p_method;
    }

    public void setG2P(String g2p) {
        this.g2p_method = g2p;
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        this.text = t;
    }

    public boolean isVoiced() {
        return ph != null && !text.equals(ph); // transcriptions that match the text are not voiced
    }

}
