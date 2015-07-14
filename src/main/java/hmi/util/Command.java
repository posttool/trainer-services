package hmi.util;

import java.io.*;
import java.util.Scanner;


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

    public static int process(String cwd, String... cmd) throws Exception {
        System.out.println(cmd);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        if (cwd != null)
            builder.directory(new File(cwd));
        builder.redirectErrorStream(true);
        Process process = builder.start();

        Scanner s = new Scanner(process.getInputStream());
        StringBuilder text = new StringBuilder();
        while (s.hasNextLine()) {
            text.append(s.nextLine());
            text.append("\n");
        }
        s.close();

        int result = process.waitFor();

        System.out.printf("Process exited with result %d and output %s%n", result, text);
        return result;

    }
}
