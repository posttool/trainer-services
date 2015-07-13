package hmi.features;


import hmi.phone.PhoneEl;
import hmi.phone.PhoneSet;

import java.util.*;

public class PhoneFeatures {
    Map<String, PhoneEl> dict;
    PhoneSet phoneSet;
    String[] features = new String[]{"cvox", "ctype", "vrnd", "vlng", "cplace", "vheight", "vc", "vfront", "isTone"};
    Map<String, Map<String, Set<String>>> feat_val_phs;

    public PhoneFeatures(PhoneSet phoneSet) {
        this.dict = new HashMap<>();
        this.phoneSet = phoneSet;
        this.feat_val_phs = new HashMap<>();
        for (String feat : features) {
            feat_val_phs.put(feat, new HashMap<>());
        }
        Map<String, PhoneEl> pels = phoneSet.getPhoneEls();
        for (PhoneEl phoneEl : pels.values()) {
            Map<String, String> feats = phoneEl.getFeatures();
            for (String k : feats.keySet()) {
                String v = feats.get(k);
                if (v.equals("0"))
                    continue;
                Map<String, Set<String>> val_phs = feat_val_phs.get(k);
                if (val_phs == null) {
                    System.out.println("missing feature " + k);
                    continue;
                }
                Set<String> s;
                if (!val_phs.containsKey(v)) {
                    s = new HashSet<>();
                    val_phs.put(v, s);
                } else {
                    s = val_phs.get(v);
                }
                if (!s.contains(phoneEl.getPhone())) {
                    s.add(phoneEl.getPhone());
                }
            }
        }
    }

    public String[] getFeatures() {
        return features;
    }

    public Set<String> getValuesForFeature(String fea) {
        if (!feat_val_phs.containsKey(fea))
            throw new RuntimeException("No such feature [" + fea + "]");
        return feat_val_phs.get(fea).keySet();
    }

    public Set<String> getPhonesForFeatureValue(String fea, String val) {
        if (!feat_val_phs.containsKey(fea))
            throw new RuntimeException("No such feature [" + fea + "]");
        return feat_val_phs.get(fea).get(val);
    }

    //hts/data/questions/questions_qst001.hed
    public String get_questions_qst001_hed() {
        StringBuilder b = new StringBuilder();
        for (PhoneEl phoneEl : phoneSet.getPhoneEls().values()) {
            b.append(qed0(phoneEl.getPhone()));
        }
        for (String fea : getFeatures()) {
            for (String val : getValuesForFeature(fea)) {
                Set<String> phs = getPhonesForFeatureValue(fea, val);
                b.append(qed1(fea, val, phs));
            }
        }
        return b.toString();
    }

    private static String qed0(String ph) {
        StringBuilder b = new StringBuilder();
        b.append("QS \"prev_prev_phone=" + ph + "\"\t{" + ph + "^*}\n");
        b.append("QS \"prev_phone=" + ph + "\"\t\t{*^" + ph + "-*}\n");
        b.append("QS \"phone=" + ph + "\"\t\t\t{*-" + ph + "+*}\n");
        b.append("QS \"next_phone=" + ph + "\"\t\t{*+" + ph + "=*}\n");
        b.append("QS \"next_next_phone=" + ph + "\"\t{*=" + ph + "||*}\n\n");
        return b.toString();
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
