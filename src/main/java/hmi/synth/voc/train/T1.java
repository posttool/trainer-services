package hmi.synth.voc.train;

import java.util.Properties;

public class T1 {
    public static void main(String[] args) throws Exception {
        String r = "/Users/posttool/Documents/github";
        String v = "/voice";
        Properties p = new Properties();
        p.setProperty(HTKLabeler.LOCALE, "en_US");
        p.setProperty(HTKLabeler.HTKDIR, r + "/marytts-pt/lib/external/bin");
        p.setProperty(HTKLabeler.ALLOPHONES, r + "/la/src/test/resources/en_US/phones.xml");
        p.setProperty(HTKLabeler.WAVDIR, r + v + "/wav");
        p.setProperty(HTKLabeler.WAVEXT, ".wav");
        p.setProperty(HTKLabeler.PROMPTALLOPHONESDIR, r + v + "/prompt_allophones");
        p.setProperty(HTKLabeler.HTDIR, r + v + "/htk");
        p.setProperty(HTKLabeler.OUTLABDIR, r + v + "/htk/lab");
        p.setProperty(HTKLabeler.MAXITER, "150");
        p.setProperty(HTKLabeler.MAXSPITER, "10");
        p.setProperty(HTKLabeler.AWK, "/usr/bin/awk");
        HTKLabeler l = new HTKLabeler(p);
        l.compute();

    }
}
