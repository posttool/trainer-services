package hmi.sig;

public class BufferedDoubleDataSource extends BaseDoubleDataSource {
    public static final int DEFAULT_BUFFERSIZE = 8192;
    protected double[] buf;
    protected int readPos = 0;
    protected int writePos = 0;
    protected InlineDataProcessor dataProcessor = null;

    public BufferedDoubleDataSource(double[] inputData) {
        this(inputData, null);
    }

    public BufferedDoubleDataSource(double[] inputData, InlineDataProcessor dataProcessor) {
        super();
        buf = new double[inputData.length];
        System.arraycopy(inputData, 0, buf, 0, buf.length);
        writePos = buf.length;
        dataLength = buf.length;
        this.dataProcessor = dataProcessor;
        if (dataProcessor != null)
            dataProcessor.applyInline(buf, 0, writePos);
    }

    public BufferedDoubleDataSource(DoubleDataSource inputSource) {
        this(inputSource, null);
    }

    public BufferedDoubleDataSource(DoubleDataSource inputSource, InlineDataProcessor dataProcessor) {
        super(inputSource);
        buf = new double[DEFAULT_BUFFERSIZE];
        this.dataProcessor = dataProcessor;
    }

    public boolean hasMoreData() {
        if (currentlyInBuffer() > 0 || inputSource != null && inputSource.hasMoreData())
            return true;
        return false;
    }

    public int currentlyInBuffer() {
        assert writePos >= readPos;
        return writePos - readPos;
    }

    public int available() {
        int available = currentlyInBuffer();
        if (inputSource != null)
            available += inputSource.available();
        return available;
    }

    protected int bufferSpaceLeft() {
        return buf.length - currentlyInBuffer();
    }

    @Override
    public int getData(double[] target, int targetPos, int length) {
        // if (target.length < targetPos+length)
        // throw new
        // IllegalArgumentException("Not enough space left in target array");
        if (currentlyInBuffer() < length) { // first need to try and read some
                                            // more data
            readIntoBuffer(length - currentlyInBuffer());
        }
        int toDeliver = length;
        if (currentlyInBuffer() < length)
            toDeliver = currentlyInBuffer();
        System.arraycopy(buf, readPos, target, targetPos, toDeliver);
        readPos += toDeliver;
        return toDeliver;
    }

    protected boolean readIntoBuffer(int minLength) {
        if (inputSource == null) {
            return false;
        }
        if (!inputSource.hasMoreData()) {
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
        readSum = inputSource.getData(buf, writePos, minLength);
        writePos += readSum;
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos - readSum, readSum);
        }
        return readSum == minLength;

    }

    protected void increaseBufferSize(int minSize) {
        int newLength = buf.length;
        while (newLength < minSize)
            newLength *= 2;
        double[] newBuf = new double[newLength];
        int avail = currentlyInBuffer();
        System.arraycopy(buf, readPos, newBuf, 0, avail);
        buf = newBuf;
        readPos = 0;
        writePos = avail;
    }

    protected void compact() {
        if (readPos == 0)
            return;
        int avail = writePos - readPos;
        System.arraycopy(buf, readPos, buf, 0, avail);
        readPos = 0;
        writePos = avail;
    }

}
