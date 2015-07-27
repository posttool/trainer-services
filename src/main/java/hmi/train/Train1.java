package hmi.train;

import hmi.phone.PhoneSet;
import hmi.util.Resource;

public class Train1 {
    public static void main(String... args) throws Exception {
        // XXX
        VoiceRepo repo = new VoiceRepo("xxx-voc-a");
        PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
        //
        CInitHTS hts = new CInitHTS(repo);
        hts.installHTS();
        hts.addRawAudio();
        hts.compute();
        //
        DDataAll data = new DDataAll(repo);
        data.compute(phoneSet);
    }

}
