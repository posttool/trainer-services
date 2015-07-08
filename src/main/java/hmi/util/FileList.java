package hmi.util;


import java.io.File;

public class FileList {
    String path;
    String ext;
    String[] files;

    public FileList(String path, String ext) {
        files = FileUtils.getFileList(path, ext, false);
    }

    public int length() {
        return files.length;
    }

    public String name(int i) {
        File f = new File(files[i]);
        String n = f.getName();
        int d = n.indexOf(".");
        if (d != -1)
            n = n.substring(0, d);
        return n;
    }

    public File file(int i) {
        return new File(path, files[i]);
    }

    public String[] files() {
        return files;
    }

}
