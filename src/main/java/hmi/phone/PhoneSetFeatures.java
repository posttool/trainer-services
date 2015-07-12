package hmi.phone;


import hmi.data.Boundary;
import hmi.data.Phone;
import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.phone.PhoneEl;
import hmi.phone.PhoneSet;

import java.io.FileWriter;
import java.util.*;

public class PhoneSetFeatures {
    Map<String, PhoneEl> dict;
    PhoneSet phoneSet;
    String[] features = new String[]{"cvox", "ctype", "vrnd", "vlng", "cplace", "vheight", "vc", "vfront", "isTone"};
    Map<String, Map<String, Set<String>>> feat_val_phs;

    public PhoneSetFeatures(PhoneSet phoneSet) {
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
                    System.out.println("NO  " + k);
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
        return feat_val_phs.get(fea).keySet();
    }

    public Set<String> getPhonesForFeatureValue(String fea, String val) {
        return feat_val_phs.get(fea).get(val);
    }





}
