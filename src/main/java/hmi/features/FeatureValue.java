package hmi.features;


public class FeatureValue {
    String name;
    String value;

    public FeatureValue(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasValue() {
        if (value == null)
            return false;
        if (value.trim().equals(""))
            return false;
        return true;
    }


}
