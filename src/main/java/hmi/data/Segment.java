package hmi.data;

import org.json.simple.JSONArray;

import java.util.List;

public abstract class Segment {
    public abstract float getBegin();
    public abstract float getEnd();
    public abstract float getDuration();

    @SuppressWarnings("unchecked")
    public <T> T getContainer(Class<T> clazz) {
        Object p = this;
        while (p instanceof IsContained) {
            Container c = ((IsContained) p).getContainer();
            if (c.getClass() == clazz) {
                return (T) c;
            }
            p = c;
        }
        return null;
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

    // PrevPrevSyllableNavigator
    // NextSyllableNavigator
    // NextNextSyllableNavigator
    // WordNavigator
    // LastSyllableInPhraseNavigator
    // NextWordNavigator
    // PrevWordNavigator
    // FirstSegmentNextWordNavigator
    // LastWordInSentenceNavigator



}
