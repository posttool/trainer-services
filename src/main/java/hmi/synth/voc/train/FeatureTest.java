package hmi.synth.voc.train;


import hmi.data.VoiceRepo;
import hmi.phone.PhoneEl;
import hmi.phone.PhoneSetFeatures;
import hmi.phone.PhoneSet;
import hmi.util.Resource;

import java.util.Iterator;
import java.util.Set;

public class FeatureTest {


    public static void main(String... args) throws Exception {
        VoiceRepo root = new VoiceRepo("jbw-vocb");
        PhoneSet ps = new PhoneSet(Resource.path("/en_US/phones.xml"));
        PhoneSetFeatures fp = new PhoneSetFeatures(ps);
        for (PhoneEl phoneEl : ps.getPhoneEls().values()) {
            String ph = phoneEl.getPhone();
            System.out.println("QS \"prev_prev_phone=" + ph + "\"\t{" + ph + "^*}\n");
            System.out.println("QS \"prev_phone=" + ph + "\"\t\t{*^" + ph + "-*}\n");
            System.out.println("QS \"phone=" + ph + "\"\t\t\t{*-" + ph + "+*}\n");
            System.out.println("QS \"next_phone=" + ph + "\"\t\t{*+" + ph + "=*}\n");
            System.out.println("QS \"next_next_phone=" + ph + "\"\t{*=" + ph + "||*}\n");
            System.out.println("\n");
        }
        for (String fea : fp.getFeatures()) {
            for (String val : fp.getValuesForFeature(fea)) {
                Set<String> phs = fp.getPhonesForFeatureValue(fea, val);
                System.out.println(qed1(fea, val, phs));
            }
        }
    }

    private static String qed1(String fea, String fval, Set<String> values) {
        String val, prev_prev, prev, ph, next, next_next;
        prev_prev = "QS \"prev_prev_" + fea + "=" + fval + "\"\t\t{";
        prev = "QS \"prev_" + fea + "=" + fval + "\"\t\t\t{";
        ph = "QS \"ph_" + fea + "=" + fval + "\"\t\t\t{";
        next = "QS \"next_" + fea + "=" + fval + "\"\t\t\t{";
        next_next = "QS \"next_next_" + fea + "=" + fval + "\"\t\t{";
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            val = it.next();
            prev_prev += val + "^*,";
            prev += "*^" + val + "-*,";
            ph += "*-" + val + "+*,";
            next += "*+" + val + "=*,";
            next_next += "*=" + val + "||*,";
        }
        return prev_prev.substring(0, prev_prev.lastIndexOf(",")) + "}\n" +
                prev.substring(0, prev.lastIndexOf(",")) + "}\n" +
                ph.substring(0, ph.lastIndexOf(",")) + "}\n" +
                next.substring(0, next.lastIndexOf(",")) + "}\n" +
                next_next.substring(0, next_next.lastIndexOf(",")) + "}\n" +
                "\n";
    }
}
