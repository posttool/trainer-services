package hmi.annotate;

import hmi.train.VoiceRepo;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

import java.util.NoSuchElementException;

public class ProsodyIterator implements DataSetIterator {
    ProsodyInfo info;
    // the number of time slices
    int timeBins;
    // a grid to store the interval
    int semitoneBins;
    // .. for the DataSetIterator
    int numExamplesToFetch;
    int miniBatchSize;
    private int examplesSoFar = 0;

    public ProsodyIterator(VoiceRepo repo, int miniBatchSize, int numExamplesToFetch, int semitoneBins, int timeBins) {
        if (numExamplesToFetch % miniBatchSize != 0)
            throw new IllegalArgumentException("numExamplesToFetch must be a multiple of miniBatchSize");
        if (miniBatchSize <= 0) throw new IllegalArgumentException("Invalid miniBatchSize (must be >0)");
        this.numExamplesToFetch = numExamplesToFetch;
        this.miniBatchSize = miniBatchSize;
        this.semitoneBins = semitoneBins;
        this.timeBins = timeBins;
        try {
            info = new ProsodyInfo(repo, timeBins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int intervalToGrid(double interval) {
        int g = (int) Math.floor((interval - info.minSemitoneInterval) * (semitoneBins / (info.maxSemitoneInterval - info.minSemitoneInterval)));
        if (g < 0) {
            // System.out.println("?" + interval);
            return 0;
        }
        if (g >= semitoneBins) {
            System.out.println("??" + interval + " " + g);
            return semitoneBins - 1;
        }
        return g;
    }

    public double gridToInterval(int gridIdx) {
        return gridIdx / (semitoneBins / info.maxSemitoneInterval);
    }

    public boolean hasNext() {
        return examplesSoFar + miniBatchSize <= numExamplesToFetch;
    }

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
            double[] deltasInTime = info.tones[rndint(info.tones.length)];
            int currDeltaIdx = intervalToGrid(deltasInTime[0]);    //Current input
            int c = 0;
            for (int j = 1; j < deltasInTime.length; j++, c++) {
                int nextDeltaIdx = intervalToGrid(deltasInTime[j]);        //Next interval to predict
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
        return info.tones.length;
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
