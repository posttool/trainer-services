package hmi.synth.voc;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LDataInputStream implements DataInput {

    protected final DataInputStream dis;

    protected final InputStream is;

    protected final byte[] work;

    public static String readUTF(DataInput in) throws IOException {
        return DataInputStream.readUTF(in);
    }

    public LDataInputStream(InputStream in) {
        this.is = in;
        this.dis = new DataInputStream(in);
        work = new byte[8];
    }

    public LDataInputStream(String filename) throws FileNotFoundException {
        this(new FileInputStream(filename));
    }

    public final void close() throws IOException {
        dis.close();
    }

    public final int read(byte ba[], int off, int len) throws IOException {
        return is.read(ba, off, len);
    }

    public final boolean readBoolean() throws IOException {
        return dis.readBoolean();
    }

    public final boolean[] readBoolean(int len) throws IOException {
        boolean[] ret = new boolean[len];

        for (int i = 0; i < len; i++)
            ret[i] = readBoolean();

        return ret;
    }

    public final byte readByte() throws IOException {
        return dis.readByte();
    }

    public final byte[] readByte(int len) throws IOException {
        byte[] ret = new byte[len];

        for (int i = 0; i < len; i++)
            ret[i] = readByte();

        return ret;
    }

    public final char readChar() throws IOException {
        dis.readFully(work, 0, 2);
        return (char) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
    }

    public final char[] readChar(int len) throws IOException {
        char[] ret = new char[len];

        for (int i = 0; i < len; i++)
            ret[i] = readChar();

        return ret;
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final double[] readDouble(int len) throws IOException {
        double[] ret = new double[len];

        for (int i = 0; i < len; i++)
            ret[i] = readDouble();

        return ret;
    }

    public final int[] readDoubleToInt(int len) throws IOException {
        int[] ret = new int[len];

        for (int i = 0; i < len; i++)
            ret[i] = (int) readDouble();

        return ret;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final float[] readFloat(int len) throws IOException {
        float[] ret = new float[len];

        for (int i = 0; i < len; i++)
            ret[i] = readFloat();

        return ret;
    }

    public final void readFully(byte ba[]) throws IOException {
        dis.readFully(ba, 0, ba.length);
    }

    public final void readFully(byte ba[], int off, int len) throws IOException {
        dis.readFully(ba, off, len);
    }

    public final int readInt() throws IOException {
        dis.readFully(work, 0, 4);
        return (work[3]) << 24 | (work[2] & 0xff) << 16 | (work[1] & 0xff) << 8 | (work[0] & 0xff);
    }

    public final int[] readInt(int len) throws IOException {
        int[] ret = new int[len];

        for (int i = 0; i < len; i++)
            ret[i] = readInt();

        return ret;
    }

    public final String readLine() throws IOException {
        return dis.readLine();
    }

    public final long readLong() throws IOException {
        dis.readFully(work, 0, 8);
        return (long) (work[7]) << 56 |
        /* long cast needed or shift done modulo 32 */
        (long) (work[6] & 0xff) << 48 | (long) (work[5] & 0xff) << 40 | (long) (work[4] & 0xff) << 32
                | (long) (work[3] & 0xff) << 24 | (long) (work[2] & 0xff) << 16 | (long) (work[1] & 0xff) << 8
                | (long) (work[0] & 0xff);
    }

    public final long[] readLong(int len) throws IOException {
        long[] ret = new long[len];

        for (int i = 0; i < len; i++)
            ret[i] = readLong();

        return ret;
    }

    public final short readShort() throws IOException {
        dis.readFully(work, 0, 2);
        return (short) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
    }

    public final short[] readShort(int len) throws IOException {
        short[] ret = new short[len];

        for (int i = 0; i < len; i++)
            ret[i] = readShort();

        return ret;
    }

    public final String readUTF() throws IOException {
        return dis.readUTF();
    }

    public final int readUnsignedByte() throws IOException {
        return dis.readUnsignedByte();
    }

    public final int[] readUnsignedByte(int len) throws IOException {
        int[] ret = new int[len];

        for (int i = 0; i < len; i++)
            ret[i] = readUnsignedByte();

        return ret;
    }

    public final int readUnsignedShort() throws IOException {
        dis.readFully(work, 0, 2);
        return ((work[1] & 0xff) << 8 | (work[0] & 0xff));
    }

    public final int[] readUnsignedShort(int len) throws IOException {
        int[] ret = new int[len];

        for (int i = 0; i < len; i++)
            ret[i] = readUnsignedShort();

        return ret;
    }

    public final int skipBytes(int n) throws IOException {
        return dis.skipBytes(n);
    }
}
