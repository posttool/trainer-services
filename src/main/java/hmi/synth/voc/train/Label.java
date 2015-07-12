package hmi.synth.voc.train;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class Label {
    public String name;
    public double startTime;
    public double endTime;
    public int index;
    public double sCost;

    public Label(String name, double startTime, double endTime, int index) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.index = index;
        this.sCost = 0;
    }

    public Label(String name, double startTime, double endTime, int index, double sCost) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.index = index;
        this.sCost = sCost;
    }

    public double getSCost() {
        return this.sCost;
    }

    public double getStartTime() {
        return this.startTime;
    }

    public double getEndTime() {
        return this.endTime;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setName(String name) {
        this.name = name;
    }


    public static Label[] readLabFile(String labFile) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(labFile)), "UTF-8"));
        String line;

        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#"))
                break;
        }

        String label = null;
        double startTimeStamp = 0.0;
        double endTimeStamp = 0.0;
        double sCost = 0.0;
        int idx = 0;
        while ((line = labels.readLine()) != null) {
            label = null;
            if (line != null) {
                List labelData = getLabelData(line);
                sCost = (new Double((String) labelData.get(3))).doubleValue();
                label = (String) labelData.get(2);
                idx = Integer.parseInt((String) labelData.get(1));
                endTimeStamp = Double.parseDouble((String) labelData.get(0));
            }
            if (label == null)
                break;
            lines.add(label.trim() + " " + startTimeStamp + " " + endTimeStamp + " " + idx + " " + sCost);
            startTimeStamp = endTimeStamp;
        }
        labels.close();

        Label[] ulab = new Label[lines.size()];
        Iterator<String> itr = lines.iterator();
        for (int i = 0; itr.hasNext(); i++) {
            String element = itr.next();
            String[] wrds = element.split("\\s+");
            ulab[i] = new Label(wrds[0], (new Double(wrds[1])).doubleValue(), (new Double(wrds[2])).doubleValue(),
                    (new Integer(wrds[3])).intValue(), (new Double(wrds[4])).doubleValue());
        }
        return ulab;
    }

    public static void writeLabFile(Label[] ulab, String outFile) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(outFile));
        pw.println("#");
        for (int i = 0; i < ulab.length; i++) {
            pw.printf(Locale.US, "%.6f %d %s\n", ulab[i].getEndTime(), ulab[i].getIndex(), ulab[i].getName());
        }
        pw.flush();
        pw.close();
    }

    private static ArrayList getLabelData(String line) throws IOException {
        if (line == null)
            return null;
        ArrayList d = new ArrayList();
        StringTokenizer st = new StringTokenizer(line.trim());
        d.add(st.nextToken());
        d.add(st.nextToken());
        d.add(st.nextToken());
        if (st.hasMoreTokens())
            d.add(st.nextToken());
        else
            d.add("0");
        return d;
    }
}