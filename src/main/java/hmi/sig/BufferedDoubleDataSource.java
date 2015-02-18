package hmi.sig;

public class BufferedDoubleDataSource implements DoubleDataSource {
    public static final int DEFAULT_BUFFERSIZE = 8192;
    protected double[] buf;
    protected int readPos = 0;
    protected int writePos = 0;
    protected InlineDataProcessor dataProcessor = null;
    protected DoubleDataSource inputSource = null;
    protected long dataLength = DoubleDataSource.NOT_SPECIFIED;

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
        this.inputSource = inputSource;
        if (inputSource != null)
            dataLength = inputSource.getDataLength();
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
    
    public double[] getData(int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be positive");
        double[] container = new double[amount];
        int received = getData(container);
        if (received == 0) {
            return null;
        } else if (received < amount) {
            double[] newContainer = new double[received];
            System.arraycopy(container, 0, newContainer, 0, received);
            return newContainer;
        } else {
            return container;
        }
    }

    public int getData(double[] target) {
        return getData(target, 0, target.length);
    }

  

    public double[] getAllData() {
        double[] all = new double[BufferedDoubleDataSource.DEFAULT_BUFFERSIZE];
        int currentPos = 0;
        while (hasMoreData()) {
            int nRead = getData(all, currentPos, all.length - currentPos);
            if (nRead < all.length - currentPos) {
                // done
                assert !hasMoreData();
                currentPos += nRead;
                break; // leave while loop
            } else {
                assert currentPos + nRead == all.length;
                double[] newAll = new double[2 * all.length];
                System.arraycopy(all, 0, newAll, 0, all.length);
                currentPos = all.length;
                all = newAll;
            }
        }
        double[] result = new double[currentPos];
        System.arraycopy(all, 0, result, 0, currentPos);
        return result;
    }

    public long getDataLength() {
        return dataLength;
    }

}
