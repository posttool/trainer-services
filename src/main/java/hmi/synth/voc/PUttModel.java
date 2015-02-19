package hmi.synth.voc;

import java.util.Vector;

public class PUttModel {

    private int numModel; /* # of models for current utterance */
    private int numState; /* # of HMM states for current utterance */
    private int totalFrame; /* # of frames for current utterance */
    private int lf0Frame; /* # of frames that are voiced or non-zero */
    private Vector<PModel> modelList;
    private String realisedAcoustParams;

    public PUttModel() {
        numModel = 0;
        numState = 0;
        totalFrame = 0;
        lf0Frame = 0;
        modelList = new Vector<PModel>();
        realisedAcoustParams = "";
    }

    public void setNumModel(int val) {
        numModel = val;
    }

    public int getNumModel() {
        return numModel;
    }

    public void setNumState(int val) {
        numState = val;
    }

    public int getNumState() {
        return numState;
    }

    public void setTotalFrame(int val) {
        totalFrame = val;
    }

    public int getTotalFrame() {
        return totalFrame;
    }

    public void setLf0Frame(int val) {
        lf0Frame = val;
    }

    public int getLf0Frame() {
        return lf0Frame;
    }

    public void addUttModel(PModel newModel) {
        modelList.addElement(newModel);
    }

    public PModel getUttModel(int i) {
        return (PModel) modelList.elementAt(i);
    }

    public int getNumUttModel() {
        return modelList.size();
    }

    public void setRealisedAcoustParams(String str) {
        realisedAcoustParams = str;
    }

    public String getRealisedAcoustParams() {
        return realisedAcoustParams;
    }

    public void concatRealisedAcoustParams(String str) {
        realisedAcoustParams = realisedAcoustParams + str;
    }

}
