package hmi.sig;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class MonoAudioInputStream extends AudioInputStream {
    protected int inputChannels;
    protected int inputMode;
    protected AudioFormat newFormat;

    public MonoAudioInputStream(AudioInputStream input) {
        this(input, AudioPlayer.STEREO);
    }

    public MonoAudioInputStream(AudioInputStream input, int inputMode) {
        super(input, input.getFormat(), input.getFrameLength());
        this.newFormat = new AudioFormat(input.getFormat().getEncoding(), input.getFormat().getSampleRate(), input
                .getFormat().getSampleSizeInBits(), 1, input.getFormat().getFrameSize()
                / input.getFormat().getChannels(), input.getFormat().getFrameRate(), input.getFormat().isBigEndian());
        this.inputChannels = input.getFormat().getChannels();
        if (inputChannels < 2)
            throw new IllegalArgumentException("expected more than one input channel!");
        this.inputMode = inputMode;
        if (inputMode == AudioPlayer.MONO)
            throw new IllegalArgumentException("expected non-mono input mode");
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int sampleSizeInBytes = frameSize / inputChannels;
        int outputFrameSize = sampleSizeInBytes; // mono output
        int nFrames = len / outputFrameSize;
        boolean bigEndian = getFormat().isBigEndian();
        byte[] inputBytes = new byte[nFrames * frameSize];
        int nInputBytes = super.read(inputBytes, 0, inputBytes.length);
        if (nInputBytes <= 0)
            return nInputBytes;

        if (inputMode == AudioPlayer.STEREO) {
            for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
                int sample = 0;
                for (int c = 0; c < inputChannels; c++) {
                    if (sampleSizeInBytes == 1) {
                        sample += inputBytes[i] << 8;
                    } else if (sampleSizeInBytes == 2) { // 16 bit
                        byte lobyte;
                        byte hibyte;
                        if (!bigEndian) {
                            lobyte = inputBytes[i];
                            hibyte = inputBytes[i + 1];
                        } else {
                            lobyte = inputBytes[i + 1];
                            hibyte = inputBytes[i];
                        }
                        sample += hibyte << 8 | lobyte & 0xFF;
                    } else { // bytesPerSample == 3, i.e. 24 bit
                        assert sampleSizeInBytes == 3 : "Unsupported sample size in bytes: " + sampleSizeInBytes;
                        byte lobyte;
                        byte midbyte;
                        byte hibyte;
                        if (!bigEndian) {
                            lobyte = inputBytes[i];
                            midbyte = inputBytes[i + 1];
                            hibyte = inputBytes[i + 2];
                        } else {
                            lobyte = inputBytes[i + 2];
                            midbyte = inputBytes[i + 1];
                            hibyte = inputBytes[i];
                        }
                        sample += hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
                    }
                }
                sample /= inputChannels; // here is where we average the three
                                         // samples
                if (sampleSizeInBytes == 1) {
                    b[j] = (byte) ((sample >> 8) & 0xFF);
                } else if (sampleSizeInBytes == 2) { // 16 bit
                    byte lobyte = (byte) (sample & 0xFF);
                    byte hibyte = (byte) (sample >> 8);
                    if (!bigEndian) {
                        b[j] = lobyte;
                        b[j + 1] = hibyte;
                    } else {
                        b[j] = hibyte;
                        b[j + 1] = lobyte;
                    }
                } else { // bytesPerSample == 3, i.e. 24 bit
                    assert sampleSizeInBytes == 3 : "Unsupported sample size in bytes: " + sampleSizeInBytes;
                    byte lobyte = (byte) (sample & 0xFF);
                    byte midbyte = (byte) ((sample >> 8) & 0xFF);
                    byte hibyte = (byte) (sample >> 16);
                    if (!bigEndian) {
                        b[j] = lobyte;
                        b[j + 1] = midbyte;
                        b[j + 2] = hibyte;
                    } else {
                        b[j] = hibyte;
                        b[j + 1] = midbyte;
                        b[j + 2] = lobyte;
                    }
                }
            } // for all frames
        } else if (inputMode == AudioPlayer.LEFT_ONLY) {
            for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
                for (int k = 0; k < sampleSizeInBytes; k++) {
                    b[j + k] = inputBytes[i + k];
                }
            }
        } else {
            assert inputMode == AudioPlayer.RIGHT_ONLY : "unexpected input mode: " + inputMode;
            for (int i = 0, j = off; i < nInputBytes; i += frameSize, j += outputFrameSize) {
                for (int k = 0; k < sampleSizeInBytes; k++) {
                    b[j + k] = inputBytes[i + k + sampleSizeInBytes];
                }
            }

        }

        return nInputBytes / inputChannels;
    }

    public long skip(long n) throws IOException {
        return super.skip(n * inputChannels) / inputChannels;
    }

    public int available() throws IOException {
        int av = super.available();
        if (av <= 0)
            return av;
        return av / inputChannels;
    }

    public AudioFormat getFormat() {
        return newFormat;
    }
}
