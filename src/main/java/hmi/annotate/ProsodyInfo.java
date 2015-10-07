package hmi.annotate;


import hmi.data.Phone;
import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.train.VoiceRepo;

import java.io.IOException;
import java.util.List;

public class ProsodyInfo {
    int MAX = 80;
    // audio features
    double maxAudioLength;
    double minF0 = Double.MAX_VALUE;
    double maxF0 = 0;
    double maxSemitoneInterval = 0;
    double minSemitoneInterval = Double.MAX_VALUE;
    // array of arrays of semitone intervals in time slices
    double[][] tones;
    // the total number of slices
    int timeBins;


    public ProsodyInfo(VoiceRepo repo, int timeBins) throws IOException {
        this.timeBins = timeBins;
        for (int i = 0; i < MAX && i < repo.files().length(); i++) {
            SpeechMarkup sm = repo.getSpeechMarkup(i);
            List<Segment> segs = sm.getSegments();
            for (Segment seg : segs) {
                maxAudioLength = Math.max(maxAudioLength, seg.getEnd() + 2);
                if (seg instanceof Phone) {
                    Phone p = (Phone) seg;
                    Segment next = seg.getNextSegment();
                    while (next != null && !(next instanceof Phone))
                        next = next.getNextSegment();
                    float[] f = p.getF0();
                    for (int c = 0; c < f.length; c++) {
                        maxF0 = Math.max(maxF0, f[c]);
                        minF0 = Math.min(minF0, f[c]);
                        double sti;
                        if (c < f.length - 1) {
                            sti = getSemitoneInterval(f[c], f[c + 1]);
                        } else if (next != null) {
                            sti = getSemitoneInterval(f[c], ((Phone) next).getF0()[0]);
                        } else {
                            sti = getSemitoneInterval(f[c], 0); //TODO meanF0
                        }
                        if (Double.isNaN(sti) || Double.isInfinite(sti)) {
//                            if (c < f.length - 1) {
//                                System.out.println("?1 " + f[c] + " " + f[c + 1]);
//                            } else if (next != null) {
//                                System.out.println("?2 " + f[c] + " " + ((Phone) next).getF0()[0]);
//                            } else {
//                                System.out.println("?3 " + f[c]);
//                            }
                        } else {
                            minSemitoneInterval = Math.min(sti, minSemitoneInterval);
                            maxSemitoneInterval = Math.max(sti, maxSemitoneInterval);
                        }
                    }
                }
            }
        }
        System.out.println("maxAudio=" + maxAudioLength);
        System.out.println("maxF0=" + maxF0);
        System.out.println("minF0=" + minF0);
        System.out.println("minSemitoneInterval=" + minSemitoneInterval);
        System.out.println("maxSemitoneInterval=" + maxSemitoneInterval);
        this.tones = new double[Math.min(MAX, repo.files().length())][];
        for (int i = 0; i < MAX && i < repo.files().length(); i++) {
            tones[i] = new double[timeBins];
            for (int c = 0; c < timeBins; c++)
                tones[i][c] = 0;
            SpeechMarkup sm = repo.getSpeechMarkup(i);
            List<Segment> segs = sm.getSegments();
            for (Segment seg : segs) {
                if (seg instanceof Phone) {
                    Phone p = (Phone) seg;
                    Segment next = seg.getNextSegment();
                    while (next != null && !(next instanceof Phone))
                        next = next.getNextSegment();
                    float[] f = p.getF0();
                    for (int c = 0; c < f.length; c++) {
                        double sti;
                        if (c < f.length - 1) {
                            sti = getSemitoneInterval(f[c], f[c + 1]);
                        } else if (next != null) {
                            sti = getSemitoneInterval(f[c], ((Phone) next).getF0()[0]);
                        } else {
                            sti = getSemitoneInterval(f[c], 0); //TODO meanF0
                        }
                        int t = timeToGrid(p.getBegin() + c * .005);
                        if (!Double.isNaN(sti)) {
                            tones[i][t] = sti;
                        }
                    }
                }
            }
            System.out.print(">" + i);
            for (int j = 0; j < timeBins; j++) {
                System.out.print(" " + tones[i][j]);
            }
            System.out.println();
        }
    }


    public double getSemitoneInterval(double f1, double f2) {
        //¢ or c = 1200 × log2 (f2 / f1) // http://www.sengpielaudio.com/calculator-centsratio.htm
        if (f1 == 0)
            throw new RuntimeException("F cannot be 0 [" + f1 + ", " + f2 + "]");
        double semis = (12 / Math.log(2)) * Math.log(f2 / f1); //interval in semitones
        return semis;//round(semis, 5);
    }

    public int timeToGrid(double f) {
        return (int) Math.floor(f * (timeBins / maxAudioLength));
    }
}
