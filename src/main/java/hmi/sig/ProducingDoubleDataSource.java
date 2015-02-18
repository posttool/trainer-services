package hmi.sig;

import java.util.concurrent.ArrayBlockingQueue;

public abstract class ProducingDoubleDataSource extends BufferedDoubleDataSource implements Runnable {
    private static final Double END_OF_STREAM = Double.NEGATIVE_INFINITY;

    protected ArrayBlockingQueue<Double> queue = new ArrayBlockingQueue<Double>(1024);
    private Thread dataProducingThread = null;
    private boolean hasSentEndOfStream = false;
    private boolean hasReceivedEndOfStream = false;

    protected ProducingDoubleDataSource() {
        this(DoubleDataSource.NOT_SPECIFIED);
    }

    protected ProducingDoubleDataSource(long numDataThatWillBeProduced) {
        this(numDataThatWillBeProduced, null);
    }

    protected ProducingDoubleDataSource(InlineDataProcessor dataProcessor) {
        this(DoubleDataSource.NOT_SPECIFIED, dataProcessor);
    }

    protected ProducingDoubleDataSource(long numDataThatWillBeProduced, InlineDataProcessor dataProcessor) {
        super((DoubleDataSource) null, dataProcessor);
        this.dataLength = numDataThatWillBeProduced;
    }

    public void start() {
        dataProducingThread = new Thread(this);
        dataProducingThread.setDaemon(true);
        dataProducingThread.start();
    }

    public abstract void run();

    public void putOneDataPoint(double value) {
        try {
            queue.put(value);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected interruption", e);
        }
    }

    protected void putEndOfStream() {
        putOneDataPoint(END_OF_STREAM);
        hasSentEndOfStream = true;
    }

    @Override
    public boolean hasMoreData() {
        checkStarted();
        return !isAllProductionDataRead() || available() > 0;
    }

    @Override
    public int available() {
        checkStarted();
        return currentlyInBuffer() + currentlyInQueue();
    }

    private int currentlyInQueue() {
        if (isAllProductionDataRead()) {
            return 0;
        }
        int inQueue = queue.size();
        if (hasSentEndOfStream && !hasReceivedEndOfStream) {
            inQueue -= 1;
        }
        return inQueue;
    }

    @Override
    protected boolean readIntoBuffer(int minLength) {
        checkStarted();
        if (isAllProductionDataRead()) {
            return false;
        }
        if (bufferSpaceLeft() < minLength) {
            // current buffer cannot hold the data requested;
            // need to make it larger
            increaseBufferSize(minLength + currentlyInBuffer());
        } else if (buf.length - writePos < minLength) {
            compact(); // create a contiguous space for the new data
        }
        // Now we have a buffer that can hold at least minLength new data points
        int readSum = 0;
        while (readSum < minLength) {
            double data = getOneDataPoint();
            if (data == END_OF_STREAM) {
                hasReceivedEndOfStream = true;
                break;
            }
            buf[writePos] = data;
            writePos++;
            readSum++;
        }
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos - readSum, readSum);
        }
        return readSum == minLength;
    }

    private double getOneDataPoint() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected interruption", e);
        }
    }

    private void checkStarted() throws IllegalStateException {
        if (!isStarted()) {
            throw new IllegalStateException("Producer thread has not been started -- call start()");
        }
    }

    private boolean isStarted() {
        return dataProducingThread != null;
    }

    private boolean isAllProductionDataRead() {
        return hasReceivedEndOfStream;
    }
}
