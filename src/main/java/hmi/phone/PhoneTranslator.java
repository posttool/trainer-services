package hmi.phone;


import hmi.util.FileUtils;
import hmi.util.Resource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneTranslator {
    String[] types;
    Map<String, String[]> map;
    boolean spaced = true;

    public PhoneTranslator(String filename) throws IOException {
        String ms = FileUtils.getFile(new File(Resource.path(filename)));
        String[] mss = ms.split("\n");
        types = mss[0].split("\t");
        map = new HashMap<>();
        for (int i = 1; i < mss.length; i++) {
            String[] c = mss[i].split("\t");
            for (int j = 0; j < c.length; j++) {
                String id = j + c[j];
                map.put(id, c);
            }
        }
    }

    public int find(String name) {
        for (int i = 0; i < types.length; i++) {
            if (name.equalsIgnoreCase(types[i]))
                return i;
        }
        return -1;
    }

    Pattern p = Pattern.compile("/([A-Z][A-Z])([0-3])/");

    public String translate(String line, String from, String to) {
        int in_idx = find(from);
        int out_idx = find(to);
        StringBuilder ls = new StringBuilder();
        String[] phs = line.split(" ");
        for (String s : phs) {
            String accent = null;
            Matcher m = p.matcher(s);
            if (m.find())
                s = m.group();
            if (m.find())
                accent = m.group();

            String[] row = map.get(in_idx + s);
            if (spaced && ls.length() != 0 && ls.charAt(ls.length() - 1) != ' ')
                ls.append(" ");
            if (row == null) {
                ls.append(s);
            } else {
                if (out_idx >= row.length) {
                    System.out.println("ERR " + row.length + " < " + out_idx + " [" + to + "]");
                } else {
                    String ts = row[out_idx];
                    //if (accent)
                    //  ts += '\'';
                    ls.append(ts);
                }
            }
        }
        return ls.toString();
    }

    public static void main(String... a) throws Exception {
        PhoneTranslator pt = new PhoneTranslator("/en_US/phoneset.tsv");
        String t = pt.translate("t ay p", "FESTVOX", "SAMPA");
        System.out.println(t);
    }

}
