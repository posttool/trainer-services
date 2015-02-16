package hmi.data;

public class SpeechMarkupTest {

    public static void main(String[] args) {
        SpeechMarkup sm = new SpeechMarkup();

        Sentence sentence = new Sentence();
        sentence.text = "Welcome to the world  ...";
        sm.addSentence(sentence);
        Phrase phrase = new Phrase();
        phrase.prosodyPitch = "+3%";
        phrase.prosodyRange = "+13%";
        sentence.addPhrase(phrase);
        Word w = new Word();
        w.text = "Welcome";
        w.setPos("VB");
        w.ph = "' w E l - k @ m";
        w.accent = "H*";
        phrase.addWord(w);
        Syllable syl = new Syllable();
        syl.text = "w E l";
        w.addSyllable(syl);
        Phone phone = new Phone();
        phone.d = 124;
        phone.end = 0.124012f;
        phone.f0 = "(0,105)";
        phone.text = "w";
        syl.addPhone(phone);
        phone = new Phone();
        phone.d = 52;
        phone.end = 0.1763529f;
        phone.f0 = "(50,115)";
        phone.text = "E";
        syl.addPhone(phone);
        phone = new Phone();
        phone.d = 73;
        phone.end = 0.24885291f;
        phone.f0 = "(100,131)";
        phone.text = "l";
        syl.addPhone(phone);
        syl = new Syllable();
        syl.text = "k @";
        w.addSyllable(syl);
        syl.addPhone(new Phone("k"));
        syl.addPhone(new Phone("@"));

        phrase.addWord(new Word("to"));
        phrase.addWord(new Word("the"));
        phrase.addWord(new Word("world"));
        phrase.addWord(new Word("..."));

        System.out.println(sm.toString());

        System.out.println("first syllable in word" + phone.getFirstSyllableInWord());
        System.out.println("last syllable in word" + phone.getLastSyllableInWord());

    }

    private static void ACC_STACK(SpeechMarkup sm) {
        String input = "One two three. Four five or six?";
        for (String s : SENTENCE_TOKENIZER(input)) {
            Sentence sentence = new Sentence();
            sm.addSentence(sentence);
            for (String p : PHRASE_TOKENIZER(s)) {
                Phrase phrase = new Phrase();
                phrase.text = p;
                sentence.addPhrase(phrase);
                for (String w : WORD_TOKENIZER(p)) {
                    Word word = new Word();
                    word.text = w;
                    phrase.addWord(word);
                }
            }
            ADD_PHONETIC_TRANSCRIPTION_TO_WORDS(sentence);
            ADD_PROSODY_TO_PHRASES(sentence);
            SYLLABILIFY_WORDS(sentence);// pronunciation
            ADD_ACCOUSTIC_INFO_TO_PHONES(sentence);
        }
    }

    private static void ADD_ACCOUSTIC_INFO_TO_PHONES(Sentence sentence) {
        // TODO Auto-generated method stub

    }

    private static void SYLLABILIFY_WORDS(Sentence sentence) {
        // TODO Auto-generated method stub

    }

    private static void ADD_PROSODY_TO_PHRASES(Sentence sentence) {
        // TODO Auto-generated method stub

    }

    private static void ADD_PHONETIC_TRANSCRIPTION_TO_WORDS(Sentence sentence) {
        // TODO Auto-generated method stub

    }

    private static String[] WORD_TOKENIZER(String p) {
        // TODO Auto-generated method stub
        return null;
    }

    private static String[] PHRASE_TOKENIZER(String s) {
        // TODO Auto-generated method stub
        return null;
    }

    private static String[] SENTENCE_TOKENIZER(String string) {
        // TODO Auto-generated method stub
        return null;
    }

}
