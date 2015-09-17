package hmi.data;

import java.util.List;

public abstract class Segment {
    public abstract String getPhone();

    public abstract float getBegin();

    public abstract void setBegin(float f);

    public abstract float getEnd();

    public abstract void setEnd(float f);

    public abstract float getDuration();

    public abstract void setDuration(float f);

    @SuppressWarnings("unchecked")
    public <T> T getContainer(Class<T> clazz) {
        Object p = this;
        while (p instanceof IsContained) {
            Container c = ((IsContained) p).getContainer();
            if (c == null)
                return null;
            if (c.getClass() == clazz) {
                return (T) c;
            }
            p = c;
        }
        return null;
    }

    public int fromSyllableEnd() {
        Syllable syllable = getContainer(Syllable.class);
        if (syllable == null || syllable.phones == null)
            return 0;
        else
            return syllable.phones.size() - syllable.phones.indexOf(this);
    }

    public int fromSyllableStart() {
        Syllable syllable = getContainer(Syllable.class);
        if (syllable == null || syllable.phones == null)
            return 0;
        else
            return syllable.phones.indexOf(this);
    }

    public int fromWordEnd() {
        Word word = getContainer(Word.class);
        if (word == null)
            return 0;
        List<Segment> wordSegs = word.getSegments();
        return wordSegs.size() - wordSegs.indexOf(this);
    }

    public int fromWordStart() {
        Word word = getContainer(Word.class);
        if (word == null)
            return 0;
        List<Segment> wordSegs = word.getSegments();
        return wordSegs.indexOf(this);
    }

    public Segment getPrevSegment() {
        Sentence sentence = getContainer(Sentence.class);
        if (sentence == null)
            return null;
        List<Segment> p = sentence.getSegments();
        int idx = p.indexOf(this);
        if (idx < 1)
            return null;
        return p.get(idx - 1);
    }

    public Segment getPrevPrevSegment() {
        Sentence sentence = getContainer(Sentence.class);
        if (sentence == null)
            return null;
        List<Segment> p = sentence.getSegments();
        int idx = p.indexOf(this);
        if (idx < 2)
            return null;
        return p.get(idx - 2);
    }

    public Segment getNextSegment() {
        Sentence sentence = getContainer(Sentence.class);
        if (sentence == null)
            return null;
        List<Segment> p = sentence.getSegments();
        int idx = p.indexOf(this);
        if (idx > p.size() - 2)
            return null;
        return p.get(idx + 1);
    }

    public Segment getNextNextSegment() {
        Sentence sentence = getContainer(Sentence.class);
        if (sentence == null)
            return null;
        List<Segment> p = sentence.getSegments();
        int idx = p.indexOf(this);
        if (idx > p.size() - 3)
            return null;
        return p.get(idx + 2);
    }

    public Segment getFirstSegmentInWord() {
        Word word = getContainer(Word.class);
        if (word == null)
            return null;
        List<Segment> p = word.getSegments();
        return p.get(0);
    }

    public Segment getLastSegmentInWord() {
        Word word = getContainer(Word.class);
        if (word == null)
            return null;
        List<Segment> p = word.getSegments();
        return p.get(p.size() - 1);
    }

    public Syllable getFirstSyllableInWord() {
        Word word = getContainer(Word.class);
        if (word == null)
            return null;
        return word.syllables.get(0);
    }

    public Syllable getLastSyllableInWord() {
        Word word = getContainer(Word.class);
        if (word == null)
            return null;
        return word.syllables.get(word.syllables.size() - 1);
    }

    public Syllable getSyllable() {
        return getContainer(Syllable.class);
    }

    public Syllable getPrevSyllable() {
        Sentence sentence = getContainer(Sentence.class);
        if (sentence == null)
            return null;
        List<Syllable> syllables = sentence.getSyllables();
        int idx = syllables.indexOf(getSyllable());
        if (idx < 1)
            return null;
        return syllables.get(idx - 1);
    }


}
