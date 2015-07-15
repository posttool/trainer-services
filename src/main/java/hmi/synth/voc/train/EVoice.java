package hmi.synth.voc.train;

import hmi.data.VoiceRepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EVoice {


    public boolean compute(VoiceRepo repo) throws Exception {

        String voicedir = repo.path("/");

        launchProcWithLogFile("perl " + voicedir + "hts/scripts/Training.pl " + voicedir
                + "hts/scripts/Config.pm", voicedir);

        return true;
    }


    private void launchProcWithLogFile(String cmdLine, String voicedir) {

        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;

        Date today;
        String output;
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyy.MM.dd-H:mm:ss", new Locale("en", "US"));
        today = new Date();
        output = formatter.format(today);
        String logFile = voicedir + "hts/log-" + output;

        System.out.println("\nRunning: " + cmdLine);
        System.out.println("\nThe training procedure can take several hours...");
        System.out.println("Detailed information about the training status can be found in the logfile:\n  " + logFile);
        System.out.println("The following is general information about execution of training steps:");

        try {
            FileWriter log = new FileWriter(logFile);
            ProcessBuilder pb = new ProcessBuilder(cmdLine.split(" "));
            pb.directory(new File(voicedir));
            pb.redirectErrorStream(true);
            proc = pb.start();

            procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = procStdout.readLine()) != null) {
                if (line.contains("Start "))
                    System.out.println("\nStep: " + line);
                log.write(line + "\n");
            }

            /* Wait and check the exit value */
            proc.waitFor();
            if (proc.exitValue() != 0) {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                while ((line = errReader.readLine()) != null) {
                    System.err.println("ERR> " + line);
                }
                errReader.close();
                throw new RuntimeException("Failed on file [" + voicedir + "]!\n"
                        + "Command line was: [" + cmdLine + "].");
            }
            log.close();
        } catch (IOException e) {
            throw new RuntimeException("IOException on file [" + voicedir + "].", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted on file [" + voicedir + "].", e);
        }

    }


}
