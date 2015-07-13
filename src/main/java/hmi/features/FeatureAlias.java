package hmi.features;


import java.util.HashMap;
import java.util.Map;

public class FeatureAlias {
    int c = 0;
    Map<String, String> featsToAlias;
    Map<String, String> aliasToFeats;

    public FeatureAlias() {
        c = 0;
        featsToAlias = new HashMap<>();
        aliasToFeats = new HashMap<>();
    }

    public void add(String feat) {
        String alias = "f" + c;
        c++;
        featsToAlias.put(feat, alias);
        aliasToFeats.put(alias, feat);
    }

    public String getAlias(String feat) {
        return featsToAlias.get(feat);
    }

    public String getFeature(String alias) {
        return aliasToFeats.get(alias);
    }

    // TODO serialize
}
