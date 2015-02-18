package hmi.sig;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class FFTTest {
    public static void main(String[] args) throws Exception {

        for (int i = 0; i < args.length; i++) {
            System.out.println("Measuring FFT accuracy for " + args[i]);
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
            double[] signal = AudioUtils.getSamplesAsDoubleArray(ais);
            int N = signal.length;
            if (!MathUtils.isPowerOfTwo(N)) {
                N = MathUtils.closestPowerOfTwoAbove(N);
            }
            double[] ar = new double[N];
            double[] ai = new double[N];
            System.arraycopy(signal, 0, ar, 0, signal.length);

            /*
             * double[] conv1 = autoCorrelateOrig(ar); double[] conv2 =
             * autoCorrelate(ar);
             * System.err.println("Difference between conv1 and 2: "+
             * MathUtils.sumSquaredError(conv1, conv2)); for (int j=0;
             * j<conv1.length; j++) { double ddelta =
             * Math.abs(conv1[j]-conv2[j]); //if (ddelta > 1.E-4)
             * System.err.println("delta["+j+"]="+ddelta+" out of "+conv1[j]); }
             */
            // Transform:
            FFT.transform(ar, ai, false);
            double[] result1 = new double[2 * N];
            for (int j = 0; j < N; j++) {
                result1[2 * j] = ar[j];
                result1[2 * j + 1] = ai[j];
            }
            // Transform 2:
            double[] result2 = new double[2 * N];
            for (int j = 0; j < signal.length; j++) {
                result2[2 * j] = signal[j];
            }
            FFT.transform(result2, false);
            System.err.println("Difference between result1 and 2: " + MathUtils.sumSquaredError(result1, result2));
            // Transform 3:
            double[] result3 = new double[N];
            System.arraycopy(signal, 0, result3, 0, signal.length);
            FFT.realTransform(result3, false);
            double[] result2a = new double[N];
            System.arraycopy(result2, 0, result2a, 0, N);
            System.err.println("F2(N/2)=" + result2[N] + " F3(N/2)=" + result3[1]);
            result3[1] = 0;
            System.err.println("Difference between result 2a and 3: " + MathUtils.sumSquaredError(result2a, result3));
            double[] delta = new double[N];
            for (int j = 0; j < N; j++) {
                delta[j] = Math.abs(result2a[j] - result3[j]);
                if (delta[j] > 1.E-4)
                    System.err.println("delta[" + j + "]=" + delta[j]);
            }
            result3[1] = result2[N];

            // And backwards:
            FFT.transform(ar, ai, true);
            FFT.transform(result2, true);
            double[] inverse2 = new double[N];
            for (int j = 0; j < N; j++) {
                inverse2[j] = result2[2 * j];
            }
            FFT.realTransform(result3, true);
            System.err.println("Difference between inverse 1 and 2:" + MathUtils.sumSquaredError(ar, inverse2));
            System.err.println("Difference between inverse 1 and 3:" + MathUtils.sumSquaredError(ar, result3));
            for (int j = 0; j < N; j++) {
                delta[j] = Math.abs(ar[j] - result3[j]);
                if (delta[j] > 1.E-4)
                    System.err.println("delta[" + j + "]=" + delta[j]);
            }

            // Compute speed
            System.out.println("Computing FFT speed for 1000 transforms at different n");
            for (int k = 5; k <= 11; k++) {
                int n = 1 << k;
                double[] re = new double[n];
                double[] im = new double[n];
                System.arraycopy(signal, 0, re, 0, Math.min(signal.length, n));
                long start = System.currentTimeMillis();
                for (int j = 0; j < 5000; j++) {
                    FFT.transform(re, im, false);
                    FFT.transform(re, im, true);
                }
                long mid = System.currentTimeMillis();
                for (int j = 0; j < 5000; j++) {
                    FFT.realTransform(re, false);
                    FFT.realTransform(re, true);
                }
                long end = System.currentTimeMillis();
                long t1 = (mid - start);
                long t2 = (end - mid);
                System.out.println("n=" + n + " fft=" + t1 + ", realFFT=" + t2);
            }

        }
    }

}
