package hmi.sig;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class DDSAudioInputStream extends AudioInputStream {
    public static final int MAX_AMPLITUDE = 32767;
    protected DoubleDataSource source;
    protected double[] sampleBuf;
    protected static final int SAMPLEBUFFERSIZE = 8192;

    public DDSAudioInputStream(DoubleDataSource source, AudioFormat format) {
        super(new ByteArrayInputStream(new byte[0]), format, AudioSystem.NOT_SPECIFIED);
        if (format.getChannels() > 1) {
            throw new IllegalArgumentException("Can only produce mono audio");
        }
        if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                && !format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            throw new IllegalArgumentException("Can only produce PCM_SIGNED or PCM_UNSIGNED audio");
        }
        int bitsPerSample = format.getSampleSizeInBits();
        if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24) {
            throw new IllegalArgumentException("Can deal with sample size 8 or 16 or 24, but not " + bitsPerSample);
        }
        this.source = source;
        this.sampleBuf = new double[SAMPLEBUFFERSIZE];
        assert frameSize == bitsPerSample / 8;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int nSamples = len / frameSize;
        int totalRead = 0;
        int currentPos = off;
        do {
            int toRead = nSamples - totalRead;
            if (toRead > sampleBuf.length)
                toRead = sampleBuf.length;
            int nRead = source.getData(sampleBuf, 0, toRead);
            // System.err.println("DDSAudioInputStream: read " + nRead +
            // " samples from source");
            if (frameSize == 1) { // bytes per sample
                for (int i = 0; i < nRead; i++, currentPos++) {
                    int sample = (int) Math.round(sampleBuf[i] * 127.0); // de-normalise
                                                                         // to
                                                                         // value
                                                                         // range
                    b[currentPos] = (byte) ((sample >> 8) & 0xFF);
                }
            } else if (frameSize == 2) { // 16 bit
                boolean bigEndian = format.isBigEndian();
                for (int i = 0; i < nRead; i++, currentPos += 2) {
                    int sample = (int) Math.round(sampleBuf[i] * 32767.0); // de-normalise
                                                                           // to
                                                                           // value
                                                                           // range
                    if (sample > MAX_AMPLITUDE || sample < -MAX_AMPLITUDE) {
                        System.err.println("Warning: signal amplitude out of range: " + sample);
                    }
                    byte hibyte = (byte) (sample >> 8);
                    byte lobyte = (byte) (sample & 0xFF);
                    if (!bigEndian) {
                        b[currentPos] = lobyte;
                        b[currentPos + 1] = hibyte;
                    } else {
                        b[currentPos] = hibyte;
                        b[currentPos + 1] = lobyte;
                    }
                    // System.err.println("out sample["+i+"]="+sample+" hi:"+Integer.toBinaryString(hibyte)+"/"+hibyte+" lo:"+Integer.toBinaryString(lobyte)+"/"+lobyte);
                }
            } else { // 24 bit
                boolean bigEndian = format.isBigEndian();
                for (int i = 0; i < nRead; i++, currentPos += 3) {
                    int sample = (int) Math.round(sampleBuf[i] * 8388605.0); // de-normalise
                                                                             // to
                                                                             // value
                                                                             // range
                    byte hibyte = (byte) (sample >> 16);
                    byte midbyte = (byte) ((sample >> 8) & 0xFF);
                    byte lobyte = (byte) (sample & 0xFF);
                    if (!bigEndian) {
                        b[currentPos] = lobyte;
                        b[currentPos + 1] = midbyte;
                        b[currentPos + 2] = hibyte;
                    } else {
                        b[currentPos] = hibyte;
                        b[currentPos + 1] = midbyte;
                        b[currentPos + 2] = lobyte;
                    }
                    // System.err.println("out sample["+i+"]="+sample+" hi:"+Integer.toBinaryString(hibyte)+"/"+hibyte+" lo:"+Integer.toBinaryString(lobyte)+"/"+lobyte);
                }
            }
            totalRead += nRead;
            assert currentPos <= off + len;
        } while (source.hasMoreData() && totalRead < nSamples);
        if (totalRead == 0)
            return -1;
        else
            return totalRead * frameSize;
    }

    public long skip(long n) throws IOException {
        double[] data = source.getData((int) n);
        return data.length;
    }

    public int available() throws IOException {
        return frameSize * source.available();
    }

    public void close() throws IOException {
    }

    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
    }

    public boolean markSupported() {
        return false;
    }

    public long getFrameLength() {
        long dataLength = source.getDataLength();
        if (dataLength == DoubleDataSource.NOT_SPECIFIED)
            return AudioSystem.NOT_SPECIFIED;
        else
            return dataLength;
    }
}
