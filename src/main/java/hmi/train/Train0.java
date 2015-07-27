package hmi.train;

import hmi.phone.PhoneSet;
import hmi.util.Resource;

public class Train0 {
    public static void main(String... args) throws Exception {
        // JBW
        VoiceRepo repo = new VoiceRepo("jbw-vocc") ;
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        ASpeechMarkup asm = new ASpeechMarkup(repo);
        asm.compute();
        BAlign aligner = new BAlign(repo);
        aligner.compute(phoneSet);
        aligner.copyToSpeechMarkup();
        CInitHTS hts = new CInitHTS(repo);
        hts.init();
        hts.compute();
        DDataAll data = new DDataAll(repo);
        data.makeQuestions(phoneSet);
        data.makeLabels();
    }

}
