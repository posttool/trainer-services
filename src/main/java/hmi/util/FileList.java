package hmi.util;


import java.io.File;

public class FileList {
    String[] files;

    public FileList(String path, String ext) {
        files = FileUtils.getFileList(path, ext, false);
    }

    public int length() {
        return files.length;
    }

    public String get(int i) {
        File f = new File(files[i]);
        String n = f.getName();
        int d = n.indexOf(".");
        if (d != -1)
            n = n.substring(0, d);
        return n;
    }

    public String getFile(int i) {
        return files[i];
    }

    public String[] getFiles() {
        return files;
    }

}
