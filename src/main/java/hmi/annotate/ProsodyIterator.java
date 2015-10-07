package hmi.annotate;

import hmi.data.Phone;
import hmi.data.Segment;
import hmi.data.SpeechMarkup;
import hmi.train.VoiceRepo;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class ProsodyIterator implements DataSetIterator {
    int MAX = 80;
    VoiceRepo repo;
    // audio features
    double maxAudioLength;
    double minF0 = Double.MAX_VALUE;
    double maxF0 = 0;
    double maxSemitoneDeltaInterval = 0;
    double minSemitoneDeltaInterval = Double.MAX_VALUE;
    // array of arrays of semitone intervals in time slices
    double[][] tones;
    // the number of time slices
    int timeBins;
    // divions of the maximum f0 delta
    int semitoneBins;
    // ..
    int numExamplesToFetch;
    int miniBatchSize;
    private int examplesSoFar = 0;

    public ProsodyIterator(VoiceRepo repo, int miniBatchSize, int numExamplesToFetch, int semitoneBins, int timeBins) {
        if (numExamplesToFetch % miniBatchSize != 0)
            throw new IllegalArgumentException("numExamplesToFetch must be a multiple of miniBatchSize");
        if (miniBatchSize <= 0) throw new IllegalArgumentException("Invalid miniBatchSize (must be >0)");
        this.repo = repo;
        this.numExamplesToFetch = numExamplesToFetch;
        this.miniBatchSize = miniBatchSize;
        this.semitoneBins = semitoneBins;
        this.timeBins = timeBins;
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
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
                            System.out.println(">" + sti);
                            minSemitoneDeltaInterval = Math.min(sti, minSemitoneDeltaInterval);
                            maxSemitoneDeltaInterval = Math.max(sti, maxSemitoneDeltaInterval);
                        }
                    }
                }
            }
        }
        System.out.println("maxAudio=" + maxAudioLength);
        System.out.println("maxF0=" + maxF0);
        System.out.println("minF0=" + minF0);
        System.out.println("minSemitoneDeltaInterval=" + minSemitoneDeltaInterval);
        System.out.println("maxSemitoneDeltaInterval=" + maxSemitoneDeltaInterval);
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

    public int intervalToGrid(double interval) {
        return (int) Math.floor((interval - minSemitoneDeltaInterval) * (semitoneBins / (maxSemitoneDeltaInterval - minSemitoneDeltaInterval)));
    }

    public double gridToInterval(int gridIdx) {
        return gridIdx / (semitoneBins / maxSemitoneDeltaInterval);
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return examplesSoFar + miniBatchSize <= numExamplesToFetch;
    }


    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public DataSet next() {
        return next(miniBatchSize);
    }

    public int rndint(int d) {
        return (int) (Math.random() * d);
    }

    @Override
    public DataSet next(int num) {
        INDArray input = Nd4j.zeros(new int[]{num, semitoneBins, timeBins});
        INDArray labels = Nd4j.zeros(new int[]{num, semitoneBins, timeBins});

        for (int i = 0; i < num; i++) {
            double[] deltasInTime = tones[rndint(tones.length)];
            int currDeltaIdx = intervalToGrid(deltasInTime[0]);    //Current input
            int c = 0;
            for (int j = 1; j < deltasInTime.length; j++, c++) {
                int nextDeltaIdx = intervalToGrid(deltasInTime[j]);        //Next character to predict
                input.putScalar(new int[]{i, currDeltaIdx, c}, 1.0);
                labels.putScalar(new int[]{i, nextDeltaIdx, c}, 1.0);
                float temp = input.getFloat(new int[]{i, currDeltaIdx, c});
                float temp2 = labels.getFloat(new int[]{i, nextDeltaIdx, c});
                if (temp != 1.0f)
                    throw new RuntimeException();
                if (temp2 != 1.0f)
                    throw new RuntimeException();
                currDeltaIdx = nextDeltaIdx;
            }
        }
        examplesSoFar += num;
        return new DataSet(input, labels);
    }

    @Override
    public int totalExamples() {
        return tones.length;
    }

    @Override
    public int inputColumns() {
        return semitoneBins;
    }

    public int totalOutcomes() {
        return semitoneBins;
    }

    public void reset() {
        examplesSoFar = 0;
    }

    public int batch() {
        return miniBatchSize;
    }

    public int cursor() {
        return examplesSoFar;
    }

    public int numExamples() {
        return numExamplesToFetch;
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {

    }


}
