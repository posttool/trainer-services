package hmi.synth.voc;

import java.util.Properties;

public class HMMVoice extends Voice {

    private HMMData data = new HMMData();

    public HMMVoice(Properties props) throws Exception {
        super(props);
        data.initHMMData(props);
    }

    public HMMData getHMMData() {
        return this.data;
    }

    public void setF0Std(double dval) {
        data.setF0Std(dval);
    }

    public void setF0Mean(double dval) {
        data.setF0Mean(dval);
    }

    public void setLength(double dval) {
        data.setLength(dval);
    }

    public void setDurationScale(double dval) {
        data.setDurationScale(dval);
    }

}
