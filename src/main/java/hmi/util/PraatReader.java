package hmi.util;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class PraatReader {

    final static int PR_INIT = 0;
    final static int PR_ITEM = 1;
    final static int PR_INTERVALS = 2;

    final static Pattern WN = Pattern.compile("(\\w+) \\[(\\d+)]:");

    private List<String> sections = new ArrayList<>();
    private Map<String, List<Line>> data = new TreeMap<>();

    public PraatReader(String path) throws IOException {
        read(new File(path));
    }

    public PraatReader(File f) throws IOException {
        read(f);
    }

    public List<String> getSections() {
        return sections;
    }

    public List<Line> getSection(String s) {
        return data.get(s);
    }

    private void read(File f) throws IOException {
        String ss = FileUtils.getFileAsString(f, "utf16");
        String[] s = ss.split("\n");
        int state = PR_INIT;
        String section = null;
        Line current_line = null;
        for (int i = 0; i < s.length; i++) {
            String tg_line = s[i].trim();
            if (tg_line.indexOf(" = ") != -1) {
                String[] nv = tg_line.split(" = ");
                switch (state) {
                    case PR_INIT:
                        break;
                    case PR_ITEM:
                        if (nv[0].equals("name")) {
                            section = nv[1].substring(1, nv[1].length() - 1);
                            sections.add(section);
                            data.put(section, new ArrayList<>());
                        }
                        break;
                    case PR_INTERVALS:
                        current_line.put(nv[0], nv[1]);
                }
            } else if (WN.matcher(tg_line).matches()) {
                String[] nv = tg_line.split(" \\[");
                String n = nv[0];
                if (n.equals("item")) {
                    state = PR_ITEM;
                } else if (n.equals("intervals")) {
                    state = PR_INTERVALS;
                    current_line = new Line();
                    data.get(section).add(current_line);
                }
            }
        }
    }

    public class Line extends HashMap<String, String> {
        public String text() {
            return get("text").replaceAll("\"", "");
        }

        public float xmax() {
            return Float.parseFloat(get("xmax"));
        }

        public float xmin() {
            return Float.parseFloat(get("xmin"));
        }
    }

    public static void main(String... a) throws Exception {
        PraatReader x = new PraatReader("/Users/posttool/Documents/github/jibo/script00100/0001.TextGrid");
        System.out.println(x.getSection("ARPABET"));
    }

}
