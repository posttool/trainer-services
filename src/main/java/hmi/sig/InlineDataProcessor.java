package hmi.sig;

public interface InlineDataProcessor {

    public void applyInline(double[] data, int off, int len);
}
