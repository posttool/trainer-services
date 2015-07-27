package hmi.features;


import hmi.data.*;

import java.util.*;

public class SpeechMarkupFeatures {
    static String[] features = {
            "phrase_numsyls",
            "phrase_numwords",
            "sentence_numwords",
            "sentence_numphrases",
            "segs_from_syl_end",
            "segs_from_syl_start",
            "segs_from_word_end",
            "segs_from_word_start",
            "sentence_numphrases",
            "sentence_numwords",
            "sentence_punc",
            "syl_break",
            "syl_numsegs",
            "syls_from_phrase_end",
            "syls_from_phrase_start",
            "syls_from_word_end",
            "syls_from_word_start",
            "word_numsegs",
            "word_numsyls",
            "words_from_phrase_end",
            "words_from_phrase_start",
            "words_from_prev_punctuation",
            "words_from_sentence_end",
            "words_from_sentence_start",
            "words_to_next_punctuation"
    };
    FeatureAlias fa;

    public SpeechMarkupFeatures(FeatureAlias fa) {
        this.fa = fa;
        for (String f : features)
            fa.add(f);
    }

    public String[] getFeatures() {
        return features;
    }

    public String getFeatureValue(String feat, Segment seg) {
        Syllable syllable = seg.getContainer(Syllable.class);
        Word word = seg.getContainer(Word.class);
        Phrase phrase = seg.getContainer(Phrase.class);
        Sentence sentence = seg.getContainer(Sentence.class);
        switch (feat) {
            case "phrase_numsyls":
                return str(phrase.getSyllables().size());
            case "phrase_numwords":
                return str(phrase.getWords().size());
            case "sentence_numwords":
                return str(sentence.getWords().size());
            case "sentence_numphrases":
                return str(sentence.getPhrases().size());
            case "segs_from_syl_end":
                return str(seg.fromSyllableEnd());
            case "segs_from_syl_start":
                return str(seg.fromSyllableStart());
            case "segs_from_word_end":
                return str(seg.fromWordEnd());
            case "segs_from_word_start":
                return str(seg.fromWordStart());
        }
        return "";
    }

    public List<SegmentFeatures> getFeatures(SpeechMarkup sm) {
        List<Segment> segs = sm.getSegments();
        List<SegmentFeatures> sfs = new ArrayList<>();
        for (Segment seg : segs) {
            SegmentFeatures sf = new SegmentFeatures(seg);
            for (int i = 0; i < features.length; i++) {
                sf.add(new FeatureValue(fa.getAlias(features[i]), getFeatureValue(features[i], seg)));
            }
            sfs.add(sf);
        }
        return sfs;
    }

    public String str(Number n) {
        return n.toString();
    }

    //hts/data/questions/questions_qst001.hed
    public String questions_qst001_hed() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            String feaAlias = fa.getAlias(features[i]);
            Set<String> val_fea = getNumbers();
            for (String val : val_fea) {
                b.append("QS \"" + feaAlias + "=" + val + "\" \t{*|" + feaAlias + "=" + val + "|*}\n");
            }
            b.append("\n");
        }

        return b.toString();
    }


    //hts/data/questions/questions_utt_qst001.hed
    public String questions_utt_qst001_hed() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String feaAlias = features[i];
            Set<String> val_fea = getNumbers();
            for (String val : val_fea) {
                b.append("QS \"" + feaAlias + "=" + val + "\" \t{*|" + feaAlias + "=" + val + "|*}\n");
            }
            b.append("\n");
        }

        return b.toString();
    }

    private Set<String> getNumbers() {
        Set<String> s = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            s.add(i + "");
        }
        return s;
    }



}
