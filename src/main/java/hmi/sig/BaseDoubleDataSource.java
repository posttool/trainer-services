package hmi.sig;

public class BaseDoubleDataSource implements DoubleDataSource {
    protected DoubleDataSource inputSource = null;
    protected long dataLength = DoubleDataSource.NOT_SPECIFIED;

    public BaseDoubleDataSource() {
    }

    public BaseDoubleDataSource(DoubleDataSource inputSource) {
        this.inputSource = inputSource;
        if (inputSource != null)
            dataLength = inputSource.getDataLength();
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

    public int getData(double[] target, int targetPos, int length) {
        if (target.length - targetPos < length) {
            throw new IllegalArgumentException("Target array cannot hold enough data (" + (target.length - targetPos)
                    + " left, but " + length + " requested)");
        }
        if (inputSource == null)
            return 0;
        return inputSource.getData(target, targetPos, length);
    }

    public boolean hasMoreData() {
        if (inputSource == null)
            return false;
        return inputSource.hasMoreData();
    }

    public int available() {
        if (inputSource == null)
            return 0;
        return inputSource.available();

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
