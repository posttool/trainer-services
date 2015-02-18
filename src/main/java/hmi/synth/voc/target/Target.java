package hmi.synth.voc.target;

import hmi.data.Boundary;
import hmi.data.Phone;
import hmi.data.Segment;
import hmi.ml.feature.FeatureVector;
import hmi.phone.Allophone;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

public class Target {
    protected String name;
    protected Segment sm;

    protected FeatureVector featureVector = null;

    protected float duration = -1;
    protected float f0 = -1;
    protected int isSilence = -1;

    public Target(String name, Segment sm) {
        this.name = name;
        this.sm = sm;
    }

    public String getName() {
        return name;
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    public float getTargetDurationInSeconds() {
        if (duration != -1) {
            return duration;
        } else {
            if (sm == null)
                return 0;
            throw new NullPointerException("Target " + name + " does not have duration.");
        }
    }

    public void setTargetDurationInSeconds(float newDuration) {
        if (sm != null) {
            if (sm instanceof Phone) {
                ((Phone) sm).setDuration(newDuration);
            } else {
                assert sm instanceof Boundary : "segment should be a phone or a boundary, but is a " + sm.getClass();
                ((Boundary) sm).setDuration(newDuration);
            }
        }
    }

    public float getTargetF0InHz() {
        if (f0 != -1) {
            return f0;
        } else {
            if (sm == null)
                throw new NullPointerException("Target " + name + " does not have a  element.");
            float logf0 = new UnitLogF0().process(this);
            if (logf0 == 0)
                f0 = 0;
            else
                f0 = (float) Math.exp(logf0);
            return f0;
        }
    }

    public boolean hasFeatureVector() {
        return featureVector != null;
    }

    public static UserDataHandler targetFeatureCloner = new UserDataHandler() {
        public void handle(short operation, String key, Object data, Node src, Node dest) {
            if (operation == UserDataHandler.NODE_CLONED && key == "target") {
                dest.setUserData(key, data, this);
                System.err.println("yay");
            } else {
                System.err.println("nay");
            }
        }
    };

    /**
     * Determine whether this target is a silence target
     * 
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence() {

        if (isSilence == -1) {
            // TODO: how do we know the silence symbol here?
            String silenceSymbol = "_";
            if (name.startsWith(silenceSymbol)) {
                isSilence = 1; // true
            } else {
                isSilence = 0; // false
            }
        }
        return isSilence == 1;
    }

    public Allophone getAllophone() {
        return null;
//        if (sm != null) {
//            AllophoneSet allophoneSet = null;
//            Element voiceElement = (Element) getAncestor(sm, VOICE);
//            if (voiceElement != null) {
//                Voice v = Voice.getVoice(voiceElement);
//                if (v != null) {
//                    allophoneSet = v.getAllophoneSet();
//                }
//            }
//            if (allophoneSet == null) {
//                try {
//                    allophoneSet = determineAllophoneSet(sm);
//                } catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            String sampa;
//            if (sm.getNodeName().equals(PHONE)) {
//                sampa = sm.getAttribute("p");
//            } else {
//                assert sm.getNodeName().equals(BOUNDARY);
//                sampa = "_";
//            }
//            return allophoneSet.getAllophone(sampa);
//        }
//        return null;
    }

    public String toString() {
        return name;
    }
}
