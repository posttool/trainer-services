package hmi.util;


public class FileList {
    String[] files;

    public FileList(String path, String ext) {
        files = FileUtils.getFileList(path, ext, false);
    }

    public int length() {
        return files.length;
    }

    public String get(int i) {
        return files[i];
    }

}
