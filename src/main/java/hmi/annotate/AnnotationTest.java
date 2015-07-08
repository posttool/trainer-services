package hmi.annotate;

import hmi.data.SpeechMarkup;

public class AnnotationTest {
    static String S = "This is one. Tis is two. How do you do, if you don't mind me asking? "
            + "Furthermore, it stands to reason that I wouldn't use a comma here but would in San Francisco, California. "
            + "Why would you eat out when you could eat on Mars? A sentence without a break -- like this one here -- "
            + "is a weird sentence.\n\nThis is even more material for the test.";

    public static void main(String[] args) throws Exception {
        SpeechMarkupAnnotater annotater = new SpeechMarkupAnnotater("en_US");
        SpeechMarkup sm = annotater.annotate(S);
        System.out.println(sm);
    }
}
