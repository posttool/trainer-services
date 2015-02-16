package hmi.ml.cart.io;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class CARTHeader {
    private final static int MAGIC = 0x4d415259;
    private final static int VERSION = 40; // 4.0

    public final static int UNKNOWN = 0;
    public final static int CARTS = 100;
    public final static int DIRECTED_GRAPH = 110;
    public final static int UNITS = 200;
    public final static int LISTENERUNITS = 225;
    public final static int UNITFEATS = 300;
    public final static int LISTENERFEATS = 325;
    public final static int HALFPHONE_UNITFEATS = 301;
    public final static int JOINFEATS = 400;
    public final static int SCOST = 445;
    public final static int PRECOMPUTED_JOINCOSTS = 450;
    public final static int TIMELINE = 500;

    private int magic = MAGIC;
    private int version = VERSION;
    private int type = UNKNOWN;

    public static int peekFileType(String fileName) throws IOException {
        DataInputStream dis = null;
        dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        /* Load the header */
        try {
            CARTHeader hdr = new CARTHeader(dis);
            int type = hdr.getType();
            return type;
        } catch (Exception e) {
            // not a valid header
            return -1;
        } finally {
            dis.close();
        }

    }

    public CARTHeader(int newType) {
        if ((newType > TIMELINE) || (newType < UNKNOWN)) {
            throw new IllegalArgumentException("? file type [" + type + "].");
        }
        type = newType;

        // post-conditions:
        assert version == VERSION;
        assert hasLegalMagic();
        assert hasLegalType();
    }

    public CARTHeader(DataInput input) throws Exception {
        try {
            this.load(input);
        } catch (IOException e) {
            throw new Exception("Cannot load header", e);
        }
        if (!hasLegalMagic() || !hasLegalType()) {
            throw new Exception("? header!");
        }

        // post-conditions:
        assert hasLegalMagic();
        assert hasLegalType();
    }

    public CARTHeader(ByteBuffer input) throws Exception {
        try {
            this.load(input);
        } catch (BufferUnderflowException e) {
            throw new Exception("Cannot load header", e);
        }
        if (!hasLegalMagic() || !hasLegalType()) {
            throw new Exception("? header!");
        }

        // post-conditions:
        assert hasLegalMagic();
        assert hasLegalType();
    }

    public long writeTo(DataOutput output) throws IOException {

        long nBytes = 0;

        assert this.hasLegalType() : "Unknown type [" + type + "].";

        output.writeInt(magic);
        nBytes += 4;
        output.writeInt(version);
        nBytes += 4;
        output.writeInt(type);
        nBytes += 4;

        return (nBytes);
    }

    private void load(DataInput input) throws IOException {

        magic = input.readInt();
        version = input.readInt();
        type = input.readInt();
    }

    private void load(ByteBuffer input) {
        magic = input.getInt();
        version = input.getInt();
        type = input.getInt();
    }

    public int getMagic() {
        return (magic);
    }

    public int getVersion() {
        return (version);
    }

    public int getType() {
        return (type);
    }

    public boolean hasCurrentVersion() {
        return (version == VERSION);
    }

    private boolean hasLegalType() {
        return (type <= TIMELINE) && (type > UNKNOWN);
    }

    private boolean hasLegalMagic() {
        return (magic == MAGIC);
    }

}
