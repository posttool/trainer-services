package hmi.util;

import java.io.*;


public class Command {
    public static int bash(String s) throws Exception {
        return bash(s, true);
    }

    public static int bash(String s, boolean DEBUG) throws Exception {
        Runtime rtime = Runtime.getRuntime();
        Process process = rtime.exec("/bin/bash");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        pw.print(s);
        if (DEBUG)
            System.out.println(s);
        pw.close();
        process.waitFor();
        if (process.exitValue() != 0) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            throw new Exception(errorReader.readLine());
        } else {
            return process.exitValue();
        }
    }
}
