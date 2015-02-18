package hmi.synth.voc;

import java.util.Locale;

import javax.sound.sampled.AudioFormat;

public class HMMVoice extends Voice {

    private HMMData htsData = new HMMData();

    /**
     * constructor
     */
    public HMMVoice(String voiceName, Synthesizer synthesizer) throws Exception {
        super(voiceName, synthesizer);

        htsData.initHMMData(voiceName);

    }

    public HMMData getHMMData() {
        return this.htsData;
    }

    /*
     * set parameters for generation: f0Std, f0Mean and length, default values
     * 1.0, 0.0 and 0.0
     */
    public void setF0Std(double dval) {
        htsData.setF0Std(dval);
    }

    public void setF0Mean(double dval) {
        htsData.setF0Mean(dval);
    }

    public void setLength(double dval) {
        htsData.setLength(dval);
    }

    public void setDurationScale(double dval) {
        htsData.setDurationScale(dval);
    }

} /* class HMMVoice */
