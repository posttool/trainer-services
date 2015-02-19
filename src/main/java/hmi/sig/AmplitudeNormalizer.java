package hmi.sig;

import hmi.util.MathUtils;

public class AmplitudeNormalizer implements InlineDataProcessor {

    private double max;

    public AmplitudeNormalizer(double initialMax) {
        this.max = initialMax;
    }

    public void applyInline(double[] data, int off, int len) {
        double localMax = MathUtils.absMax(data, off, len);
        if (localMax > max) {
            max = localMax;
        }
        for (int i = off; i < off + len; i++) {
            data[i] /= max;
        }
    }

}
