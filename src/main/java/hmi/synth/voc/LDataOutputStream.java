package hmi.synth.voc;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LDataOutputStream implements DataOutput {

    protected final DataOutputStream dis;

    protected final byte[] work;

    public LDataOutputStream(OutputStream out) {
        this.dis = new DataOutputStream(out);
        work = new byte[8];// work array for composing output
    }

    public LDataOutputStream(String filename) throws FileNotFoundException {
        this(new FileOutputStream(filename));
    }

    public final void close() throws IOException {
        dis.close();
    }

    public void flush() throws IOException {
        dis.flush();
    }

    public final int size() {
        return dis.size();
    }

    public final synchronized void write(int ib) throws IOException {
        dis.write(ib);
    }

    public final void write(byte ba[]) throws IOException {
        dis.write(ba, 0, ba.length);
    }

    public final synchronized void write(byte ba[], int off, int len) throws IOException {
        dis.write(ba, off, len);
    }

    public final void writeBoolean(boolean v) throws IOException {
        dis.writeBoolean(v);
    }

    public final void writeBoolean(boolean[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeBoolean(v[i]);
    }

    public final void writeBoolean(boolean[] v) throws IOException {
        writeBoolean(v, 0, v.length);
    }

    public final void writeByte(int v) throws IOException {
        dis.writeByte(v);
    }

    public final void writeByte(byte[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeByte(v[i]);
    }

    public final void writeByte(byte[] v) throws IOException {
        writeByte(v, 0, v.length);
    }

    /**
     * Write a string.
     * 
     * @param s
     *            the string to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public final void writeBytes(String s) throws IOException {
        dis.writeBytes(s);
    }

    /**
     * Write a char. Like DataOutputStream.writeChar. Note the parm is an int
     * even though this as a writeChar
     * 
     * @param v
     *            the char to write
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeChar(int v) throws IOException {
        // same code as writeShort
        work[0] = (byte) v;
        work[1] = (byte) (v >> 8);
        dis.write(work, 0, 2);
    }

    public final void writeChar(char[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeChar(v[i]);
    }

    public final void writeChar(char[] v) throws IOException {
        writeChar(v, 0, v.length);
    }

    /**
     * Write a string, not a char[]. Like DataOutputStream.writeChars, flip
     * endianness of each char.
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            writeChar(s.charAt(i));
        }
    }// end writeChars

    /**
     * Write a double.
     * 
     * @param v
     *            the double to write. Like DataOutputStream.writeDouble.
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public final void writeDouble(double[] v, int startPos, int len) throws IOException {
        for (int i = startPos; i < startPos + len; i++)
            writeDouble(v[i]);
    }

    public final void writeDouble(double[] v) throws IOException {
        writeDouble(v, 0, v.length);
    }

    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public final void writeFloat(float[] v, int startPos, int len) throws IOException {

        for (int i = startPos; i < startPos + len; i++)
            writeFloat(v[i]);
    }

    public final void writeFloat(float[] v) throws IOException {
        writeFloat(v, 0, v.length);
    }

    public final void writeInt(int v) throws IOException {
        work[0] = (byte) v;
        work[1] = (byte) (v >> 8);
        work[2] = (byte) (v >> 16);
        work[3] = (byte) (v >> 24);
        dis.write(work, 0, 4);
    }

    public final void writeInt(int[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeInt(v[i]);
    }

    public final void writeInt(int[] v) throws IOException {
        writeInt(v, 0, v.length);
    }

    public final void writeLong(long v) throws IOException {
        work[0] = (byte) v;
        work[1] = (byte) (v >> 8);
        work[2] = (byte) (v >> 16);
        work[3] = (byte) (v >> 24);
        work[4] = (byte) (v >> 32);
        work[5] = (byte) (v >> 40);
        work[6] = (byte) (v >> 48);
        work[7] = (byte) (v >> 56);
        dis.write(work, 0, 8);
    }

    public final void writeLong(long[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeLong(v[i]);
    }

    public final void writeLong(long[] v) throws IOException {
        writeLong(v, 0, v.length);
    }

    public final void writeShort(int v) throws IOException {
        work[0] = (byte) v;
        work[1] = (byte) (v >> 8);
        dis.write(work, 0, 2);
    }

    public final void writeShort(short[] v, int startPos, int len) throws IOException {
        assert v.length < startPos + len;

        for (int i = startPos; i < startPos + len; i++)
            writeShort(v[i]);
    }

    public final void writeShort(short[] v) throws IOException {
        writeShort(v, 0, v.length);
    }

    public final void writeUTF(String s) throws IOException {
        dis.writeUTF(s);
    }

}
