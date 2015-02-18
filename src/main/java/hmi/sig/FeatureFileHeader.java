package hmi.sig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FeatureFileHeader {
    public int numfrm; // Total number of frames
    public int dimension; // Feature vector dimension (total)
    public float winsize; // Analysis window size in seconds
    public float skipsize; // Analysis skip size in seconds
    public int samplingRate; // Sampling rate in Hz

    public FeatureFileHeader() {
        numfrm = 0;
        dimension = 0;
        winsize = 0.020f;
        skipsize = 0.010f;
        samplingRate = 0;
    }

    public FeatureFileHeader(FeatureFileHeader existingHeader) {
        numfrm = existingHeader.numfrm;
        dimension = existingHeader.dimension;
        winsize = existingHeader.winsize;
        skipsize = existingHeader.skipsize;
        samplingRate = existingHeader.samplingRate;
    }

    public FeatureFileHeader(String featureFile) {
        try {
            readHeader(featureFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isIdenticalAnalysisParams(FeatureFileHeader hdr) {
        if (this.numfrm != hdr.numfrm)
            return false;
        if (this.dimension != hdr.dimension)
            return false;
        if (this.winsize != hdr.winsize)
            return false;
        if (this.skipsize != hdr.skipsize)
            return false;
        return this.samplingRate == hdr.samplingRate;
    }

    public void readHeader(String file) throws IOException {
        readHeader(file, false);
    }

    public DataInputStream readHeader(String file, boolean bLeaveStreamOpen) throws IOException {
        DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        if (stream != null) {
            readHeader(stream);
            if (!bLeaveStreamOpen) {
                stream.close();
            }
        }
        return stream;
    }

    // Baseline version does nothing!
    // It is the derived class´ responsibility to do the reading and closing the
    // file handle
    public void readHeader(DataInput stream) throws IOException {
        numfrm = stream.readInt();
        dimension = stream.readInt();
        winsize = stream.readFloat();
        skipsize = stream.readFloat();
        samplingRate = stream.readInt();
    }

    public void writeHeader(String file) throws IOException {
        writeHeader(file, false);
    }

    // This version returns the file output stream for further use, i.e. if you
    // want to write additional information
    // in the file use this version
    public DataOutputStream writeHeader(String file, boolean bLeaveStreamOpen) throws IOException {
        DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        if (stream != null) {
            writeHeader(stream);

            if (!bLeaveStreamOpen) {
                stream.close();
                stream = null;
            }
        }

        return stream;
    }

    // Baseline version does nothing!
    // It is the derived class´ responsibility to do the writing and closing the
    // file handle
    public void writeHeader(DataOutput ler) throws IOException {
        ler.writeInt(numfrm);
        ler.writeInt(dimension);
        ler.writeFloat(winsize);
        ler.writeFloat(skipsize);
        ler.writeInt(samplingRate);
    }
}
