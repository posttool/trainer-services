package hmi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Vector;

public class DirectoryList {
    private Vector bList = null;
    private String fromDir = null;
    private String fromExt = null;
    private boolean hasChanged;
    private static final int DEFAULT_INCREMENT = 128;

    public DirectoryList() {
        fromDir = null;
        fromExt = null;
        bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);
        hasChanged = false;
    }

    public DirectoryList(String setFromDir, String setFromExt, Vector setVec) {
        fromDir = setFromDir;
        fromExt = setFromExt;
        bList = setVec;
        hasChanged = false;
    }

    public DirectoryList(String[] str) {
        fromDir = null;
        fromExt = null;
        bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);
        add(str);
        hasChanged = false;
    }

    public DirectoryList(String dirName, final String extension) {
        fromDir = dirName;
        if (extension.indexOf(".") != 0)
            fromExt = "." + extension;
        else
            fromExt = extension;

        File dir = new File(dirName);
        if (!dir.exists()) {
            throw new RuntimeException("Directory [" + dirName + "] does not exist. Can't find the [" + extension
                    + "] files.");
        }
        File[] selectedFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        });

        Arrays.sort(selectedFiles);

        bList = new Vector(selectedFiles.length, DEFAULT_INCREMENT);
        String str = null;
        int subtractFromFilename = extension.length();
        for (int i = 0; i < selectedFiles.length; i++) {
            str = selectedFiles[i].getName().substring(0, selectedFiles[i].getName().length() - subtractFromFilename);
            add(str);
        }
        hasChanged = false;
    }

    public DirectoryList(String fileName) throws IOException {
        load(fileName);
        hasChanged = false;
    }

    public void write(String fileName) throws IOException {
        write(new File(fileName));
    }

    public void write(File file) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), true);
        if (fromDir != null) {
            pw.println("FROM: " + fromDir + "*" + fromExt);
        }
        String str = null;
        for (int i = 0; i < bList.size(); i++) {
            str = (String) (bList.elementAt(i));
            pw.println(str);
        }
    }

    public void load(String fileName) throws IOException {
        BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
        if (bList == null)
            bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);

        String line = bfr.readLine();
        if (line.indexOf("FROM: ") != -1) {
            line = line.substring(6);
            String[] parts = new String[2];
            parts = line.split("\\*", 2);
            fromDir = parts[0];
            fromExt = parts[1];
        } else if (!(line.matches("^\\s*$")))
            add(line);
        while ((line = bfr.readLine()) != null) {
            if (!(line.matches("^\\s*$")))
                add(line);
        }
    }

    public void add(String str) {
        if (!bList.contains(str))
            bList.add(str);
        hasChanged = true;
    }

    public void add(String[] str) {
        for (int i = 0; i < str.length; i++)
            add(str[i]);
        hasChanged = true;
    }

    public boolean remove(String str) {
        hasChanged = true;
        return (bList.remove(str));
    }

    public boolean remove(DirectoryList bnl) {
        boolean ret = true;
        for (int i = 0; i < bnl.getLength(); i++) {
            bList.remove(bnl.getName(i));
        }
        hasChanged = true;
        return (ret);
    }

    public DirectoryList duplicate() {
        return (new DirectoryList(this.fromDir, this.fromExt, (Vector) (this.bList.clone())));
    }

    public DirectoryList subList(int fromIndex, int toIndex) {
        Vector subVec = new Vector(toIndex - fromIndex, DEFAULT_INCREMENT);
        for (int i = fromIndex; i < toIndex; i++)
            subVec.add(this.getName(i));
        return (new DirectoryList(this.fromDir, this.fromExt, subVec));
    }

    public String[] getListAsArray() {
        String[] ret = new String[this.getLength()];
        ret = (String[]) bList.toArray(ret);
        return ((String[]) (ret));
    }

    public Vector getListAsVector() {
        return (bList);
    }

    public int getLength() {
        return (bList.size());
    }

    public String getDir() {
        return (fromDir);
    }

    public String getExt() {
        return (fromExt);
    }

    public String getName(int i) {
        return (String) bList.elementAt(i);
    }

    public boolean contains(String str) {
        return (bList.contains(str));
    }

    public boolean contains(DirectoryList bnl) {
        if (bnl.getLength() > this.getLength())
            return (false);
        for (int i = 0; i < bnl.getLength(); i++) {
            if (!this.contains(bnl.getName(i)))
                return (false);
        }
        return (true);
    }

    public boolean equals(DirectoryList bnl) {
        if (bnl.getLength() != this.getLength())
            return (false);
        for (int i = 0; i < bnl.getLength(); i++) {
            if (!this.contains(bnl.getName(i)))
                return (false);
        }
        return (true);
    }

    public void sort() {
        String[] str = getListAsArray();
        Arrays.sort(str);
        bList.removeAllElements();
        add(str);
        hasChanged = true;
    }

    public void clear() {
        fromDir = null;
        fromExt = null;
        bList.removeAllElements();
        hasChanged = true;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

}