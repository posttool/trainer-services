package hmi.synth.voc.train;


import hmi.data.SpeechMarkup;
import hmi.data.VoiceRepo;
import hmi.features.FeatureAlias;
import hmi.features.PhoneFeatures;
import hmi.features.SegmentFeatures;
import hmi.features.SentenceFeatures;
import hmi.phone.PhoneSet;
import hmi.util.Resource;

import java.util.List;

public class FeatureTest {

    public static void main(String... args) throws Exception {
        PhoneSet ps = new PhoneSet(Resource.path("/en_US/phones.xml"));
        VoiceRepo repo = new VoiceRepo("jbw-vocb");
        SpeechMarkup sm = repo.getSpeechMarkup(0);
        FeatureAlias fa = new FeatureAlias();
        //
        PhoneFeatures fp = new PhoneFeatures(ps);
        for (String f : fp.getFeatures())
                fa.add(f);
//        String qst = fp.get_questions_qst001_he

//        System.out.println(qst);
        SentenceFeatures sf = new SentenceFeatures(fa);
//        String qst2 = sf.questions_qst001_hed();
//        System.out.println(qst2);
        List<SegmentFeatures> segfs = sf.getFeatures(sm);
        for (SegmentFeatures segfeats : segfs) {
            System.out.println(segfeats);
        }
        //
//        String utt_qst = sf.questions_utt_qst001_hed();
//        System.out.println(utt_qst);
    }


}
