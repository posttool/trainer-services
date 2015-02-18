package hmi.sig;

public interface DoubleDataSource {
    public int NOT_SPECIFIED = -1;

    public double[] getData(int amount);

    public int getData(double[] target);

    public int getData(double[] target, int targetPos, int length);

    public boolean hasMoreData();

    public int available();

    public double[] getAllData();

    public long getDataLength();

}
